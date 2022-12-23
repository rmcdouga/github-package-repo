package com.github.rmcdouga.ghrepo;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

public class MavenSettings {
	static record Credentials(String username, String password) {}
	
	private final XML xml;
	
	private MavenSettings(XML xml) {
		this.xml = xml;
	}

	public Credentials credentials(String serverId) {
		String xpath = "/mvn:settings/mvn:servers/mvn:server/mvn:id[text()='%s']".formatted(serverId);
		List<String> results = xml.xpath(xpath + "/text()");
		var size = results.size(); 
		if (size != 1 || !serverId.equals(results.get(0))) {
			throw new IllegalStateException("Expected 1 server with id ='" + serverId + "', but found " + size + " instead.");
		}
		return new Credentials(xml.xpath(xpath + "/../mvn:username/text()").get(0), xml.xpath(xpath + "/../mvn:password/text()").get(0));
	}
	
	public static MavenSettings from(Path settingsFileLocation) throws FileNotFoundException {
		return new MavenSettings(new XMLDocument(settingsFileLocation).registerNs("mvn", "http://maven.apache.org/SETTINGS/1.0.0"));
	}
	
	public static MavenSettings get() throws FileNotFoundException {
		Path homeDir = Path.of(System.getProperty("user.home"));
		return from(homeDir.resolve(Path.of(".m2", "settings.xml")));
	}
}
