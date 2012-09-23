package ru.yinfag.chitose;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chitose's main class. Contains the application entry point.
 */
public class Chitose {

	/**
	 * Application entry point. Handles loading of configuration files,
	 * connecting to server, joining chat rooms, waiting for exit command,
	 * disconnecting.
	 *
	 * @param args    command line arguments. Ignored.
	 */

	private final static String DEFAULT_CONF_D = "chitose.conf.d";

	public static void main(final String[] args) {
		final Map<String, String> properties = new HashMap<>();
		try {
			Files.walkFileTree(Paths.get(DEFAULT_CONF_D), new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					final Properties props = new Properties();
					try (final Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
						props.load(reader);
					} catch (IOException | IllegalArgumentException e) {
						log("Failed to read properties from " + file, e);
						return FileVisitResult.SKIP_SUBTREE;
					}
					properties.putAll((Map<String, String>) props);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
					log("Fail to read configs from " + file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log("Error while reading properties", e);
		}

		final ServiceLoader<Plugin> pluginLoader = ServiceLoader.load(Plugin.class);
		final List<Plugin> plugins = new ArrayList<>();
		final List<MessageProcessorPlugin> messageProcessors = new ArrayList<>();
		final BlockingQueue<Pair<String, String>> messageQueue = new LinkedBlockingQueue<>();
		for (final Plugin plugin : pluginLoader) {
			final String pluginName = plugin.getClass().getName();
			log("Loading plugin: " + pluginName);
			if (plugin instanceof MessageProcessorPlugin) {
				messageProcessors.add((MessageProcessorPlugin) plugin);
			}
			if (plugin instanceof MessageSenderPlugin) {
				((MessageSenderPlugin) plugin).setMessageSender(new MessageSender() {
					@Override
					public void sendToConference(final String conference, final String message) {
						messageQueue.add(new Pair<>(conference, message));
					}
				});
			}
			for (final String key : properties.keySet()) {
				if (key.startsWith(pluginName)) {
					plugin.setProperty(key.substring(pluginName.length() + 1), null, properties.get(key));
				}
			}
			plugin.init();
			plugins.add(plugin);
		}

		final List<String> chatrooms;
		try {
			chatrooms = Files.readAllLines(
					Paths.get("chatrooms.cfg"),
					Charset.forName("UTF-8")
			);
		} catch (IOException e) {
			log("Failed to load chatroom list", e);
			return;
		}
		
		final Properties props = new Properties();
		final Properties chatroomProps = new Properties();
		
		final Map<String, Properties> perMucProps = new HashMap<>();
		
		try (final Reader reader = Files.newBufferedReader(
			Paths.get("default_chatroom_config.cfg"),
			Charset.forName("UTF-8")
		)) {
			chatroomProps.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		try (final Reader reader = Files.newBufferedReader(
			Paths.get("chitose.cfg"),
			Charset.forName("UTF-8")
		)) {
			props.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			log("Failed to load chitose.cfg", e);
			return;
		}
		
		// get a connection object and connect
		final XMPPConnection conn =
				new XMPPConnection(props.getProperty("domain"));
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
		
		final Connection dbconn;	
		try {
			dbconn = DriverManager.getConnection(
					"jdbc:hsqldb:file:" + props.getProperty("path.to.database"), 
					"SA",
					""
			);
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}	
		
		final Timer tokyotoshoTimer = new Timer();

		final ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		final AtomicBoolean shuttingDown = new AtomicBoolean();
		try {
			// attempt login
			try {
				conn.login(
						props.getProperty("login"),
						props.getProperty("password"),
						props.getProperty("resource")
				);
			} catch (XMPPException e) {
				log("Failed to login", e);
				return;
			}

			// join all listed conferences
			
			final List<MultiUserChat> mucs = new ArrayList<>();
			Properties mucProps;
			final Map<String, MultiUserChat> mucByAddress = new HashMap<>();
		
			for (final String chatroom : chatrooms) {
				
				mucProps = new Properties(chatroomProps);
				
				try (final Reader reader = Files.newBufferedReader(
						Paths.get(chatroom + ".cfg"),
						Charset.forName("UTF-8")
				)) {
					mucProps.load(reader);
				} catch (IOException | IllegalArgumentException e) {
					e.printStackTrace();
				}
				
				perMucProps.put(chatroom, mucProps);
				
				final MultiUserChat muc = new MultiUserChat(conn, chatroom);
				mucs.add(muc);
				mucByAddress.put(chatroom, muc);
				// add message and presence listeners
				final ChitoseMUCListener listener =
						new ChitoseMUCListener(muc, props, mucProps, dbconn, messageProcessors);
				muc.addParticipantListener(listener.newProxyPacketListener());
				muc.addMessageListener(listener.newProxyPacketListener());

				// don't get history of previous messages
				final DiscussionHistory history = new DiscussionHistory();
				history.setMaxStanzas(0);

				// enter the chatroom
				
				final String nickname = mucProps.getProperty("nickname");
				
				try {
					muc.join(nickname, null, history, 5000);
				} catch (XMPPException e) {
					log("Failed to join the chat room", e);
					mucs.remove(muc);
					mucByAddress.remove(chatroom);
				}
			}

			executorService.execute(new Runnable() {
				@Override
				public void run() {
					while (true) {
						final Pair<String, String> take;
						try {
							take = messageQueue.take();
						} catch (InterruptedException e) {
							if (shuttingDown.get()) {
								break;
							} else {
								continue;
							}
						}
						final MultiUserChat multiUserChat = mucByAddress.get(take.getFirst());
						try {
							multiUserChat.sendMessage(take.getSecond());
						} catch (XMPPException e) {
							log("Failed to send message.", e);
						}
					}
				}
			});

			try {
				final Tokyotosho parser = new Tokyotosho(perMucProps, mucs, dbconn);
				final long tokyotoshoUpdatePeriod = 60000 * Long.parseLong(
					props.getProperty("tokyotosho.update.period", "10")
				);
				tokyotoshoTimer.schedule(parser, 30000, tokyotoshoUpdatePeriod);
			} catch (SQLException e) {
				log("Failed to initialize Tokyotosho monitor", e);
			}

			waitForInterrupt();
			log("Shutting down gracefully...");
		} finally {
			shuttingDown.set(true);
			executorService.shutdownNow();
			for (final Plugin plugin : plugins) {
				plugin.shutdown();
			}
			tokyotoshoTimer.cancel();
			// disconnect from server
			conn.disconnect();
			try {
				dbconn.createStatement().execute("shutdown");
				dbconn.close();
			} catch (SQLException e) {
				log("Error while closing DB connection", e);
			}
		}
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
	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}

	private static void log(final String message) {
		log(message, null);
	}

}
		
