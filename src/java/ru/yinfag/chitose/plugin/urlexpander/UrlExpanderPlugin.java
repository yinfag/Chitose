package ru.yinfag.chitose.plugin.urlexpander;

import org.jivesoftware.smack.packet.Message;
import ru.yinfag.chitose.ConferenceMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 06.10.12
 * Time: 22:15
 * To change this template use File | Settings | File Templates.
 */
public class UrlExpanderPlugin implements ConferenceMessageProcessorPlugin, MessageSenderPlugin {
	public static final Pattern COMMAND_PATTERN = Pattern.compile("http://goo\\.gl/\\w+");

	private MessageSender mySender;

	@Override
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}

	@Override
	public void processConferenceMessage(final Message message) throws MessageProcessingException {
		final String from = message.getFrom();
		final String conference = from.substring(0, from.indexOf("/"));

		final Matcher m = COMMAND_PATTERN.matcher(message.getBody());

		StringBuilder textBody = null;
		StringBuilder xhtmlBody = null;
		while (m.find()) {
			if (textBody == null) {
				textBody = new StringBuilder("Короткие урлы ведут сюда:");
				xhtmlBody = new StringBuilder("<body>Короткие урлы ведут сюда:");
			}
			final String shortUrlString = m.group(0);
			textBody.append("\n").append(shortUrlString).append(" -> ");
			xhtmlBody.append("<br/>").append(shortUrlString).append(" -> ");
			final URL shortUrl;
			try {
				shortUrl = new URL(shortUrlString);
			} catch (MalformedURLException e) {
				textBody.append("(плохой урл почему-то)");
				xhtmlBody.append("(плохой урл почему-то)");
				continue;
			}
			final HttpURLConnection con;
			try {
				con = ((HttpURLConnection) shortUrl.openConnection());
				con.setInstanceFollowRedirects(false);
				con.connect();
			} catch (IOException e) {
				log("Не получилось открыть соединение для " + shortUrlString, e);
				textBody.append("(не удалось открыть соединение)");
				xhtmlBody.append("(не удалось открыть соединение)");
				continue;
			}
			final String location = con.getHeaderField("Location");
			textBody.append(location);
			xhtmlBody.append("<a href=\"").append(location).append("\">сюда</a>");
		}
		if (textBody != null) {
			xhtmlBody.append("</body>");
			mySender.sendToConference(conference, textBody.toString(), xhtmlBody.toString());
		}
	}

	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}

	@Override
	public void setProperty(final String name, final String domain, final String value) {
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
