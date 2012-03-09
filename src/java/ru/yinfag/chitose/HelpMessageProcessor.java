package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpMessageProcessor implements MessageProcessor {

	private static final Pattern PATTERN = Pattern.compile(
			".*?Chitose.*?(?:[Хх]елп|[Hh]elp)"
	);
	
	private static final String HELP_MESSAGE =
		"\nКидаем кубики: Читосе, кинь 2d6" +
				"\nПостим няшек: Читосе запости няшку" +
				"\nПостим определённую няшку: Читосе, запости misaka_mikoto" +
				"\nНапоминаем о том, что неюка всё зафейлит: Читосе, напомни о \"Неюка всё зафейлит\" через 6 минут" +
				"\nИщем мультики на вротарте: Читосе расскажи про \"НАРУТО\"";

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		final Matcher m = PATTERN.matcher(message.getBody());
		return (m.find()) ? HELP_MESSAGE : null;
	}
}
		
