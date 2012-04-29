package ru.yinfag.chitose;

import org.jivesoftware.smack.packet.Message;


import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yanus Poluektovich (ypoluektovich@gmail.com)
 */
public class DiceMessageProcessor implements MessageProcessor {

	private final Pattern COMMAND_PATTERN;
	private final boolean enabled;

	DiceMessageProcessor(final Properties mucProps) {
		enabled = "1".equals(mucProps.getProperty("Dice"));
		final String botname = mucProps.getProperty("nickname");
		COMMAND_PATTERN = Pattern.compile(
			".*?" + botname + ".*?кинь.*?(\\d+)[dд](\\d+)"
		);
	}

	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		if (!enabled) {
			return null;
		}
		
		final Matcher m = COMMAND_PATTERN.matcher(message.getBody());
		if (!m.matches()) {
			return null;
		}

		final String userNick = MessageProcessorUtils.getUserNick(message);


		final String dcString = m.group(1);
		final String dsString = m.group(2);
		if (dcString.length() > 4 || dsString.length() > 5) {
			return userNick + ": я не настолько умная же! >_<\"";
		}

		final int diceCount = Integer.parseInt(dcString);
		final int dieSides = Integer.parseInt(dsString);
		final Random random = ThreadLocalRandom.current();
		int result = 0;
		for (int i = 0; i < diceCount; i++) {
			result += random.nextInt(dieSides) + 1;
		}
		return userNick + ": Выпало " + result + ", такие дела, нян!";
	}
}
