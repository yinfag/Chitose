package ru.yinfag.chitose;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerMessageProcessor implements MessageProcessor {

	private static final Pattern p = Pattern.compile(
			".*?(?:Chitose|[Чч]итосе).*?напомни.*?о \"(.+?)\" через ([1-9][0-9]*).*?(?:минут[ыу]?)"
	);

	private final MultiUserChat muc;

	TimerMessageProcessor(final MultiUserChat muc) {
		this.muc = muc;
	}

	private final Map<String, Timer> timers = new HashMap<>();
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final Matcher m = p.matcher(message.getBody());
		
		if (!m.matches()) {
			return null;
		}
		
		final String sage = m.group(1);

		final String timeMinute = m.group(2);
		final long timeMinuteLong;
		try {
			timeMinuteLong = Long.parseLong(timeMinute.trim());
		} catch (NumberFormatException e) {
			// we could reply with an error message
			// but our regex shouldn't even allow this to be thrown
			throw new MessageProcessingException(e);
		}

		final String pseudoJid = message.getFrom();
		final boolean oldTimer = timers.containsKey(pseudoJid);
		if (oldTimer) {
			timers.remove(pseudoJid).cancel();
		}

		final String userNick = MessageProcessorUtils.getUserNick(message);

		final Timer timer = new Timer();
		timers.put(pseudoJid, timer);
		final TimerTask task = new TimerTask() {
			public void run() {
				timers.remove(pseudoJid);
				try {
					muc.sendMessage(userNick + ": Напоминаю!\n" + sage);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		};

		timer.schedule(task, timeMinuteLong * 60000);
		return userNick + ": " + (oldTimer ? "таймер изменён!" : "окей!");
	}
}
