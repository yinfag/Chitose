package ru.yinfag.chitose;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.net.URL;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.TimerTask;
import javax.xml.parsers.FactoryConfigurationError;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smack.XMPPException;

public class Tokyotosho extends TimerTask implements Runnable {
	
	private String lastTitle;
	private List<MultiUserChat> mucs;
	
	Tokyotosho(final List<MultiUserChat> mucs) {
		this.mucs = mucs;
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
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			doc = factory.newDocumentBuilder().parse(rss.openStream());
		} catch (IOException | SAXException | IllegalArgumentException | ParserConfigurationException e) {
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
					try {
						muc.sendMessage("Ня, новые торренты на тотошке!\n" + Utils.join(titles, "\n"));
					} catch (XMPPException e) {
						e.printStackTrace();
					}
				}
			}
			lastTitle = titles.get(0);
		}
	}
}
			
			
		
			

		
		
	
