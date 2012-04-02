package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class WorldArtMessageProcessor implements MessageProcessor {
	

	private static final String MULTIPLE_TITLES_INDICATOR = "отсортировано по дате выхода";

	private static final Pattern MULTIPLE_TITLE_LIST_ENTRY_PATTERN = Pattern.compile("'estimation'>(.+?)&nbsp");

	private static final Pattern SYNOPSIS_PATTERN = Pattern.compile("Краткое содержание:.*?class='review'>(.+?)</p>");

	private static final String MULTIPLE_SECTIONS_INDICATOR = "<b>Раздел &laquo;анимация&raquo;";

	private static final Pattern TITLE_ENTRY_PATTERN = Pattern.compile("animation/animation.php\\?id=(\\d+)");

	private static final String META_REFRESH = "<meta http-equiv='Refresh' content='0;";
	
	private final Map<String, Properties> perMucProps;
	
	private Pattern COMMAND_PATTERN;

	WorldArtMessageProcessor(final Map<String, Properties> perMucProps) {
		this.perMucProps = perMucProps;
	}
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		final String mucJID = MessageProcessorUtils.getMuc(message);
		final Properties props = perMucProps.get(mucJID);
		final boolean enabled = "1".equals(props.getProperty("urlExpandEnabled"));
		String botname = props.getProperty("nickname");
		String regex = ".*?"+botname+".*?расскажи.*?про \"(.+?)\"";
		COMMAND_PATTERN = Pattern.compile(regex);
		
		if (!enabled) {
			return null;
		}
		
		final String title = getTitle(message.getBody());
		if (title == null) {
			return null;
		}

		final URL worldart;
		try {
			worldart = new URL(
					"http://www.world-art.ru/search.php?public_search=" +
							title +
							"&global_sector=animation"
			);
		} catch (MalformedURLException e) {
			throw new MessageProcessingException(e);
		}

		boolean multipleTitleVariants = false;
		boolean multipleSections = false;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains(MULTIPLE_TITLES_INDICATOR)) {
					multipleTitleVariants = true;
				}
				if (inputLine.contains(MULTIPLE_SECTIONS_INDICATOR)) {
					multipleSections = true;
				}
				if (multipleTitleVariants && multipleSections) {
					break;
				}
			}
		} catch (IOException e) {
			throw new MessageProcessingException(e);
		}

		/*
		 * Examples:
		 * "Air" gives several titles and 2 sections (animation and clips)
		 */

		final String answer;
		if (multipleTitleVariants) {
			final List<String> titles = new ArrayList<>();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					final Matcher matcher = MULTIPLE_TITLE_LIST_ENTRY_PATTERN.matcher(inputLine);
					while (matcher.find()) {
						final String postTitles = matcher.group(1);
						titles.add(postTitles);
					}
				}
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			}
			if (titles.size() > 10) {
				answer = "Найдено " + titles.size() + " результатов, попробуй уточнить свой запрос.";
			} else {
				final String titleList = Utils.join(titles, ", ");
				answer = "Возможно, ты имел в виду: " + titleList;
			}
		} else if (multipleSections) {
			String titleID = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					final Matcher matcher = TITLE_ENTRY_PATTERN.matcher(inputLine);
					if (matcher.find()) {
						titleID = matcher.group(1);
						break;
					}
				}
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			}
			answer = getSynopsis(titleID);
		} else {
			String titleID = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.contains(META_REFRESH)) {
						final Matcher matcher = TITLE_ENTRY_PATTERN.matcher(inputLine);
						if (matcher.find()) {
							titleID = matcher.group(1);
							break;
						}
					}
				}
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			}
			answer = getSynopsis(titleID);
		}

		return MessageProcessorUtils.getUserNick(message) + ": " + answer;
	}
	
	private String getSynopsis(final String titleId) throws MessageProcessingException {
		if (titleId == null) {
			return "Нет такого мультфильма!";
		}
		final URL url;
		try {
			url = new URL("http://www.world-art.ru/animation/animation.php?id=" + titleId);
		} catch (MalformedURLException e) {
			throw new MessageProcessingException(e);
		}
		String synopsis = "Похоже, описание отсутствует. Щто поделать, десу.";
		try (final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "cp1251"))) {
			String line;
			while ((line = in.readLine()) != null) {
				final Matcher matcher = SYNOPSIS_PATTERN.matcher(line);
				if (matcher.find()) {
					synopsis = "\n" + matcher.group(1);
					break;
				}
			}
		} catch (IOException e) {
			throw new MessageProcessingException(e);
		}
		return synopsis.replaceAll("<br>", "\n").replaceAll("\n{2,}", "\n") + "\n" + url;
	}
	
	private String getTitle(final String message) throws MessageProcessingException {
		final Matcher commandMatcher = COMMAND_PATTERN.matcher(message);
		if (!commandMatcher.matches()) {
			return null;
		}
		try {
			return URLEncoder.encode(commandMatcher.group(1), "CP1251");
		} catch (UnsupportedEncodingException e){
			throw new MessageProcessingException(e);
		}
	}
}
	
