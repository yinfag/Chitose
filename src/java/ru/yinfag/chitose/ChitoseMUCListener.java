package ru.yinfag.chitose;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import java.util.*;
import java.util.concurrent.atomic.AtomicMarkableReference;

class ChitoseMUCListener implements PacketListener {

	private static final Set<String> VOICED_ROLES = new HashSet<>();

	static {
		VOICED_ROLES.add("participant");
		VOICED_ROLES.add("moderator");
	}

	private final List<MessageProcessor> messageProcessors = new ArrayList<>();

	private final MultiUserChat muc;

	private final String conference;
	private final String defaultNickname;
	private final String jid;
	private final AtomicMarkableReference<String> nick;
	
	private final Map<String, Properties> perMucProps;

	ChitoseMUCListener(final MultiUserChat muc, final Properties props, final Map<String, Properties> perMucProps) {
		this.perMucProps = perMucProps;
		this.muc = muc;
		conference = muc.getRoom();
		final Properties propsMuc = perMucProps.get(conference);
		defaultNickname = propsMuc.getProperty("nickname");
		nick = new AtomicMarkableReference<>(defaultNickname, false);
		jid = props.getProperty("login") + "@" + props.getProperty("domain") + "/" + props.getProperty("resource");

		populateMessageProcessors(perMucProps, props, muc);
	}

	private void populateMessageProcessors(final Map<String, Properties> perMucProps, final Properties props, final MultiUserChat muc) {
		messageProcessors.add(new GoogleMessageProcessor(perMucProps, props));
		messageProcessors.add(new SmoochMessageProcessor(perMucProps));
		messageProcessors.add(new URLExpander(perMucProps));
		messageProcessors.add(new GelbooruMessageProcessor(perMucProps));
		messageProcessors.add(new DiceMessageProcessor(perMucProps));
		messageProcessors.add(new JpgToMessageProcessor(perMucProps));
		messageProcessors.add(new WorldArtMessageProcessor(perMucProps));
		messageProcessors.add(new TimerMessageProcessor(perMucProps, muc));
		messageProcessors.add(new HelpMessageProcessor(perMucProps));		
	}

	public PacketListener newProxyPacketListener() {
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
