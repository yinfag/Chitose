package ru.yinfag.chitose;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 20:17
 * To change this template use File | Settings | File Templates.
 */
public class PresenceProcessingException extends Exception {
	public PresenceProcessingException(final String message) {
		super(message);
	}

	public PresenceProcessingException(final Throwable cause) {
		super(cause);
	}
}
