package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class GelbooruMessageProcessor implements MessageProcessor {

	private static final Pattern PICTURE_LIST_ENTRY_PATTERN = Pattern.compile("sample_url=\"(.+?)\"");
	private final Map<String, Properties> perMucProps;
	
	GelbooruMessageProcessor(final Map<String, Properties> perMucProps) {
				this.perMucProps = perMucProps;
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		final String mucJID = MessageProcessorUtils.getMuc(message);
		final Properties props = perMucProps.get(mucJID);
		final boolean enabled = "1".equals(props.getProperty("Gelbooru"));
		final String botname = props.getProperty("nickname");
		final Pattern COMMAND_PATTERN = Pattern.compile(
			".*?" + botname + ".*?(?:(?:запости)|(?:доставь)).+?([\\w().*+]+|(?:няшку))[.!]?"
		);
		
		if (!enabled) {
			return null;
		}
		
		final Matcher m = COMMAND_PATTERN.matcher(message.getBody());
		if (!m.matches()) {
			return null;
		}

		final String userNick = MessageProcessorUtils.getUserNick(message);

		final String nyakaName;
		if ("няшку".equals(m.group(1))) {
			nyakaName = "";
		} else {
			nyakaName = m.group(1);
		}
		if (nyakaName == null) {
			return userNick + ": у няшек тихий час, их нельзя постить.";
		}

		final List<String> urls = getGelbooruLinks(nyakaName);
		if (urls.size() == 0) {
			return userNick + ": " + (
					(nyakaName.length() > 0) ?
							(nyakaName + " не няшка!") :
							"няшки кончились."
			);
		} else {
			return userNick + ": " + urls.get(ThreadLocalRandom.current().nextInt(urls.size()));
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

}
	
