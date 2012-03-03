package ru.yinfag.chitose;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChitoseMUCListener implements PacketListener {
	// регэкспы регэкспушки
	private static final Pattern p = Pattern.compile("(?:(?:Chitose)|(?:[Чч]итосе)).*?кинь.*?(\\d+)[dд](\\d+)");
	private static final Pattern p1 = Pattern.compile(".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?(?:(?:запости)|(?:доставь)).*?(?:([\\w().*\\+]+?)|(няшку))\\.?$");
	private static final Pattern p2 = Pattern.compile("sample_url=\"(.+?)\"");
	private static final Pattern p3 = Pattern.compile("(?:(?:Chitose)|(?:[Чч]итосе)).*?расскажи.*?про \"(.+?)\"");
	private static final String p4 = "отсортировано по дате выхода";
	private static final Pattern p5 = Pattern.compile("\\'estimation\\'\\>(.+?)\\&nbsp");
	private static final Pattern p6 = Pattern.compile("Краткое содержание\\:.*?class\\=\\'review\\'\\>(.+?)\\<\\/p\\>");
	private static final String p7 = "<b>Раздел &laquo;анимация&raquo;";
	private static final Pattern p8 = Pattern.compile("animation\\/animation.php\\?id\\=(\\d+)");
	private static final String p9 = "<meta http-equiv='Refresh' content='0;";
	private static final Pattern p11 = Pattern.compile(".+?@.+?\\..+?\\..+?/(.+?)");
	private static final Pattern p12 = Pattern.compile(".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?напомни.*?о \"(.+?)\" через ([0-9]+).*?(?:(?:минут)|(?:минуты)|(?:минуту))");
	private static final Pattern p13 = Pattern.compile(".*?([А-Яа-яA-Za-z_ё]+?)\\.(?:(?:жпг)|(?:жпег)|(?:jpg)|(?:пнг)|(?:гиф))");

	private static final Set<String> VOICED_ROLES = new HashSet<>();

	static {
		VOICED_ROLES.add("participant");
		VOICED_ROLES.add("moderator");
	}

	private final List<MessageProcessor> messageProcessors = new ArrayList<>();

	private final Map<String, Timer> timers = new HashMap<>();

	private final MultiUserChat muc;

	// r - рандом
	private final Random r = new Random();
	
	private final String conference;
	private final String defaultNickname;
	private final String jid;
	private final AtomicMarkableReference<String> nick;

	ChitoseMUCListener(final MultiUserChat muc, final Properties props) {
		this.muc = muc;
		conference = props.getProperty("conference");
		defaultNickname = props.getProperty("nickname");
		nick = new AtomicMarkableReference<>(defaultNickname, false);
		jid = props.getProperty("login") + "@" + props.getProperty("domain") + "/" + props.getProperty("resource");

		populateMessageProcessors(props);
	}

	private void populateMessageProcessors(final Properties props) {
		messageProcessors.add(new SmoochMessageProcessor());
		messageProcessors.add(new URLExpander(props));
		messageProcessors.add(new GelbooruMessageProcessor());
	}
	

	public PacketListener newProxypacketListener() {
		return new PacketListener() {
			@Override
			public void processPacket(final Packet packet) {
				ChitoseMUCListener.this.processPacket(packet);
			}
		};
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof Presence) {
			processPresence((Presence) packet);
		} else if (packet instanceof Message) {
			processMessage((Message) packet);
		}
	}

	private void processMessage(final Message message) {
		System.out.println("message from " + message.getFrom());
		// if this is a message from ourselves, don't react to it
		if (
				(conference + "/" + nick.getReference()).equals(message.getFrom()) ||
						jid.equals(message.getFrom())
		) {
			return;
		}

		for (final MessageProcessor processor : messageProcessors) {
			try {
				final CharSequence result = processor.process(message);
				if (result == null) {
					continue;
				}
				try {
					muc.sendMessage(result.toString());
				} catch (XMPPException e) {
					log("Failed to send a message", e);
				}
				return;
			} catch (MessageProcessingException e) {
				log("Error while processing a message with " + processor, e);
			}
		}

		//тырим описание с вротарта
		Matcher m3 = p3.matcher(message.getBody());
		if (m3.matches()) {
			URL worldart;
			String title = "";
			int rnum = r.nextInt(3);
			if (rnum == 0 || rnum == 1 || rnum == 2) {
				try {
					title = URLEncoder.encode(m3.group(1), "CP1251");
					System.out.println(title);
				} catch (UnsupportedEncodingException e){
					log("урленкодер не сработал", e);
				}
				try {
					worldart = new URL("http://www.world-art.ru/search.php?public_search=" +title+"&global_sector=animation");
				} catch (MalformedURLException e) {
					log("Не получилось составить урл для запроса на вротарт", e);
					return;
				}
				boolean multipleTitleVariants = false;
				boolean animationPresent = false;
				try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						if (inputLine.contains(p4)) {
							multipleTitleVariants = true;
						}
						if (inputLine.contains(p7)) {
							animationPresent = true;
						}
						if (multipleTitleVariants && animationPresent) {
							break;
						}
					}
				} catch (IOException e) {
					log("Ошибка ввода-вывода при чтении страницы", e);
					try {
						muc.sendMessage("Няшки закрыты на ремонт.");
					} catch (XMPPException e1) {
						e1.printStackTrace();
					}
				}
				System.out.format("MTV: %b; AP: %b%n", multipleTitleVariants, animationPresent);
				if (multipleTitleVariants) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
						final List<String> titles = new ArrayList<String>();
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							Matcher m5 = p5.matcher(inputLine);
							while (m5.find()) {
								final String postTitles = m5.group(1);
								titles.add(postTitles);
							}
						}
						String titleList = Utils.join(titles, ", ");
						if (titles.size() > 10) {
							try {
								muc.sendMessage("Найдено "+titles.size()+" результатов, попробуй уточнить свой запрос.");
							} catch (XMPPException e) {
								e.printStackTrace();
							}
						} else {
							try {
								muc.sendMessage("Возможно ты имел ввиду: "+titleList);
							} catch (XMPPException e) {
								e.printStackTrace();
							}
						}
					} catch (IOException e) {
						log("Ошибка ввода-вывода при чтении страницы", e);
						try {
							muc.sendMessage("Няшки закрыты на ремонт.");
						} catch (XMPPException e1) {
							e1.printStackTrace();
						}
					}
				} else if (animationPresent) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
						String inputLine;
						String titleID = null;
						URL worldart1;
						while ((inputLine = in.readLine()) != null) {
							Matcher m8 = p8.matcher(inputLine);
							if (m8.find()) {
								titleID = m8.group(1);
							}
						}
						try {
							worldart1 = new URL("http://www.world-art.ru/animation/animation.php?id="+titleID);
						} catch (MalformedURLException e) {
							log("Не получилось составить урл для запроса на вротарт", e);
							return;
						}
						String synopsis1 = "Похоже описание отсутствует. Щто поделать, десу.";
						try (BufferedReader in1 = new BufferedReader(new InputStreamReader(worldart1.openStream(), "cp1251"))) {
							String inputLine1;
							while ((inputLine1 = in1.readLine()) != null) {
								Matcher m6 = p6.matcher(inputLine1);
								if (m6.find()) {
									synopsis1 = m6.group(1);
								}
							}
						}
						try {
							muc.sendMessage(synopsis1.replaceAll("\\<br\\>", "\n") + "\n" +worldart1);
						} catch (XMPPException e1) {
							e1.printStackTrace();
						}
					} catch (IOException e) {
						log("Ошибка ввода-вывода при чтении страницы", e);
						try {
							muc.sendMessage("Няшки закрыты на ремонт.");
						} catch (XMPPException e1) {
							e1.printStackTrace();
						}
					}
				} else {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(worldart.openStream(), "cp1251"))) {
						String inputLine;
						URL worldart1;
						String titleID = null;
						while ((inputLine = in.readLine()) != null) {
							if (inputLine.contains(p9)) {
								Matcher m8 = p8.matcher(inputLine);
								if (m8.find()) {
									titleID = m8.group(1);
								}
							}
						}
						try {
							worldart1 = new URL("http://www.world-art.ru/animation/animation.php?id="+titleID);
						} catch (MalformedURLException e) {
							log("Не получилось составить урл для запроса на вротарт", e);
							return;
						}
						String synopsis = "Нет такого мультфильма!";
						try (BufferedReader in1 = new BufferedReader(new InputStreamReader(worldart1.openStream(), "cp1251"))) {
							String inputLine1;
							while ((inputLine1 = in1.readLine()) != null) {
								Matcher m6 = p6.matcher(inputLine1);
								if (m6.find()) {
									synopsis = m6.group(1);
								}
							}
						}
						if (titleID == null) {
							try {
								muc.sendMessage(synopsis);
							} catch (XMPPException e1) {
								e1.printStackTrace();
							}
						} else {
							try {
								muc.sendMessage(synopsis.replaceAll("\\<br\\>", "\n") + "\n" +worldart);
							} catch (XMPPException e1) {
								e1.printStackTrace();
							}
						}
					} catch (IOException e) {
						log("Ошибка ввода-вывода при чтении страницы", e);
						try {
							muc.sendMessage("Няшки закрыты на ремонт.");
						} catch (XMPPException e1) {
							e1.printStackTrace();
						}
					} 
				}
			}
		}

		//Таймер
		Matcher m12 = p12.matcher(message.getBody());
		if (m12.matches()) {
			
			//имя пославшего запрос
			Matcher m11 = p11.matcher(message.getFrom());
			String nyasha = "";
			if (m11.matches()) {
				nyasha = m11.group(1);
			}
			final String nyashaFinal = nyasha;
			
			
			String timeMinute = m12.group(2);
			final String sage = m12.group(1);	
			long timeMinuteLong = 0;
			try {
				timeMinuteLong =  Long.parseLong(timeMinute.trim());
			} catch (NumberFormatException e) {
				log ("Ошибка перевода стринг в лонг", e);
			}
			final String pseudoJid = message.getFrom();
			boolean oldTimer = timers.containsKey(pseudoJid);
			if (oldTimer) {
				timers.remove(pseudoJid).cancel();
				
			}
			Timer timer = new Timer();
			timers.put(pseudoJid, timer);
			TimerTask task = new TimerTask() {
				public void run()
				{
					timers.remove(pseudoJid);
					try {
						muc.sendMessage(nyashaFinal + ": Напоминаю! " + "\n" + sage);
					} catch (XMPPException e) {
						log("Напоминание сфейлилось", e);
					}
				}
			};
			if (timeMinuteLong != 0 && oldTimer ) {
				long time = timeMinuteLong * 60000;
				timer.schedule(task, time);
				try {
					muc.sendMessage(nyashaFinal + ": таймер изменён!");
				} catch (XMPPException e) {
					log("Failed to say таймер изменён :3", e);
				}
			} else if (timeMinuteLong != 0) {
				long time = timeMinuteLong * 60000;
				timer.schedule(task, time);
				try {
					muc.sendMessage(nyashaFinal + ": окей!");
				} catch (XMPPException e) {
					log("Failed to say окей :3", e);
				}
			}
		}
		
		//jpg.to
		Matcher m13 = p13.matcher(message.getBody());
		if (m13.matches()) {
			String pic = m13.group(1);
			try {
				muc.sendMessage("http://" + pic + ".jpg.to/");
			} catch (XMPPException e) {
				log("Не получилось запостить пикчу + жпг.то", e);
			}
		}
			
			

		//бросаем костяшки
		Matcher m = p.matcher(message.getBody());
		if (m.matches()) {
			int rnum1 = r.nextInt(2);
			if (rnum1 == 0) {
				int a = Integer.parseInt(m.group(1));
				int b = Integer.parseInt(m.group(2));
				if (a > 9999 || b > 99999) {
					try {
						muc.sendMessage("Я не настолько умная же! ><'");
					} catch (XMPPException e) {
						e.printStackTrace();
					}
					return;
				}
				int result = 0;
				for (int i = 0; i < a; i++) {
					result += r.nextInt(b) + 1;
				}
				try {
					muc.sendMessage("Выпало " + result + ", такие дела, нян!");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
			if (rnum1 == 1) {
				try {
					muc.sendMessage("Неохота. ~___~");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void processPresence(final Presence presence) {
		System.out.println(presence.getFrom());
		if ((conference + "/" + nick.getReference()).equals(presence.getFrom())) {
			for (PacketExtension extension : presence.getExtensions()) {
				System.out.println("e: " + extension);
				if (extension instanceof MUCUser) {
					final MUCUser.Item item = ((MUCUser) extension).getItem();
					final String role = item.getRole();
					if (!VOICED_ROLES.contains(role) && !nick.isMarked()) {
						try {
							final String newNick = "Дайте_войс";
							muc.changeNickname(newNick);
							nick.set(newNick, true);
						} catch (XMPPException e) {
							log("Failed to change nick", e);
						}
					} else if (VOICED_ROLES.contains(role) && nick.isMarked()) {
						try {
							final String newNick = defaultNickname;
							muc.changeNickname(newNick);
							nick.set(newNick, false);
						} catch (XMPPException e) {
							log("Failed to change nick", e);
						}
						try {
							muc.sendMessage("Аригато!");
						} catch (XMPPException e) {
							log("Failed to say thanks", e);
						}
					}
				}
			}
		}
	}

	//сокращаем всякие ебанутые принтлны и стектрейсы
	private static void log(final String message, final Exception e) {
		System.out.println(message);
		if (e != null) {
			e.printStackTrace();
		}
	}
}
