package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class JpgToMessageProcessor implements MessageProcessor {

	private static final Pattern COMMAND_PATTERN = Pattern.compile("(?<!http)([А-Яа-яA-Za-z_ё]+?)\\.(?:(?:жпг)|(?:жпег)|(?:jpg)|(?:пнг)|(?:гиф))");
	private final boolean enabled;

	public JpgToMessageProcessor(final Properties mucProps) {
		enabled = "1".equals(mucProps.getProperty("JpgTo"));	
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		
		if (!enabled) {
			return null;
		}
		
		final Matcher matcher = COMMAND_PATTERN.matcher(message.getBody());
		if (!matcher.matches()) {
			return null;
		}
		return "http://" + matcher.group(1) + ".jpg.to/";
	}
}
