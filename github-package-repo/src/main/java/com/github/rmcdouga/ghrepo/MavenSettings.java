package com.github.rmcdouga.ghrepo;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

public class MavenSettings {
	public static record Credentials(String username, String password) {}
	
	private final XmlDocument xml;
	
	private MavenSettings(XmlDocument xml) {
		this.xml = xml;
	}

	public Credentials credentials(String serverId) {
		String xpath = "/mvn:settings/mvn:servers/mvn:server/mvn:id[text()='%s']".formatted(serverId);
		List<String> results = xml.getStrings(xpath + "/text()");
		var size = results.size(); 
		if (size != 1 || !serverId.equals(results.get(0))) {
			throw new IllegalStateException("Expected 1 server with id ='" + serverId + "', but found " + size + " instead.");
		}
		return new Credentials(xml.getString(xpath + "/../mvn:username/text()"), xml.getString(xpath + "/../mvn:password/text()"));
	}
	
	public static MavenSettings from(Path settingsFileLocation) throws FileNotFoundException {
		return new MavenSettings(XmlDocument.builder(settingsFileLocation).defaultNsPrefix("mvn").build());
	}
	
	public static MavenSettings get() throws FileNotFoundException {
		Path homeDir = Path.of(System.getProperty("user.home"));
		return from(homeDir.resolve(Path.of(".m2", "settings.xml")));
	}
}
