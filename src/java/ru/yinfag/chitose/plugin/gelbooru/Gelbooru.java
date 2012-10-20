package ru.yinfag.chitose.plugin.gelbooru;

import org.jivesoftware.smack.packet.Message;
import ru.yinfag.chitose.ConferenceMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageProcessorUtils;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.NicknameAwarePlugin;
import ru.yinfag.chitose.NicknameByConference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 07.10.12
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */
public class Gelbooru implements ConferenceMessageProcessorPlugin, MessageSenderPlugin, NicknameAwarePlugin {

	private MessageSender mySender;
	private NicknameByConference myNicknameByConference;
	private static final Pattern PICTURE_LIST_ENTRY_PATTERN = Pattern.compile("sample_url=\"(.+?)\"");
	private final Map<String, Pattern> patternByConference = new HashMap<>();

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

		final Pattern commandPattern;
		if (patternByConference.containsKey(conference)) {
			System.out.println(" using cached pattern");
			commandPattern = patternByConference.get(conference);
		} else {
			final String nick = myNicknameByConference.get(conference);
			System.out.println(" pattern cache miss; creating new for nick " + nick);
			patternByConference.put(
					conference,
					commandPattern = Pattern.compile(
							".*?" + nick + ".*?(?:(?:запости)|(?:доставь)).+?([\\w().*+]+|(?:няшку))[.!]?"
					)
			);
		}
		final Matcher m = commandPattern.matcher(message.getBody());
		if (!m.matches()) {
			return;
		}

		final String userNick = MessageProcessorUtils.getUserNick(message);

		final String nyakaName;
		if ("няшку".equals(m.group(1))) {
			nyakaName = "";
		} else {
			nyakaName = m.group(1);
		}
		if (nyakaName == null) {
			mySender.sendToConference(conference, userNick + ": у няшек тихий час, их нельзя постить.");
		}

		final List<String> urls = getGelbooruLinks(nyakaName);
		if (urls.size() == 0) {
			mySender.sendToConference(conference, userNick + ": " + (
					(nyakaName.length() > 0) ?
							(nyakaName + " не няшка!") :
							"няшки кончились.")
			);
		} else {
			mySender.sendToConference(conference,
					userNick + ": " + urls.get(ThreadLocalRandom.current().nextInt(urls.size()))
			);
		}

	}

	private List<String> getGelbooruLinks(final String nyakaName) throws MessageProcessingException {
		final URL gelbooru;
		try {
			gelbooru = new URL(
					"http://gelbooru.com/index.php?page=dapi&s=post&q=index&limit=10000&tags=solo+" +
							nyakaName
			);
		} catch (MalformedURLException e) {
			throw new MessageProcessingException(e);
		}
		final List<String> urls = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(gelbooru.openStream()))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				final Matcher entryMatcher = PICTURE_LIST_ENTRY_PATTERN.matcher(inputLine);
				while (entryMatcher.find()) {
					final String postUrl = String.format(entryMatcher.group(1));
					urls.add(postUrl);
				}
			}
		} catch (IOException e) {
			throw new MessageProcessingException(e);
		}
		return urls;
	}

	@Override
	public void init() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public String getHelpMessage(final String conference) {
	    final String nick =  myNicknameByConference.get(conference);
		return "Постим няшек: "+nick+" запости няшку" +
						"\nПостим определённую няшку: "+nick+", запости misaka_mikoto";
	}
}
