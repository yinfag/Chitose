package ru.yinfag.chitose;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;
import java.net.*;
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.Properties;
import java.net.URLEncoder;


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
	private static final Pattern p10 = Pattern.compile("http://goo\\.gl/\\w+");
	private static final Pattern p11 = Pattern.compile(".+?@.+?\\..+?\\..+?/(.+?)");
	private static final Pattern p12 = Pattern.compile(".*?(?:(?:Chitose)|(?:[Чч]итосе)).*?напомни.*?о \"(.+?)\" через ([0-9]+).*?(?:(?:минут)|(?:минуты)|(?:минуту))");
	private static final Pattern p13 = Pattern.compile(".*?([А-Яа-яA-Za-z_ё]+?)\\.(?:(?:жпг)|(?:жпег)|(?:jpg)|(?:пнг)|(?:гиф))");
	
	private static final Set<String> VOICED_ROLES = new HashSet<>();
	
	private final Map<String, Timer> timers = new HashMap<>();

	static {
		VOICED_ROLES.add("participant");
		VOICED_ROLES.add("moderator");
	}


	private final MultiUserChat muc;

	// r - рандом
	private final Random r = new Random();
	
	private final String conference;
	private final String defaultNickname;
	private final String jid;
	private final boolean urlExpandEnabled;
	private final AtomicMarkableReference<String> nick;

	ChitoseMUCListener(final MultiUserChat muc, final Properties props) {
		this.muc = muc;
		conference = props.getProperty("conference");
		defaultNickname = props.getProperty("nickname");
		nick = new AtomicMarkableReference<>(defaultNickname, false);
		jid = props.getProperty("login") + "@" + props.getProperty("domain") + "/" + props.getProperty("resource");
		urlExpandEnabled = "1".equals(props.getProperty("urlExpandEnabled"));
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
		if (
				(conference + "/" + nick.getReference()).equals(message.getFrom()) ||
						jid.equals(message.getFrom())
		) {
			return;
		}

		//отвечаем в чятик на определённое слово
		if ("*smooch*".equals(message.getBody())) {
			try {
				muc.sendMessage("*nosebleed*");
			} catch (XMPPException e) {
				e.printStackTrace();
			}
			return;
		}
		//отвечаем на определённую строку случайным ответом
		if ("Янус - няша?".equals(message.getBody())) {
			int rnum = r.nextInt(2);
			if (rnum == 0) {
				try {
					muc.sendMessage("Няша конечно же. Учит джаве всех итц. ^^");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			} else if (rnum == 1) {
				try {
					muc.sendMessage("Янус - быдло ёбаное, и задрал итц всех своей джавой. ><");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		//постим няшек
		Matcher m1 = p1.matcher(message.getBody());
		if (m1.matches()) {
			
			//имя пославшего запрос
			Matcher m11 = p11.matcher(message.getFrom());
			String nyasha = "";
			if (m11.matches()) {
				nyasha = m11.group(1);
			}
			
			URL gelbooru;
			URLConnection c;
			int rnum2 = r.nextInt(3);
			if (rnum2 == 0 || rnum2 == 1) {
				String nyakaName;
				if ("няшку".equals(m1.group(2))) {
					nyakaName = "";
				} else {
					nyakaName = m1.group(1);
				}
				try {
					gelbooru = new URL("http://gelbooru.com/index.php?page=dapi&s=post&q=index&limit=10000&tags=solo+"+nyakaName);
				} catch (MalformedURLException e) {
					log("Не получилось составить урл для запроса в гелбуру", e);
					return;
				}
				try {
					c = gelbooru.openConnection();
				} catch (IOException e) {
					log("Не получилось открыть соединение с гелбуру", e);
					return;
				}
				c.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.77 Safari/535.7");
				try (BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
					String inputLine;
					final List<String> urls = new ArrayList<String>();
					while ((inputLine = in.readLine()) != null) {
						Matcher m2 = p2.matcher(inputLine);
						if (!m2.find()) {
							continue;
						}
						final String postUrl = String.format(m2.group(1));
						urls.add(postUrl);
					}
					if (urls.size() == 0) {
						try {
							muc.sendMessage(nyasha + ": " + m1.group(1) +" не няшка!");
						} catch (XMPPException e1) {
								e1.printStackTrace();
						}
						return;
					}
					try {
						Random random = new Random();
						muc.sendMessage(nyasha + ": " + urls.get(random.nextInt(urls.size())));
					} catch (XMPPException e) {
						e.printStackTrace();
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
			if (rnum2 == 2) {
				try {
					muc.sendMessage(nyasha + ": " + "gelbooru.com - Все няшки там. Удачи, лентяй.");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
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

		//раскрываем ссылки
		if (urlExpandEnabled) {
			final Matcher m10 = p10.matcher(message.getBody());
			StringBuilder urlExpanderSB = null;
			while (m10.find()) {
				if (urlExpanderSB == null) {
					urlExpanderSB = new StringBuilder("Короткие урлы ведут сюда:");
				}
				final String shortUrlString = m10.group(0);
				urlExpanderSB.append("\n").append(shortUrlString).append(" -> ");
				final URL shortUrl;
				try {
					shortUrl = new URL(shortUrlString);
				} catch (MalformedURLException e) {
					urlExpanderSB.append("(плохой урл почему-то)");
					continue;
				}
				final HttpURLConnection con;
				try {
					con = ((HttpURLConnection) shortUrl.openConnection());
					con.setInstanceFollowRedirects(false);
					con.connect();
				} catch (IOException e) {
					log("Не получилось открыть соединение", e);
					urlExpanderSB.append("(не удалось открыть соединение)");
					continue;
				}
				urlExpanderSB.append(con.getHeaderField("Location"));
			}
			if (urlExpanderSB != null) {
				try {
					muc.sendMessage(urlExpanderSB.toString());
				} catch (XMPPException e) {
					e.printStackTrace();
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
