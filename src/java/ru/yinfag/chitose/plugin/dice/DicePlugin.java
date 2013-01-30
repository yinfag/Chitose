package ru.yinfag.chitose.plugin.dice;

import org.jivesoftware.smack.packet.Message;
import ru.yinfag.chitose.ConferenceMessageProcessorPlugin;
import ru.yinfag.chitose.MessageProcessingException;
import ru.yinfag.chitose.MessageProcessorUtils;
import ru.yinfag.chitose.MessageSender;
import ru.yinfag.chitose.MessageSenderPlugin;
import ru.yinfag.chitose.NicknameAwarePlugin;
import ru.yinfag.chitose.NicknameByConference;

import java.util.HashMap;
import java.util.Map;
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

	private MessageSender mySender;
	private NicknameByConference myNicknameByConference;

	private final Map<String, Pattern> patternByConference = new HashMap<>();

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

		final Pattern commandPattern;
		if (patternByConference.containsKey(conference)) {
			System.out.println(" using cached pattern");
			commandPattern = patternByConference.get(conference);
		} else {
			final String nick = myNicknameByConference.get(conference);
			System.out.println(" pattern cache miss; creating new for nick " + nick);
			patternByConference.put(
					conference,
					commandPattern = Pattern.compile(
							".*?" + nick + ".*?кинь.*?(\\d+)[dд](\\d+)"
					)
			);
		}

		final Matcher m = commandPattern.matcher(message.getBody());
		if (!m.matches()) {
			return;
		}

		final String userNick = MessageProcessorUtils.getUserNick(message);

		final String dcString = m.group(1);
		final String dsString = m.group(2);
		if (dcString.length() > 2 || dsString.length() > 5) {
			mySender.sendToConference(
					conference,
					userNick + ": я не настолько умная же! >_<\""
			);
			return;
		}

		final int diceCount = Integer.parseInt(dcString);
		final int dieSides = Integer.parseInt(dsString);
		mySender.sendToConference(conference, userNick + ": " + doThrowDice(diceCount, dieSides));
	}

	public static CharSequence doThrowDice(final int diceCount, final int dieSides) {
		final Random random = ThreadLocalRandom.current();
		final StringBuilder stringBuilder = new StringBuilder()
				.append(diceCount).append("d").append(dieSides).append(" = ");
		int result = 0;
		for (int i = 0; i < diceCount; i++) {
			final int die = random.nextInt(dieSides) + 1;
			result += die;
			if (i != 0) {
				stringBuilder.append(" + ");
			}
			stringBuilder.append(die);
		}
		if (diceCount > 1) {
			stringBuilder.append(" = ").append(result);
		}
		return stringBuilder;
	}

}
