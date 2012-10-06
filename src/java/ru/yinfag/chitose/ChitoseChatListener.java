package ru.yinfag.chitose;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class ChitoseChatListener implements MessageListener {

	private final List<ChatMessageProcessorPlugin> myChatMessageProcessors;

	public ChitoseChatListener(final List<ChatMessageProcessorPlugin> chatMessageProcessors) {
		myChatMessageProcessors = chatMessageProcessors;
	}

	@Override
	public void processMessage(final Chat chat, final Message message) {
		log("private message from " + message.getFrom());
		for (final ChatMessageProcessorPlugin processor : myChatMessageProcessors) {
			try {
				processor.processChatMessage(message);
			} catch (MessageProcessingException e) {
				log("Error while processing private message", e);
			}
		}
	}

	private static void log(final String message, final Exception e) {
		log(message);
		if (e != null) {
			e.printStackTrace();
		}
	}

	private static void log(final String message) {
		System.out.println(message);
	}

}
