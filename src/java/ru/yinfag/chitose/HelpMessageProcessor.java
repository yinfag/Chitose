package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

public class HelpMessageProcessor implements MessageProcessor {

	private final Pattern PATTERN;
	private final String HELP_MESSAGE;
	
	HelpMessageProcessor(final Properties props) {
		String botname = props.getProperty("nickname");
		String regex = ".*?" + botname + ".*?(?:[Хх]елп|[Hh]elp)";
		PATTERN = Pattern.compile(regex);
		HELP_MESSAGE =
		"\nКидаем кубики: "+botname+", кинь 2d6" +
				"\nПостим няшек: "+botname+" запости няшку" +
				"\nПостим определённую няшку: "+botname+", запости misaka_mikoto" +
				"\nНапоминаем о том, что неюка всё зафейлит: "+botname+", напомни о \"Неюка всё зафейлит\" через 6 минут" +
				"\nИщем мультики на вротарте: "+botname+" расскажи про \"НАРУТО\"";
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final Matcher m = PATTERN.matcher(message.getBody());
		return (m.find()) ? HELP_MESSAGE : null;
	}
}
		
