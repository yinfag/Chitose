package ru.yinfag.chitose;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChitoseMUCListener implements PacketListener {
	// регэкспы регэкспушки
	private static final Pattern p3 = Pattern.compile("(?:(?:Chitose)|(?:[Чч]итосе)).*?расскажи.*?про \"(.+?)\"");
	private static final String p4 = "отсортировано по дате выхода";
	private static final Pattern p5 = Pattern.compile("'estimation'>(.+?)&nbsp");
	private static final Pattern p6 = Pattern.compile("Краткое содержание:.*?class='review'>(.+?)</p>");
	private static final String p7 = "<b>Раздел &laquo;анимация&raquo;";
	private static final Pattern p8 = Pattern.compile("animation/animation.php\\?id=(\\d+)");
	private static final String p9 = "<meta http-equiv='Refresh' content='0;";
	private static final Pattern p11 = Pattern.compile(".+?@.+?\\..+?\\..+?/(.+?)");
	private static final Pattern p12 = Pattern.compile(".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?напомни.*?о \"(.+?)\" через ([0-9]+).*?(?:(?:минут)|(?:минуты)|(?:минуту))");

	private static final Set<String> VOICED_ROLES = new HashSet<>();

	static {
		VOICED_ROLES.add("participant");
		VOICED_ROLES.add("moderator");
	}

	private final List<MessageProcessor> messageProcessors = new ArrayList<>();

	private final Map<String, Timer> timers = new HashMap<>();

	private final MultiUserChat muc;

	// r - рандом
	private final Random r = new Random();
	
	private final String conference;
	private final String defaultNickname;
	private final String jid;
	private final AtomicMarkableReference<String> nick;

	ChitoseMUCListener(final MultiUserChat muc, final Properties props) {
		this.muc = muc;
		conference = props.getProperty("conference");
		defaultNickname = props.getProperty("nickname");
		nick = new AtomicMarkableReference<>(defaultNickname, false);
		jid = props.getProperty("login") + "@" + props.getProperty("domain") + "/" + props.getProperty("resource");

		populateMessageProcessors(props, muc);
	}

	private void populateMessageProcessors(final Properties props, final MultiUserChat muc) {
		messageProcessors.add(new SmoochMessageProcessor());
		messageProcessors.add(new URLExpander(props));
		messageProcessors.add(new GelbooruMessageProcessor());
		messageProcessors.add(new DiceMessageProcessor());
		messageProcessors.add(new JpgToMessageProcessor());
		messageProcessors.add(new WorldArtMessageProcessor());
		messageProcessors.add(new TimerMessageProcessor(muc));
	}
	

	public PacketListener newProxypacketListener() {
		return new PacketListener() {
			@Override
			public void processPacket(final Packet packet) {
				ChitoseMUCListener.this.processPacket(packet);
			}
		};
	}

	@Override
	public void processPacket(final Packet packet) {
		if (packet instanceof Presence) {
			processPresence((Presence) packet);
		} else if (packet instanceof Message) {
			processMessage((Message) packet);
		}
	}

	private void processMessage(final Message message) {
		log("message from " + message.getFrom());
		// if this is a message from ourselves, don't react to it
		if (
				(conference + "/" + nick.getReference()).equals(message.getFrom()) ||
						jid.equals(message.getFrom())
		) {
			return;
		}

		CharSequence answer = null;
		for (final MessageProcessor processor : messageProcessors) {
			try {
				answer = processor.process(message);
			} catch (MessageProcessingException e) {
				log("Error while processing a message with " + processor, e);
				answer = "Няшки закрыты на ремонт.";
			}
			if (answer != null) {
				break;
			}
		}
		if (answer != null) {
			try {
				muc.sendMessage(answer.toString());
			} catch (XMPPException e) {
				log("Failed to send a message", e);
			}
			return;
		}
	}
	private void processPresence(final Presence presence) {
		System.out.println(presence.getFrom());
		if ((conference + "/" + nick.getReference()).equals(presence.getFrom())) {
			for (final PacketExtension extension : presence.getExtensions()) {
				System.out.println("e: " + extension);
				if (extension instanceof MUCUser) {
					final MUCUser.Item item = ((MUCUser) extension).getItem();
					final String role = item.getRole();
					if (!VOICED_ROLES.contains(role) && !nick.isMarked()) {
						try {
							final String newNick = "Дайте_войс";
							muc.changeNickname(newNick);
							nick.set(newNick, true);
						} catch (XMPPException e) {
							log("Failed to change nick", e);
						}
					} else if (VOICED_ROLES.contains(role) && nick.isMarked()) {
						try {
							final String newNick = defaultNickname;
							muc.changeNickname(newNick);
							nick.set(newNick, false);
						} catch (XMPPException e) {
							log("Failed to change nick", e);
						}
						try {
							muc.sendMessage("Аригато!");
						} catch (XMPPException e) {
							log("Failed to say thanks", e);
						}
					}
				}
			}
		}
	}

	private static void log(final String message, final Exception e) {
		log(message);
		if (e != null) {
			e.printStackTrace();
		}
	}
	
	private static void log(final String message) {
		System.out.println(message);
	}
}
