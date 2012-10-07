package ru.yinfag.chitose.plugin.captcha;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.Occupant;
import ru.yinfag.chitose.ChatMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.MucAdministrator;
import ru.yinfag.chitose.MucAdministratorPlugin;
import ru.yinfag.chitose.NicknameAwarePlugin;
import ru.yinfag.chitose.NicknameByConference;
import ru.yinfag.chitose.PresenceProcessingException;
import ru.yinfag.chitose.PresenceProcessorPlugin;
import ru.yinfag.chitose.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class CaptchaPlugin implements PresenceProcessorPlugin, NicknameAwarePlugin, MessageSenderPlugin, ChatMessageProcessorPlugin, MucAdministratorPlugin {

	private NicknameByConference myNicknameByConference;
	private MessageSender mySender;
	private MucAdministrator myMucAdministrator;

	private List<Presence> myPresenceBacklog = new ArrayList<>();

	private final Map<String, String> myCaptcha = new HashMap<>();

	@Override
	public void setNicknameByConference(final NicknameByConference nbc) {
		myNicknameByConference = nbc;
	}

	@Override
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}

	@Override
	public void setMucAdministrator(final MucAdministrator administrator) {
		myMucAdministrator = administrator;
	}

	@Override
	public boolean isProcessingOwnPresence() {
		return true;
	}

	@Override
	public void processPresence(final Presence presence) throws PresenceProcessingException {
		if (presence.getType() != Presence.Type.available) {
			return;
		}
		final String user = presence.getFrom();
		final String[] userConferenceAndNick = user.split("/", 2);
		final boolean presenceIsMine = myNicknameByConference.get(userConferenceAndNick[0]).equals(userConferenceAndNick[1]);
		if (myPresenceBacklog != null) {
			if (presenceIsMine) {
				final List<Presence> backlog = myPresenceBacklog;
				myPresenceBacklog = null;
				for (final Presence backloggedPresence : backlog) {
					log("  -unbacklogging");
					processPresence(backloggedPresence);
				}
				log("  -returning");
			} else {
				log("  -backlogging");
				myPresenceBacklog.add(presence);
				return;
			}
		}
		if (presenceIsMine) {
			return;
		}

		final String role;
		try {
			final Occupant occupantData = myMucAdministrator.getOccupantData(user);
			role = occupantData.getRole();
		} catch (Throwable e) {
			e.printStackTrace(System.out);  //To change body of catch statement use File | Settings | File Templates.
			throw e;
		}
		System.out.println("  -role=" + role);
		if (!"visitor".equals(role)) {
			myCaptcha.remove(user);
			return;
		}
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		final List<Integer> list = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			list.add(random.nextInt(10));
		}
		final String captcha = Utils.join(list, " ");
		log("  Remembering '" + captcha + "' for " + user);
		myCaptcha.put(user, captcha);
		mySender.sendToUser(user, "Щтобы писать в эту няшную конфочку, введи вот эти вот нящные циферки, будь няшей!");
		mySender.sendToUser(user, captcha);
	}

	@Override
	public void processChatMessage(final Message message) throws MessageProcessingException {
		final String user = message.getFrom();
		if (myCaptcha.containsKey(user)) {
			final String rightAnswer = myCaptcha.get(user);
			log("  user " + user + " has captcha: " + rightAnswer);
			if (rightAnswer.equals(message.getBody())) {
				try {
					myMucAdministrator.grantVoice(message.getFrom());
				} catch (XMPPException e) {
					throw new MessageProcessingException(e);
				}
			}
		} else {
			log("  user " + user + " has no captcha");
		}
	}

	@Override
	public void setProperty(final String name, final String domain, final String value) {
	}

	@Override
	public void init() {
	}

	@Override
	public void shutdown() {
	}

	private static void log(final String message) {
		System.out.println(message);
	}
}
