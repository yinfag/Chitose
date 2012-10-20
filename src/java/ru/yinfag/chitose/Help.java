package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 07.10.12
 * Time: 17:45
 * To change this template use File | Settings | File Templates.
 */
public class Help implements ConferenceMessageProcessorPlugin, MessageSenderPlugin, NicknameAwarePlugin {

	private MessageSender mySender;
	private NicknameByConference myNicknameByConference;
	private final Map<String, Pattern> patternByConference = new HashMap<>();
	private final List <Plugin> plugins;

	public Help(final List<Plugin> plugins) {
		this.plugins = plugins;
	}

	@Override
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}

	@Override
	public void setNicknameByConference(final NicknameByConference nbc) {
		myNicknameByConference = nbc;
	}

	@Override
	public void setProperty(final String name, final String domain, final String value) {

	}

	@Override
	public void processConferenceMessage(final Message message) throws MessageProcessingException {
		final String from = message.getFrom();
		final String conference = from.substring(0, from.indexOf("/"));
		final String nick = myNicknameByConference.get(conference);

		final Pattern commandPattern;
		if (patternByConference.containsKey(conference)) {
			System.out.println(" using cached pattern");
			commandPattern = patternByConference.get(conference);
		} else {
			System.out.println(" pattern cache miss; creating new for nick " + nick);
			patternByConference.put(
					conference,
					commandPattern = Pattern.compile(
							".*?" + nick + ".*?(?:[Хх]елп|[Hh]elp)"
					)
			);
		}

		final Matcher m = commandPattern.matcher(message.getBody());
		if (!m.matches()) {
			return;
		}

		final StringBuilder sb = new StringBuilder("Я много чего умею! Надо только правильно попросить.");
		if (plugins.size() > 0) {
			for (final Plugin plugin : plugins) {
				final String helpMessage = plugin.getHelpMessage(conference);
				if (helpMessage != null) {
					sb.append("\n").append(helpMessage);
				}
			}
			mySender.sendToConference(conference, sb.toString());
		} else {
			mySender.sendToConference(conference, "Ни одного няшного плагина нет!");
		}
	}

	@Override
	public void init() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public String getHelpMessage(final String conference) {
		return null;
	}
}
