package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class MessageProcessorUtils {
	public static final Pattern USER_NICK_PATTERN = Pattern.compile("[^@]+@[^/]+/(.*)");
	public static final Pattern CONFERENCE_JID_PATTERN = Pattern.compile("^([^\\/]+).*");

	static String getUserNick(final Message message) throws MessageProcessingException {
		final Matcher matcher = USER_NICK_PATTERN.matcher(message.getFrom());
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new MessageProcessingException("Failed to parse nickname from message's author");
		}
	}
	
	static String getMuc(final Message message) throws MessageProcessingException {
		final Matcher matcher = CONFERENCE_JID_PATTERN.matcher(message.getFrom());
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new MessageProcessingException("Failed to parse nickname from message's author");
		}
	}
}
