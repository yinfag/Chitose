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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

public class Tokyotosho extends TimerTask implements Runnable {
	
	private String lastTitle;
	private List<MultiUserChat> mucs;
	private Map<String, Properties> perMucProps;
	private final Statement statement;
	
	Tokyotosho(
			final Map<String, Properties> perMucProps,
			final List<MultiUserChat> mucs,
			final Connection dbconn
	) throws SQLException  {
		this.mucs = mucs;
		this.perMucProps = perMucProps;
		statement = dbconn.createStatement();
		try (final ResultSet rs = dbconn.getMetaData().getTables(null, null, "FILTER", null)) {
			if (!rs.next()) {
				statement.execute("create table filter (text varchar(512) not null)");
			}
		}
	}
	
	@Override
	public void run() {
		final List<String> filters = new ArrayList<>();
		System.out.println("Trying to query for Tokyotosho filters...");
		try (final ResultSet rs = statement.executeQuery("select text from filter")) {
			while (rs.next()) {
				filters.add(rs.getString("text"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		System.out.printf("Got %d filters%n", filters.size());
		
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
			System.out.printf("Checking RSS title %d: %s%n", i, title);
			if (title.equals(lastTitle)) {
				System.out.println("Equals last title");
				break;
			}
			if (filters.isEmpty()) {
				titles.add(title);
			} else {
				System.out.println("Filtering...");
				for (final String filter : filters) {
					if (title.contains(filter)) {
						System.out.println("Matched filter: " + filter);
						titles.add(title);
						break;
					}
				}
			}
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
			
			
		
			

		
		
	
