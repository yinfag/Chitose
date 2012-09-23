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

	@Override
	public void setProperty(final String name, final String domain, final String value) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void init() {
		//...
	}
	
	public void processMessage(final Message message) {
		final String messageBody = message.getBody();
		if (messageBody != null && messageBody.contains("*smooch*")) {
			final String from = message.getFrom();
			mySender.sendToConference(from.substring(0, from.indexOf("/")), "*nosebleed*");
		}
	}
	
	public void shutdown() {
		//...
	}
	
	
}

