package ru.yinfag.chitose.plugin.smooch;

import ru.yinfag.chitose.MessageProcessorPlugin;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.MessageSender;
import org.jivesoftware.smack.packet.Message;


public class SmoochPlugin implements MessageProcessorPlugin, MessageSenderPlugin {
	
	private MessageSender mySender;
	
	@Override
	
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}
	
	public void init() {
		//...
	}
	
	public void processMessage(final Message message) {
		final String messageBody = message.getBody();
		if (messageBody != null && messageBody.contains("*smooch*")) {
			mySender.sendToConference(message.getFrom(), "*nosebleed*");
		}
	}
	
	public void shutdown() {
		//...
	}
	
	
}

