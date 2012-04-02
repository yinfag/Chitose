package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class HelpMessageProcessor implements MessageProcessor {

	private final boolean enabled;
	private final Pattern PATTERN;
	private final String helpMessage;
	
	HelpMessageProcessor(final Properties mucProps) {
		enabled = "1".equals(mucProps.getProperty("Help"));
		String botname = mucProps.getProperty("nickname");
		PATTERN = Pattern.compile(
			".*?" + botname + ".*?(?:[Хх]елп|[Hh]elp)"
		);
		helpMessage =
		"\nКидаем кубики: "+botname+", кинь 2d6" +
				"\nПостим няшек: "+botname+" запости няшку" +
				"\nПостим определённую няшку: "+botname+", запости misaka_mikoto" +
				"\nНапоминаем о том, что неюка всё зафейлит: "+botname+", напомни о \"Неюка всё зафейлит\" через 6 минут" +
				"\nИщем мультики на вротарте: "+botname+" расскажи про \"НАРУТО\"";
		
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		if (!enabled) {
			return null;
		}
		final Matcher m = PATTERN.matcher(message.getBody());
		return (m.find()) ? helpMessage : null;
	}
}
		
