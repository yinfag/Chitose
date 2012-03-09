package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.XMPPException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class TimerMessageProcessor implements MessageProcessor {
	
	private final MultiUserChat muc;
	
	TimerMessageProcessor(final MultiUserChat muc) {
		this.muc = muc;
	}
	
	private static final Pattern p = Pattern.compile(
		".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?напомни.*?о \"(.+?)\" через ([0-9]+).*?(?:(?:минут)|(?:минуты)|(?:минуту))"
	);
	
	private final Map<String, Timer> timers = new HashMap<>();
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final Matcher m = p.matcher(message.getBody());
		
		if (!m.matches()) {
			return null;
		}
		
		final String userNick = MessageProcessorUtils.getUserNick(message);
		
		String timeMinute = m.group(2);
		final String sage = m.group(1);	
		long timeMinuteLong = 0;
		try {
			timeMinuteLong =  Long.parseLong(timeMinute.trim());
		} catch (NumberFormatException e) {
			throw new MessageProcessingException(e);
		}
		final String pseudoJid = message.getFrom();
		boolean oldTimer = timers.containsKey(pseudoJid);
		if (oldTimer) {
			timers.remove(pseudoJid).cancel();
			
		}
		Timer timer = new Timer();
		timers.put(pseudoJid, timer);
		TimerTask task = new TimerTask() {
			public void run()
			{
				timers.remove(pseudoJid);
				try {
					muc.sendMessage(userNick + ": Напоминаю! " + "\n" + sage);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		};
		if (timeMinuteLong != 0 && oldTimer ) {
			long time = timeMinuteLong * 60000;
			timer.schedule(task, time);
			return userNick + ": таймер изменён!";
		} else if (timeMinuteLong != 0) {
			long time = timeMinuteLong * 60000;
			timer.schedule(task, time);
			return userNick + ": окей!";
		} else {
			return null;
		}
	}
}
