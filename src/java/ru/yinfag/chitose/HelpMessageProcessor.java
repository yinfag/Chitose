package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpMessageProcessor implements MessageProcessor {

	private static final Pattern p = Pattern.compile(
		".*?Chitose.*?(?:[Хх]елп|[Hh]elp)"
		);
	
	private static final String help =  
		" \n" +
		"Кидаем кубики: Читосе, кинь 2d6\n" +
		"Постим няшек: Читосе запости няшку\n" +
		"Постим определённую няшку: Читосе, запости misaka_mikoto\n" +
		"Напоминаем о том, что неюка всё зафейлит: Читосе, напомни о \"Неюка всё зафелит\" через 6 минут \n" +
		"Ищем мультики на вротарте: Читосе расскажи про \"НАРУТО\"";
	
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		Matcher m = p.matcher(message.getBody());
		return (!m.find()) ? null : help;
	}
}
		
