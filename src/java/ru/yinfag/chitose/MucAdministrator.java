package ru.yinfag.chitose;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.Occupant;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 07.10.12
 * Time: 0:49
 * To change this template use File | Settings | File Templates.
 */
public interface MucAdministrator {

	Occupant getOccupantData(String user);

	// todo: async & remove throws
	void grantVoice(String user) throws XMPPException;
}
