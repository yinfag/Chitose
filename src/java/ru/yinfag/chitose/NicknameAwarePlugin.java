package ru.yinfag.chitose;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 23.09.12
 * Time: 18:44
 * To change this template use File | Settings | File Templates.
 */
public interface NicknameAwarePlugin extends Plugin {
	void setNicknameByConference(final NicknameByConference nbc);
}
