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
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class WorldArtMessageProcessor implements MessageProcessor {
	
	private static final Pattern p = Pattern.compile(
			"(?:(?:Chitose)|(?:[Чч]итосе)).*?расскажи.*?про \"(.+?)\""
	);
	private static final String p1 = "отсортировано по дате выхода";
	private static final Pattern p2 = Pattern.compile("'estimation'>(.+?)&nbsp");
	private static final Pattern p3 = Pattern.compile("Краткое содержание:.*?class='review'>(.+?)</p>");
	private static final String p4 = "<b>Раздел &laquo;анимация&raquo;";
	private static final Pattern p5 = Pattern.compile("animation/animation.php\\?id=(\\d+)");
	private static final String p6 = "<meta http-equiv='Refresh' content='0;";
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		Matcher m = p.matcher(message.getBody());
		if (!m.matches()) {
			return null;
		}
		
		final String userNick = MessageProcessorUtils.getUserNick(message);
		
		URL worldart;
		String title = "";
		try {
			title = URLEncoder.encode(m.group(1), "CP1251");
		} catch (UnsupportedEncodingException e){
			throw new MessageProcessingException(e);
		}
		try {
			worldart = new URL("http://www.world-art.ru/search.php?public_search=" +title+"&global_sector=animation");
		} catch (MalformedURLException e) {
			throw new MessageProcessingException(e);
		}
		boolean multipleTitleVariants = false;
		boolean animationPresent = false;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains(p1)) {
					multipleTitleVariants = true;
				}
				if (inputLine.contains(p4)) {
						animationPresent = true;
				}
				if (multipleTitleVariants && animationPresent) {
					break;
				}
			}
		} catch (IOException e) {
			throw new MessageProcessingException(e);
		}
		System.out.format("MTV: %b; AP: %b%n", multipleTitleVariants, animationPresent);
		if (multipleTitleVariants) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				final List<String> titles = new ArrayList<String>();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					Matcher m2 = p2.matcher(inputLine);
					while (m2.find()) {
						final String postTitles = m2.group(1);
						titles.add(postTitles);
					}
				}
				String titleList = Utils.join(titles, ", ");
				if (titles.size() > 10) {
					return "Найдено "+titles.size()+" результатов, попробуй уточнить свой запрос.";
				} else {
					return "Возможно ты имел ввиду: "+titleList;
				}
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			}
		} else if (animationPresent) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				String inputLine;
				String titleID = null;
				URL worldart1;
				while ((inputLine = in.readLine()) != null) {
					Matcher m5 = p5.matcher(inputLine);
					if (m5.find()) {
						titleID = m5.group(1);
					}
				}
				try {
					worldart1 = new URL("http://www.world-art.ru/animation/animation.php?id="+titleID);
				} catch (MalformedURLException e) {
					throw new MessageProcessingException(e);
				}
				String synopsis1 = "Похоже описание отсутствует. Щто поделать, десу.";
				try (BufferedReader in1 = new BufferedReader(new InputStreamReader(worldart1.openStream(), "cp1251"))) {
					String inputLine1;
					while ((inputLine1 = in1.readLine()) != null) {
						Matcher m3 = p3.matcher(inputLine1);
						if (m3.find()) {
							synopsis1 = m3.group(1);
						}
					}
				}
				return synopsis1.replaceAll("\\<br\\>", "\n") + "\n" +worldart1;
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			}
		} else {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
				String inputLine;
				URL worldart1;
				String titleID = null;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.contains(p6)) {
						Matcher m5 = p5.matcher(inputLine);
						if (m5.find()) {
							titleID = m5.group(1);
						}
					}
				}
				try {
					worldart1 = new URL("http://www.world-art.ru/animation/animation.php?id="+titleID);
				} catch (MalformedURLException e) {
					throw new MessageProcessingException(e);
				}
				String synopsis = "Нет такого мультфильма!";
				try (BufferedReader in1 = new BufferedReader(new InputStreamReader(worldart1.openStream(), "cp1251"))) {
					String inputLine1;
					while ((inputLine1 = in1.readLine()) != null) {
						Matcher m3 = p3.matcher(inputLine1);
						if (m3.find()) {
							synopsis = m3.group(1);
						}
					}
				}
				if (titleID == null) {
					return synopsis;
				} else {
					return synopsis.replaceAll("\\<br\\>", "\n") + "\n" +worldart;
				}
			} catch (IOException e) {
				throw new MessageProcessingException(e);
			} 
		}
	}
}
	
