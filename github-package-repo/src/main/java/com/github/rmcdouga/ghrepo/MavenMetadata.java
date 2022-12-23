package com.github.rmcdouga.ghrepo;

import java.util.List;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

public class MavenMetadata {
	private final String METADATA_TAG = "/metadata";
	private final String VERSION_XPATH = "/versioning/snapshotVersions/snapshotVersion/extension[text()='jar']/../value/text()";
	private final XML xml;

	private MavenMetadata(XML xml) {
		this.xml = xml;
	}

	public String getLatestJarName() {
		return "%s-%s.jar".formatted(artifactId(), latestVersion());
	}
	
	public String getSnapshotName() {
		return "%s-%s.jar".formatted(artifactId(), version());
	}
	
	public static MavenMetadata from(byte[] xml) {
		return new MavenMetadata(new XMLDocument(xml));
	}
	
	private String artifactId() {
		return xml.xpath(METADATA_TAG + "/artifactId/text()").get(0);
	}
	
	private String version() {
		return xml.xpath(METADATA_TAG + "/version/text()").get(0);
	}
	
	private String latestVersion() {
		List<String> results = xml.xpath(METADATA_TAG + VERSION_XPATH);
		return results.get(results.size()-1);
	}
}
