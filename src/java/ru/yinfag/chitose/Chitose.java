package ru.yinfag.chitose;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.XHTMLExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chitose's main class. Contains the application entry point.
 */
public class Chitose {

	private final static String DEFAULT_CONF_D = "chitose.conf.d";

	private static Map<String, String> nicknameByConference = new HashMap<>();
	static {
		nicknameByConference.put("", "Chitose");
	}

	/**
	 * Application entry point. Handles loading of configuration files,
	 * connecting to server, joining chat rooms, waiting for exit command,
	 * disconnecting.
	 *
	 * @param args    command line arguments. Ignored.
	 */
	public static void main(final String[] args) {

		final ServiceLoader<Plugin> pluginLoader = ServiceLoader.load(Plugin.class);
		final List<Plugin> plugins = new ArrayList<>();
		try {
			for (final Plugin plugin : pluginLoader) {
				log("Created plugin object: " + plugin.getClass().getName());
				plugins.add(plugin);
			}
		} catch (ServiceConfigurationError e) {
			log("Error while instantiating plugins", e);
			return;
		}

		final Path configDir = Paths.get(DEFAULT_CONF_D);

		final HashSet<String> conferences = new HashSet<>();
		try {
			loadConfiguration(configDir, conferences, plugins);
		} catch (IOException e) {
			log("Error while reading configuration", e);
			return;
		}

		final List<ConferenceMessageProcessorPlugin> conferenceMessageProcessors = new ArrayList<>();
		final List<PresenceProcessorPlugin> presenceProcessors = new ArrayList<>();
		final BlockingQueue<Message> conferenceMessageQueue = new LinkedBlockingQueue<>();

		final List<ChatMessageProcessorPlugin> chatMessageProcessors = new ArrayList<>();
		final BlockingQueue<Message> privateMessageQueue = new LinkedBlockingQueue<>();

		for (final Plugin plugin : pluginLoader) {
			final String pluginName = plugin.getClass().getName();
			log("Loading plugin: " + pluginName);
			if (plugin instanceof ConferenceMessageProcessorPlugin) {
				conferenceMessageProcessors.add((ConferenceMessageProcessorPlugin) plugin);
			}
			if (plugin instanceof PresenceProcessorPlugin) {
				presenceProcessors.add((PresenceProcessorPlugin) plugin);
			}
			if (plugin instanceof ChatMessageProcessorPlugin) {
				chatMessageProcessors.add((ChatMessageProcessorPlugin) plugin);
			}
			if (plugin instanceof MessageSenderPlugin) {
				((MessageSenderPlugin) plugin).setMessageSender(new MessageSender() {
					@Override
					public void sendToConference(final String conference, final String text) {
						final Message message = new Message(conference, Message.Type.groupchat);
						message.addBody(null, text);
						conferenceMessageQueue.add(message);
					}

					@Override
					public void sendToConference(final String conference, final String text, final String xhtml) {
						final Message message = new Message(conference, Message.Type.groupchat);
						message.addBody(null, text);
						final XHTMLExtension xhtmlExtension = new XHTMLExtension();
						xhtmlExtension.addBody(xhtml);
						message.addExtension(xhtmlExtension);
						conferenceMessageQueue.add(message);
					}

					@Override
					public void sendToUser(final String user, final String text) {
						final Message message = new Message(user, Message.Type.chat);
						message.addBody(null, text);
						privateMessageQueue.add(message);
					}
				});
			}
			if (plugin instanceof NicknameAwarePlugin) {
				((NicknameAwarePlugin) plugin).setNicknameByConference(new NicknameByConference() {
					@Override
					public String get(final String conference) {
						return getNicknameByConference(conference);
					}
				});
			}
			plugin.init();
			plugins.add(plugin);
		}

		final Properties account = new Properties();
		try (final Reader reader = Files.newBufferedReader(
			configDir.resolve("account.conf"),
			Charset.forName("UTF-8")
		)) {
			account.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			log("Failed to load account configuration", e);
			return;
		}
		
		// get a connection object and connect
		final XMPPConnection conn =
				new XMPPConnection(account.getProperty("domain"));
		try {
			conn.connect();
		} catch (XMPPException e) {
			log("Failed to connect to server", e);
			return;
		}

		try {
			Class.forName("org.hsqldb.jdbc.JDBCDriver" );
		} catch (Exception e) {
			log("Failed to load DB driver", e);
			return;
		}
		
//		final Connection dbconn;
//		try {
//			dbconn = DriverManager.getConnection(
//					"jdbc:hsqldb:file:" + props.getProperty("path.to.database"),
//					"SA",
//					""
//			);
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return;
//		}
		
//		final Timer tokyotoshoTimer = new Timer();

		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
		final AtomicBoolean shuttingDown = new AtomicBoolean();
		try {
			// attempt login
			try {
				conn.login(
						account.getProperty("login"),
						account.getProperty("password"),
						account.getProperty("resource")
				);
			} catch (XMPPException e) {
				log("Failed to login", e);
				return;
			}

			// join all listed conferences
			
//			final List<MultiUserChat> mucs = new ArrayList<>();
			Properties mucProps;
			final Map<String, MultiUserChat> mucByAddress = new HashMap<>();
		
			for (final String chatroom : conferences) {
				final MultiUserChat muc = new MultiUserChat(conn, chatroom);
//				mucs.add(muc);
				mucByAddress.put(chatroom, muc);
				final String nickname = getNicknameByConference(chatroom);
				final ChitoseMUCListener listener =
						new ChitoseMUCListener(muc, account, nickname, conferenceMessageProcessors, presenceProcessors);
				// add message and presence listeners
				muc.addParticipantListener(listener.newProxyPacketListener());
				muc.addMessageListener(listener.newProxyPacketListener());

				// don't get history of previous messages
				final DiscussionHistory history = new DiscussionHistory();
				history.setMaxStanzas(0);

				// enter the chatroom
				try {
					muc.join(nickname, null, history, 5000);
				} catch (XMPPException e) {
					log("Failed to join the chat room", e);
//					mucs.remove(muc);
					mucByAddress.remove(chatroom);
				}
			}

			executorService.scheduleWithFixedDelay(
					new Runnable() {
						@Override
						public void run() {
							while (true) {
								final Message message;
								try {
									message = conferenceMessageQueue.take();
								} catch (InterruptedException e) {
									if (shuttingDown.get()) {
										break;
									} else {
										continue;
									}
								}
								final MultiUserChat multiUserChat = mucByAddress.get(message.getTo());
								try {
									multiUserChat.sendMessage(message);
								} catch (XMPPException e) {
									log("Failed to send message.", e);
								}
							}
						}
					},
					0,
					1,
					TimeUnit.SECONDS
			);

			final ChitoseChatListener chatListener = new ChitoseChatListener(chatMessageProcessors);
			final Map<String, Chat> chatByAddress = new HashMap<>();
			executorService.scheduleWithFixedDelay(
					new Runnable() {
						@Override
						public void run() {
							while (true) {
								final Message message;
								try {
									message = privateMessageQueue.take();
								} catch (InterruptedException e) {
									if (shuttingDown.get()) {
										break;
									} else {
										continue;
									}
								}
								final String jid = message.getTo();
								final Chat chat;
								if (chatByAddress.containsKey(jid)) {
									chat = chatByAddress.get(jid);
								} else {
									chatByAddress.put(jid, chat = conn.getChatManager().createChat(jid, chatListener));
								}
								try {
									chat.sendMessage(message);
								} catch (XMPPException e) {
									log("Failed to send message.", e);
								}
							}
						}
					},
					0,
					1,
					TimeUnit.SECONDS
			);

//			try {
//				final Tokyotosho parser = new Tokyotosho(perMucProps, mucs, dbconn);
//				final long tokyotoshoUpdatePeriod = 60000 * Long.parseLong(
//						props.getProperty("tokyotosho.update.period", "10")
//				);
//				tokyotoshoTimer.schedule(parser, 30000, tokyotoshoUpdatePeriod);
//			} catch (SQLException e) {
//				log("Failed to initialize Tokyotosho monitor", e);
//			}

			waitForInterrupt();
			log("Shutting down gracefully...");
		} finally {
			shuttingDown.set(true);
			executorService.shutdownNow();
			for (final Plugin plugin : plugins) {
				plugin.shutdown();
			}
//			tokyotoshoTimer.cancel();
			// disconnect from server
			conn.disconnect();
//			try {
//				dbconn.createStatement().execute("shutdown");
//				dbconn.close();
//			} catch (SQLException e) {
//				log("Error while closing DB connection", e);
//			}
		}
	}

	private static String getNicknameByConference(final String conference) {
		return nicknameByConference.containsKey(conference) ?
				nicknameByConference.get(conference) :
				nicknameByConference.get("");
	}

	private static void loadConfiguration(final Path confDir, final Set<String> conferences, final List<Plugin> plugins) throws IOException {
		if (!Files.exists(confDir)) {
			throw new FileNotFoundException("The specified configuration directory does not exist");
		}
		if (!Files.isDirectory(confDir)) {
			throw new NotDirectoryException(confDir.toString());
		}
		// get conference list
		// todo: check validity
		conferences.addAll(Files.readAllLines(confDir.resolve("conferences.list"), Charset.forName("UTF-8")));
		final Map<String, Plugin> pluginByClassName = new HashMap<>();
		for (final Plugin plugin : plugins) {
			pluginByClassName.put(plugin.getClass().getName(), plugin);
		}
		Files.walkFileTree(
				confDir,
				Collections.singleton(FileVisitOption.FOLLOW_LINKS),
				2,
				new FileVisitor<Path>() {
					private int depth = -1;
					private String conference;
					@Override
					public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
						++depth;
						switch (depth) {
							case 0:
								return FileVisitResult.CONTINUE;
							case 1:
								final String dirName = dir.getFileName().toString();
								if ("default".equals(dirName)) {
									conference = "";
									return FileVisitResult.CONTINUE;
								}
								if (conferences.contains(dirName)) {
									conference = dirName;
									return FileVisitResult.CONTINUE;
								}
								return FileVisitResult.SKIP_SUBTREE;
							default:
								return FileVisitResult.SKIP_SUBTREE;
						}
					}

					@Override
					public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
						if (depth != 1) {
							return FileVisitResult.SKIP_SUBTREE;
						}
						final String fileName = file.getFileName().toString();
						if ("chitose.conf".equals(fileName)) {
							final Properties properties = new Properties();
							try (final Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
								properties.load(reader);
							}
							final String nickname = properties.getProperty("nickname");
							if (nickname != null) {
								nicknameByConference.put(conference, nickname);
							}
						} else if (pluginByClassName.containsKey(fileName)) {
							final Properties properties = new Properties();
							try (final Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
								properties.load(reader);
							}
							final Plugin plugin = pluginByClassName.get(fileName);
							for (final String key : properties.stringPropertyNames()) {
								plugin.setProperty(key, conference, properties.getProperty(key));
							}
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
						// todo: log
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
						--depth;
						return FileVisitResult.CONTINUE;
					}
				}
		);
	}

	/**
	 * This method waits for the user to input the exit command from console.
	 */
	private static void waitForInterrupt() {
		final CountDownLatch latch = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		while (true) {
			try {
				latch.await();
				break;
			} catch (InterruptedException ignore) {
			}
		}
	}

	/**
	 * Simple logging facility.
	 *
	 * @param message    the message to display.
	 * @param e          the error to report, or <code>null</code> if it's an
	 *                      informational message.
	 */
	private static void log(final String message, final Throwable e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}

	private static void log(final String message) {
		log(message, null);
	}

}
		
