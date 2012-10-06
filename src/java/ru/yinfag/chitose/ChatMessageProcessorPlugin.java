package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 21:46
 * To change this template use File | Settings | File Templates.
 */
public interface ChatMessageProcessorPlugin extends Plugin {
	void processChatMessage(Message message) throws MessageProcessingException;
}
