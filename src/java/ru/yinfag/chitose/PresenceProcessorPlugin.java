package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Presence;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
public interface PresenceProcessorPlugin extends Plugin {
	boolean isProcessingOwnPresence();

	void processPresence(Presence presence) throws PresenceProcessingException;
}
