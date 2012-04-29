package ru.yinfag.chitose;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jivesoftware.smack.packet.Message;
import java.util.Properties;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import javax.mail.Message.RecipientType;


public class MailMessageProcessor implements MessageProcessor {
	
	private final boolean enabled;
	private final Pattern pattern;
	private final String host;
	private final String user;
	private final String password;
	
	MailMessageProcessor (final Properties mucProps, final Properties props) {
		host = props.getProperty("mail.host");
		user = props.getProperty("mail.user");
		password = props.getProperty("mail.password");
		enabled = "1".equals(mucProps.getProperty("Mail"));
		final String botname = mucProps.getProperty("nickname");
		pattern = Pattern.compile(
			".*?" + botname + ".* отправь сообщение \"(.+?)\" на ([-a-z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z]))"
		);
	}
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		if (!enabled) {
			return null;
		}
		
		final Matcher matcher = pattern.matcher(message.getBody());		
		if (matcher.matches()){
			
			final String userNick = MessageProcessorUtils.getUserNick(message);
			final String thisMuc = MessageProcessorUtils.getMuc(message);
			
			final Properties mailProps = new Properties();
			
			mailProps.put("mail.smtp.host", host);
			mailProps.put("mail.smtp.user", user);
			mailProps.put("mail.smtp.auth", "true");
			mailProps.put("mail.smtp.starttls.enable", "true");
			
			try {
				final Session session = Session.getInstance(mailProps);
				
				final MimeMessage msg = new MimeMessage(session);
				msg.setText(matcher.group(1));
				msg.setSubject("Срочное сообщение от "+userNick+" из "+thisMuc);
				msg.setFrom(new InternetAddress(user));
				msg.addRecipient(RecipientType.TO, new InternetAddress(matcher.group(2)));
				msg.saveChanges();			
				
				final Transport transport = session.getTransport("smtp");
				transport.connect(host, user, password);
				transport.sendMessage(msg, msg.getAllRecipients());
				transport.close();

				return userNick+": Отправила!";
				
			} catch (MessagingException e) {
				e.printStackTrace();
				return userNick+": Не смогла отправить. т___т";
			}
		} else {
			return null;
		}
	}
}
				
			
			
			
