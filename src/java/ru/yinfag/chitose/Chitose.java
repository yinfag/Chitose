package ru.yinfag.chitose;
import java.util.Scanner;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import java.util.Properties;
import java.io.*;

public class Chitose {
	public static void main(String[] args) {
		//получаем настройки для бота
		Properties props = new Properties();
		try (final InputStreamReader isr = new InputStreamReader(new BufferedInputStream(new FileInputStream("chitose.cfg")), "UTF-8")) {
			props.load(isr);	
		} catch (FileNotFoundException e) {
			log ("Файл нинайдин", e);
		} catch (UnsupportedEncodingException e) {
			log ("ошибка лол", e);
		} catch (IOException e) {
			log ("ошибка ёпт", e);
		} catch (IllegalArgumentException e) {
			log ("ошибка ёпт", e);
		}
		String domain = props.getProperty("domain");
		String login = props.getProperty("login");
		String pass = props.getProperty("password");
		String resourse = props.getProperty("resourse");
		String conf = props.getProperty("conference");
		String nickname = props.getProperty("nickname");
		//получить соединение
		final XMPPConnection conn = new XMPPConnection(domain);
		//подключиться
		try {
			conn.connect();
		} catch (XMPPException e) {
			log("Failed to connect to server", e);
			return;
		}
		try {
			//логинимся
			try {
				conn.login(login, pass, resourse);
			} catch (XMPPException e) {
				log("Failed to login", e);
				return;
			}
			/*эта штука создает объект, 
			* который будет управлять нашим общением 
			* с чат-комнатой "анимуфагс", 
			* с которой мы соединяемся 
			* через соединение conn*/
			final MultiUserChat muc = new MultiUserChat(conn, conf);
			//эта штука добавляет слушатель сообщений.
			ChitoseMUCListener mucListener = new ChitoseMUCListener(muc, props);
			muc.addParticipantListener(mucListener.newProxypacketListener());
			muc.addMessageListener(mucListener.newProxypacketListener());

			//отказываемся от истории, чтобы бот не отвечал на всякое левое говно
			DiscussionHistory history = new DiscussionHistory();
			history.setMaxStanzas(0);
			//заходим в конфочку
			try {
				muc.join(nickname, null, history, 5000);
			} catch (XMPPException e) {
				log("Failed to join the chat room", e);
			}
			waitForExitCommand();
		} finally {
			//дисконектимся
			conn.disconnect();
		}
	}

	//ждём команды выхода тащемто
	private static void waitForExitCommand() {
		final Scanner sc = new Scanner(System.in, "UTF-8");
		sc.useDelimiter("\\n");
		while (sc.hasNext()) {
			final String command = sc.next();
			if ("exit".equals(command)) {
				return;
			}
		}
	}

	//сокращаем всякие ебанутые принтлны и стектрейсы
	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
		//final Exception e;
	}
}
		
