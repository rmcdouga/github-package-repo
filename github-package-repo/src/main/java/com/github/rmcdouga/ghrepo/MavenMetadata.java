package com.github.rmcdouga.ghrepo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public class MavenMetadata {
	private final String METADATA_TAG = "/metadata/";
	private final String VERSIONING_LATEST_XPATH = METADATA_TAG + "versioning/latest/text()";
	private final String VERSION_XPATH = METADATA_TAG + "versioning/snapshotVersions/snapshotVersion/extension[text()='jar']/../value/text()";
	private final XmlDocument xml;

	private MavenMetadata(XmlDocument xml) {
		this.xml = xml;
	}

	public String getLatestJarName(String artifactId) {
		return "%s-%s.jar".formatted(artifactId, latestVersion().or(this::lastSnapshotVersion)
																.orElseThrow(()->new NoSuchElementException("Unable to locate latest .jar name (%s) in XML (%s)".formatted(artifactId, xml.toString()))));
	}
	
	public String getSnapshotName(String artifactId) {
		return "%s-%s.jar".formatted(artifactId, latestVersion().or(this::version)
																.orElseThrow(()->new NoSuchElementException("Unable to locate snapshot name (%s) in XML (%s)".formatted(artifactId, xml.toString()))));
	}
	
	public static MavenMetadata from(byte[] xml) {
			return new MavenMetadata(XmlDocument.create(xml));
	}
	
//	private String artifactId() {
//		return xml.xpath(METADATA_TAG + "artifactId/text()").get(0);
//	}
//	
	private Optional<String> version() {
		List<String> version_results = xml.getStrings(METADATA_TAG + "version/text()");
		return version_results.size() > 0 ? Optional.of(version_results.get(0))
										  : Optional.empty();
	}
	
	private Optional<String> latestVersion() {
		List<String> latest_results = xml.getStrings(VERSIONING_LATEST_XPATH);
		return latest_results.size() > 0 ? Optional.of(latest_results.get(0)) 
										 : Optional.empty();
	}

	private Optional<String> lastSnapshotVersion() {
		List<String> version_results = xml.getStrings(VERSION_XPATH);
		return version_results.size() > 0 ?  Optional.of(version_results.get(version_results.size()-1)) 
										  : Optional.empty();
	}
}
