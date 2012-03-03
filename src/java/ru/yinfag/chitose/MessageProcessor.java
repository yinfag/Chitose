package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

/**
 * Base interface for classes that define how Chitose responds to various messages.
 *
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public interface MessageProcessor {

	/**
	 * Attempt to process the message.
	 *
	 * @param message    the message to react to.
	 * @return <code>null</code> if the message can't be processed by this MessageProcessor;
	 * otherwise, the response to be sent back.
	 * @throws MessageProcessingException if something goes wrong during the attempt to process the message.
	 */
	CharSequence process(final Message message) throws MessageProcessingException;
}
