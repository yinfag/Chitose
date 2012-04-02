package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class SmoochMessageProcessor implements MessageProcessor {

	private final Map<String, Properties> perMucProps;
	
	public SmoochMessageProcessor(final Map<String, Properties> perMucProps) {
		this.perMucProps = perMucProps;
	}


	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		final String mucJID = MessageProcessorUtils.getMuc(message);
		final Properties props = perMucProps.get(mucJID);
		final boolean enabled = "1".equals(props.getProperty("Smooch"));
		
		if (!enabled) {
			return null;
		}
		
		
		final String messageBody = message.getBody();
		return (messageBody != null && messageBody.contains("*smooch*")) ? "*nosebleed*" : null;
	}
}
