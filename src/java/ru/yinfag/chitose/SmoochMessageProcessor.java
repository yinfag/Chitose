package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class SmoochMessageProcessor implements MessageProcessor {
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final String messageBody = message.getBody();
		return (messageBody != null && messageBody.contains("*smooch*")) ? "*nosebleed*" : null;
	}
}
