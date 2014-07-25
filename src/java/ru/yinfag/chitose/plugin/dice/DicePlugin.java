package ru.yinfag.chitose.plugin.dice;

import org.jivesoftware.smack.packet.Message;
import ru.yinfag.chitose.ConferenceMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageProcessorUtils;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.NicknameAwarePlugin;
import ru.yinfag.chitose.NicknameByConference;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 14.09.12
 * Time: 15:50
 * To change this template use File | Settings | File Templates.
 */
public class DicePlugin implements ConferenceMessageProcessorPlugin, MessageSenderPlugin, NicknameAwarePlugin {

	private static final String IMPERATIVE = "кинь";

	private static final String G_TIMES = "times";
	private static final String G_DICE = "dice";
	private static final String G_SIDES = "sides";
	private static final String G_MOD = "mod";
	private static final Pattern DICE_SPEC = Pattern.compile(
			"(?:(?<" + G_TIMES + ">\\d++)\\s*+[xх]\\s*+)?" +
					"(?<" + G_DICE + ">\\d++)[dд](?<" + G_SIDES + ">\\d++)" +
					"(?<" + G_MOD + ">[-+]\\d++)?"
	);

	private MessageSender mySender;
	private NicknameByConference myNicknameByConference;

	@Override
	public void setMessageSender(final MessageSender sender) {
		mySender = sender;
	}

	@Override
	public void setNicknameByConference(final NicknameByConference nbc) {
		myNicknameByConference = nbc;
	}

	@Override
	public void setProperty(final String name, final String domain, final String value) {
	}

	public void init() {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getHelpMessage(final String conference) {
		return "Кидаем кубики: " + myNicknameByConference.get(conference) + ", кинь 2d6";
	}

	public void processConferenceMessage(final Message message)
			throws MessageProcessingException {
		final String from = message.getFrom();
		final String conference = from.substring(0, from.indexOf("/"));

		final String command = message.getBody();
		int index = getPreambleEnd(command, conference);
		if (index == -1) {
			return;
		}

		final Matcher m = DICE_SPEC.matcher(command);
		final StringBuilder stringBuilder = new StringBuilder();
		final Random random = ThreadLocalRandom.current();
		while (m.find(index)) {
			try {
				final int times = defaultableIntGroupValue(m, G_TIMES, 1);
				final int dice = defaultableIntGroupValue(m, G_DICE, 0);
				final int sides = defaultableIntGroupValue(m, G_SIDES, 0);
				final int mod = defaultableIntGroupValue(m, G_MOD, 0);
				if (times < 1 || dice < 1 || sides < 1 || times > 50 || dice > 100 || sides > 10000) {
					if (stringBuilder.length() > 0) {
						stringBuilder.append('\n');
					}
					stringBuilder.append(m.group()).append(" = ... я не настолько умная же! >_<\"");
					continue;
				}
				for (int throwIndex = 0; throwIndex < times; throwIndex++) {
					if (stringBuilder.length() > 0) {
						stringBuilder.append('\n');
					}
					stringBuilder.append(dice).append('d').append(sides);
					if (mod > 0) {
						stringBuilder.append('+');
					}
					if (mod != 0) {
						stringBuilder.append(mod);
					}
					stringBuilder.append(" = (");
					int result = 0;
					for (int dieIndex = 0; dieIndex < dice; dieIndex++) {
						final int die = random.nextInt(sides) + 1;
						result += die;
						if (dieIndex != 0) {
							stringBuilder.append(", ");
						}
						stringBuilder.append(die);
					}
					stringBuilder.append(')');
					result += mod;
					if (mod != 0) {
						stringBuilder.append(mod > 0 ? " + " : " - ").append(Math.abs(mod));
					}
					stringBuilder.append(" = ").append(result);
				}
			} catch (NumberFormatException ignore) {
			} finally {
				index = m.end();
			}
		}
		stringBuilder.insert(0, MessageProcessorUtils.getUserNick(message) + ": ");
		mySender.sendToConference(conference, stringBuilder.toString());
	}

	private int getPreambleEnd(final String command, final String conference) {
		int i = command.indexOf(myNicknameByConference.get(conference));
		if (i == -1) {
			return -1;
		}
		i = command.indexOf(IMPERATIVE, i);
		if (i == -1) {
			return -1;
		}
		i = i + IMPERATIVE.length() + 1;
		if (i >= command.length()) {
			return -1;
		}
		return i;
	}

	private int defaultableIntGroupValue(final Matcher m, final String group, final int defaultValue) throws NumberFormatException {
		final String string = m.group(group);
		return string == null ? defaultValue : Integer.parseInt(string);
	}

}
