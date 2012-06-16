package ru.yinfag.chitose.plugin.smooch;

import ru.yinfag.chitose.MessageProcessorPlugin;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.MessageSender;

public class SmoochPlugin implements MessageProcessorPlugin, MessageSenderPlugin {
	
	
	@Override
	public void processMessage() {
		//...
	}
	
	public void init() {
		//...
	}
	
	public void shutdown() {
		//...
	}
	
	public void setMessageSender(final MessageSender sender) {
		//...
	}
	
}

