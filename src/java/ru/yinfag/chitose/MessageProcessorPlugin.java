package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;


public interface MessageProcessorPlugin extends Plugin {
    void processMessage(Message message) throws MessageProcessingException;
}
