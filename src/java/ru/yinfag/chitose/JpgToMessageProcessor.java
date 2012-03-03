package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class JpgToMessageProcessor implements MessageProcessor {

	private static final Pattern COMMAND_PATTERN = Pattern.compile(".*?([А-Яа-яA-Za-z_ё]+?)\\.(?:(?:жпг)|(?:жпег)|(?:jpg)|(?:пнг)|(?:гиф))");

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final Matcher matcher = COMMAND_PATTERN.matcher(message.getBody());
		if (!matcher.matches()) {
			return null;
		}
		return "http://" + matcher.group(1) + ".jpg.to/";
	}
}
