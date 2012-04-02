package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class SmoochMessageProcessor implements MessageProcessor {

	private final boolean enabled;
	
	public SmoochMessageProcessor(final Properties mucProps) {
		enabled = "1".equals(mucProps.getProperty("Smooch"));
	}


	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		if (!enabled) {
			return null;
		}
		
		
		final String messageBody = message.getBody();
		return (messageBody != null && messageBody.contains("*smooch*")) ? "*nosebleed*" : null;
	}
}
