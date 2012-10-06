package ru.yinfag.chitose.captcha;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import ru.yinfag.chitose.ChatMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.PresenceProcessingException;
import ru.yinfag.chitose.PresenceProcessorPlugin;
import ru.yinfag.chitose.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class CaptchaPlugin implements PresenceProcessorPlugin, MessageSenderPlugin, ChatMessageProcessorPlugin {

	private MessageSender mySender;

	private final Map<String, String> myCaptcha = new HashMap<>();

	@Override
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}

	@Override
	public void processPresence(final Presence presence) throws PresenceProcessingException {
		final String user = presence.getFrom();
		final byte[] bytes = new byte[4];
		ThreadLocalRandom.current().nextBytes(bytes);
		final String captcha = Utils.join(Arrays.asList(bytes), " ");
		myCaptcha.put(user, captcha);
		mySender.sendToUser(user, "Щтобы получить мембера в няшной конфочке " + presence. + " введи ");
	}

	@Override
	public void processChatMessage(final Message message) throws MessageProcessingException {

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
}
