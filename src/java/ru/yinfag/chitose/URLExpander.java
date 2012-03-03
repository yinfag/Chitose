package ru.yinfag.chitose;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jivesoftware.smack.packet.Message;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.HttpURLConnection;



public class URLExpander implements MessageProcessor {
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		Pattern p = Pattern.compile("http://goo\\.gl/\\w+");
		final Matcher m = p.matcher(message.getBody());
		StringBuilder urlExpanderSB = null;
		while (m.find()) {
			if (urlExpanderSB == null) {
				urlExpanderSB = new StringBuilder("Короткие урлы ведут сюда:");
			}
			final String shortUrlString = m.group(0);
			urlExpanderSB.append("\n").append(shortUrlString).append(" -> ");
			final URL shortUrl;
			try {
				shortUrl = new URL(shortUrlString);
			} catch (MalformedURLException e) {
				urlExpanderSB.append("(плохой урл почему-то)");
				continue;
			}
			final HttpURLConnection con;
			try {
				con = ((HttpURLConnection) shortUrl.openConnection());
				con.setInstanceFollowRedirects(false);
				con.connect();
			} catch (IOException e) {
				log("Не получилось открыть соединение", e);
				urlExpanderSB.append("(не удалось открыть соединение)");
				continue;
			}
			urlExpanderSB.append(con.getHeaderField("Location"));
		}
		return urlExpanderSB;
	}
	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}
}
