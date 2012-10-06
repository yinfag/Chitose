package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;


public interface ConferenceMessageProcessorPlugin extends Plugin {
    void processConferenceMessage(Message message) throws MessageProcessingException;
}
