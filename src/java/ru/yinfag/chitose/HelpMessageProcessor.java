package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public class HelpMessageProcessor implements MessageProcessor {


	private final Map<String, Properties> perMucProps;
	
	HelpMessageProcessor(Map<String, Properties> perMucProps) {
		this.perMucProps = perMucProps;
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		final String mucJID = MessageProcessorUtils.getMuc(message);
		final Properties props = perMucProps.get(mucJID);
		final boolean enabled = "1".equals(props.getProperty("Help"));
		String botname = props.getProperty("nickname");
		String regex = ".*?" + botname + ".*?(?:[Хх]елп|[Hh]elp)";
		final Pattern PATTERN = Pattern.compile(regex);
		final String HELP_MESSAGE =
		"\nКидаем кубики: "+botname+", кинь 2d6" +
				"\nПостим няшек: "+botname+" запости няшку" +
				"\nПостим определённую няшку: "+botname+", запости misaka_mikoto" +
				"\nНапоминаем о том, что неюка всё зафейлит: "+botname+", напомни о \"Неюка всё зафейлит\" через 6 минут" +
				"\nИщем мультики на вротарте: "+botname+" расскажи про \"НАРУТО\"";
		
		if (!enabled) {
			return null;
		}
		final Matcher m = PATTERN.matcher(message.getBody());
		return (m.find()) ? HELP_MESSAGE : null;
	}
}
		
