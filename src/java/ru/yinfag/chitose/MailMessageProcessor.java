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
	private final Pattern p1;
	private final Properties props;
	
	MailMessageProcessor (final Properties mucProps, final Properties props) {
		this.props = props;
		enabled = "1".equals(mucProps.getProperty("Mail"));
		String botname = mucProps.getProperty("nickname");
		p1 = Pattern.compile(
			".*?" + botname + ".* отправь сообщение \"(.+?)\" на ([-a-z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z]))"
		);
	}
	
	@Override
	public CharSequence process(final Message message) throws MessageProcessingException {
		
		if (!enabled) {
			return null;
		}
		
		final Matcher m1 = p1.matcher(message.getBody());		
		if (m1.matches()){
			
			final String userNick = MessageProcessorUtils.getUserNick(message);
			final String thisMuc = MessageProcessorUtils.getMuc(message);
			
			Properties mailProps = new Properties();
			
			mailProps.put("mail.smtp.host", props.getProperty("mail.host"));
			mailProps.put("mail.smtp.user", props.getProperty("mail.user"));
			mailProps.put("mail.smtp.auth", "true");
			mailProps.put("mail.smtp.starttls.enable", "true");
			
			try {
				Session session = Session.getInstance(mailProps);
				
				MimeMessage msg = new MimeMessage(session);
				msg.setText(m1.group(1));
				msg.setSubject("Срочное сообщение от "+userNick+" из "+thisMuc);
				msg.setFrom(new InternetAddress(props.getProperty("mail.user")));
				msg.addRecipient(RecipientType.TO, new InternetAddress(m1.group(2)));
				msg.saveChanges();			
				
				Transport transport = session.getTransport("smtp");
				transport.connect(props.getProperty("mail.host"), props.getProperty("mail.user"), props.getProperty("mail.password"));
				transport.sendMessage(msg, msg.getAllRecipients());
				transport.close();

				return userNick+" :Отправила!";
				
			} catch (MessagingException e) {
				e.printStackTrace();
				return userNick+" :Не смогла отправить. т___т";
			}
		} else {
			return null;
		}
	}
}
				
			
			
			
