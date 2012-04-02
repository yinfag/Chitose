package ru.yinfag.chitose;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Tokyotosho extends TimerTask implements Runnable {
	
	private String lastTitle;
	private List<MultiUserChat> mucs;
	private List<String> chatrooms;
	private Map<String, Properties> perMucProps;
	
	Tokyotosho(final Map<String, Properties> perMucProps, final List<MultiUserChat> mucs) {
		this.mucs = mucs;
		this.perMucProps = perMucProps;
	}
	
	@Override
	public void run() {
		final URL rss;
		try {
			rss = new URL("http://tokyotosho.info/rss.php?filter=1,7");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		final Document doc;
		try {
			final DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
			doc = factory.newDocumentBuilder().parse(rss.openStream());
		} catch (IOException | SAXException | IllegalArgumentException |
				ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}
		final NodeList nodelist = doc.getElementsByTagName("title");
		final List<String> titles = new ArrayList<>();		
		for (int i = 1; i < nodelist.getLength(); i++) {
			final Element element = (Element) nodelist.item(i);
			final String title = element.getTextContent();
			if (title.equals(lastTitle)) {
				break;
			}
			titles.add(title);
		}
		if (!titles.isEmpty()) {
			if (lastTitle != null) {
				for (final MultiUserChat muc : mucs) {
					final Properties props = perMucProps.get(muc.getRoom());
					if ("1".equals(props.getProperty("Tokyotosho"))) {
						try {
							muc.sendMessage(
								"Ня, новые торренты на тотошке!\n" +
										Utils.join(titles, "\n")
							);
						} catch (XMPPException e) {
							e.printStackTrace();
						}	
					}
				}
			}
			lastTitle = titles.get(0);
		}
	}
}
			
			
		
			

		
		
	
