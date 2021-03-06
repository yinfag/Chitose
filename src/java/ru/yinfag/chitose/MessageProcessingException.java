package ru.yinfag.chitose;

/**
 * Exception to be thrown from {@link MessageProcessor#process(org.jivesoftware.smack.packet.Message)
 * MessageProcessor.process(Message)} if something goes terribly wrong.
 *
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class MessageProcessingException extends Exception {
	public MessageProcessingException(final String message) {
		super(message);
	}

	public MessageProcessingException(final Throwable cause) {
		super(cause);
	}
}
