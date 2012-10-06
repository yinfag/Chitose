package ru.yinfag.chitose;


public interface MessageSender {
	
	void sendToConference(final String conference, final String message);

	void sendToUser(final String user, final String message);
	
}
