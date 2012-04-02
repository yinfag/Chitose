package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class URLExpander implements MessageProcessor {

	private static final Pattern PATTERN = Pattern.compile("http://goo\\.gl/\\w+");
	private final Map<String, Properties> perMucProps;

	public URLExpander(final Map<String, Properties> perMucProps) {
		this.perMucProps = perMucProps;
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		final String mucJID = MessageProcessorUtils.getMuc(message);
		final Properties props = perMucProps.get(mucJID);
		final boolean enabled = "1".equals(props.getProperty("urlExpandEnabled"));
		
		if (!enabled) {
			return null;
		}

		final Matcher m = PATTERN.matcher(message.getBody());
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
				log("Не получилось открыть соединение для " + shortUrlString, e);
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
