package ru.yinfag.chitose;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.ServiceLoader;

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
	public static void main(final String[] args) {
		
		final ServiceLoader<Plugin> pluginLoader = ServiceLoader.load(Plugin.class);
		final List<Plugin> plugins = new ArrayList<>();
		for (final Plugin plugin : pluginLoader) {
			log("Loading plugin: " + plugin.getClass().getName());
			if (plugin instanceof MessageSenderPlugin) {
				((MessageSenderPlugin) plugin).setMessageSender(new MessageSender() {
					@Override
					public void sendToConference(final String conference, final String message) {
						log("Attempted to send message '" + message + "' to " + conference);
					}
				});
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
				// add message and presence listeners
				final ChitoseMUCListener listener =
						new ChitoseMUCListener(muc, props, mucProps, dbconn);
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
				}
			}
			
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
		
