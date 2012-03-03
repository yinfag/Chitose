package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

public class GelbooruMessageProcessor implements MessageProcessor {
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		Pattern p = Pattern.compile(".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?(?:(?:запости)|(?:доставь)).*?(?:([\\w().*\\+]+?)|(няшку))\\.?$");
		Pattern p2 = Pattern.compile("sample_url=\"(.+?)\"");
		Matcher m = p.matcher(message.getBody());
		if (m.matches()) {

			Pattern p1 = Pattern.compile(".+?@.+?\\..+?\\..+?/(.+?)");
			Matcher m1 = p1.matcher(message.getFrom());
			String nyasha = "";
			if (m1.matches()) {
				nyasha = m1.group(1);
			}
			
			URL gelbooru;
			URLConnection c;
			String nyakaName;
			if ("няшку".equals(m1.group(2))) {
				nyakaName = "";
			} else {
				nyakaName = m1.group(1);
			}
			try {
				gelbooru = new URL("http://gelbooru.com/index.php?page=dapi&s=post&q=index&limit=10000&tags=solo+"+nyakaName);
			} catch (MalformedURLException e) {
				log("Не получилось составить урл для запроса в гелбуру", e);
				return nyasha + ": Ты ж бота сломал, бака!";
			}
			try {
				c = gelbooru.openConnection();
			} catch (IOException e) {
				log("Не получилось открыть соединение с гелбуру", e);
				return "Не получилось открыть соединение с гелбуру";
			}
			c.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.77 Safari/535.7");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
				String inputLine;
				final List<String> urls = new ArrayList<String>();
				while ((inputLine = in.readLine()) != null) {
					Matcher m2 = p2.matcher(inputLine);
					if (!m2.find()) {
						continue;
					}
					final String postUrl = String.format(m2.group(1));
					urls.add(postUrl);
				}
				if (urls.size() == 0) {
					msg = nyasha + ": " + m.group(1) +" не няшка!";
				} else {
					Random random = new Random();
					msg = nyasha + ": " + urls.get(random.nextInt(urls.size()));
				}	
			} catch (IOException e) {
				 log("Ошибка ввода-вывода при чтении страницы", e);
				 return "Няшки закрыты на ремонт.";
			}
		}
	}
	
	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}
}
	
