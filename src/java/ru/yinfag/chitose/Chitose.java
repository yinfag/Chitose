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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.HashMap;
import java.util.Map;

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
		
		final Map<String, Properties> perMucProps = new HashMap<>();
		for (final String chatroom : chatrooms) {
			final Properties chatroomProps = new Properties();
			try (final Reader reader = Files.newBufferedReader(
				Paths.get(chatroom + ".cfg"),
				Charset.forName("UTF-8")
			)) {
				chatroomProps.load(reader);
			} catch (IOException | IllegalArgumentException e) {
				try (final Reader reader = Files.newBufferedReader(
					Paths.get("default_chatroom_config.cfg"),
					Charset.forName("UTF-8")
				)) {
					chatroomProps.load(reader);
				} catch (IOException | IllegalArgumentException e1) {
					e1.printStackTrace();
				}
			}
			perMucProps.put(chatroom, chatroomProps);
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
			for (final String chatroom : chatrooms) {
				final MultiUserChat muc = new MultiUserChat(conn, chatroom);
				mucs.add(muc);
				// add message and presence listeners
				final ChitoseMUCListener listener =
						new ChitoseMUCListener(muc, props, perMucProps);
				muc.addParticipantListener(listener.newProxyPacketListener());
				muc.addMessageListener(listener.newProxyPacketListener());

				// don't get history of previous messages
				final DiscussionHistory history = new DiscussionHistory();
				history.setMaxStanzas(0);

				// enter the chatroom
				
				final Properties mucProps = perMucProps.get(chatroom);
				final String nickname = mucProps.getProperty("nickname");
				
				try {
					muc.join(nickname, null, history, 5000);
				} catch (XMPPException e) {
					log("Failed to join the chat room", e);
				}
			}

			final Tokyotosho parser = new Tokyotosho(perMucProps, mucs);
			final long tokyotoshoUpdatePeriod = 60000 * Long.parseLong(
					props.getProperty("tokyotosho.update.period", "10")
			);
			tokyotoshoTimer.schedule(parser, 30000, tokyotoshoUpdatePeriod);

			waitForExitCommand();
		} finally {
			tokyotoshoTimer.cancel();
			// disconnect from server
			conn.disconnect();
		}
	}

	/**
	 * This method waits for the user to input the exit command from console.
	 */
	private static void waitForExitCommand() {
		final Scanner sc = new Scanner(System.in, "UTF-8");
		sc.useDelimiter("\\n");
		while (sc.hasNext()) {
			final String command = sc.next();
			if ("exit".equals(command)) {
				return;
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

}
		
