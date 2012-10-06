package ru.yinfag.chitose;


public interface MessageSender {
	
	void sendToConference(final String conference, final String text);

	void sendToConference(final String conference, final String text, final String xhtml);

	void sendToUser(final String user, final String message);
	
}
