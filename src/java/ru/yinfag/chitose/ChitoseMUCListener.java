package ru.yinfag.chitose;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;

class ChitoseMUCListener implements PacketListener {

	private static final Set<String> VOICED_ROLES = new HashSet<>();

	static {
		VOICED_ROLES.add("participant");
		VOICED_ROLES.add("moderator");
	}

	private final List<ConferenceMessageProcessorPlugin> messageProcessors = new ArrayList<>();
	private final List<PresenceProcessorPlugin> presenceProcessors = new ArrayList<>();

	private final MultiUserChat muc;

	private final String conference;
	private final String defaultNickname;
	private final String jid;
	private final AtomicMarkableReference<String> nick;
	
	ChitoseMUCListener(final MultiUserChat muc, final Properties account, final String nickname, final List<ConferenceMessageProcessorPlugin> messageProcessors, final List<PresenceProcessorPlugin> presenceProcessors) {
		this.muc = muc;
		conference = muc.getRoom();
		defaultNickname = nickname;
		nick = new AtomicMarkableReference<>(nickname, false);
		jid = account.getProperty("login") + "@" + account.getProperty("domain") + "/" + account.getProperty("resource");
		this.messageProcessors.addAll(messageProcessors);
		this.presenceProcessors.addAll(presenceProcessors);

//		populateMessageProcessors(mucProps, props, muc, dbconn);
	}

/*	private void populateMessageProcessors(final Properties mucProps, final Properties props, final MultiUserChat muc, final Connection dbconn) {
		messageProcessors.add(new GoogleMessageProcessor(mucProps, props));
		messageProcessors.add(new SmoochMessageProcessor(mucProps));
		messageProcessors.add(new URLExpander(mucProps));
		messageProcessors.add(new GelbooruMessageProcessor(mucProps));
		messageProcessors.add(new DiceMessageProcessor(mucProps));
		messageProcessors.add(new JpgToMessageProcessor(mucProps));
		messageProcessors.add(new WorldArtMessageProcessor(mucProps));
		messageProcessors.add(new TimerMessageProcessor(mucProps, muc));
		messageProcessors.add(new HelpMessageProcessor(mucProps));
		messageProcessors.add(new MailMessageProcessor(mucProps, props));
		try {
			messageProcessors.add(new TokyotoshoMessageProcessor(mucProps, dbconn));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
*/
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

/*		CharSequence answer = null;
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
*/
		for (final ConferenceMessageProcessorPlugin plugin : messageProcessors) {
			log(" trying " + plugin);
			try {
				plugin.processConferenceMessage(message);
			} catch (MessageProcessingException e) {
				log("Error while processing message", e);
			}
		}
	}
	private void processPresence(final Presence presence) {
		log("Presence from " + presence.getFrom());
		log(" t: " + presence.getType());
		log(" m: " + presence.getMode());
		for (final String propertyName : presence.getPropertyNames()) {
			log(" p: " + propertyName + " = " + presence.getProperty(propertyName));
		}
		for (final PacketExtension extension : presence.getExtensions()) {
			log(" e: " + extension);
		}
		final boolean presenceIsMine = (conference + "/" + nick.getReference()).equals(presence.getFrom());
		if (presenceIsMine) {
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

		for (final PresenceProcessorPlugin plugin : presenceProcessors) {
			if (presenceIsMine && !plugin.isProcessingOwnPresence()) {
				continue;
			}
			log(" trying " + plugin);
			try {
				plugin.processPresence(presence);
			} catch (PresenceProcessingException e) {
				log("Error while processing presence", e);
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
