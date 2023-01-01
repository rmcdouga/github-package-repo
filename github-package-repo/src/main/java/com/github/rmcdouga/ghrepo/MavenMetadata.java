package com.github.rmcdouga.ghrepo;

import java.util.List;
import java.util.Optional;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

public class MavenMetadata {
	private final String METADATA_TAG = "/metadata/";
	private final String VERSIONING_LATEST_XPATH = METADATA_TAG + "versioning/latest/text()";
	private final String VERSION_XPATH = METADATA_TAG + "versioning/snapshotVersions/snapshotVersion/extension[text()='jar']/../value/text()";
	private final XML xml;

	private MavenMetadata(XML xml) {
		this.xml = xml;
	}

	public String getLatestJarName() {
		return "%s-%s.jar".formatted(artifactId(), latestVersion().or(this::lastSnapshotVersion).orElseThrow());// TODO: Validate use of .or()
	}
	
	public String getSnapshotName() {
		return "%s-%s.jar".formatted(artifactId(), latestVersion().or(this::version).orElseThrow());
	}
	
	public static MavenMetadata from(byte[] xml) {
		return new MavenMetadata(new XMLDocument(xml));
	}
	
	private String artifactId() {
		return xml.xpath(METADATA_TAG + "artifactId/text()").get(0);
	}
	
	private Optional<String> version() {
		List<String> version_results = xml.xpath(METADATA_TAG + "version/text()");
		return version_results.size() > 0 ? Optional.of(version_results.get(0))
										  : Optional.empty();
	}
	
	private Optional<String> latestVersion() {
		List<String> latest_results = xml.xpath(VERSIONING_LATEST_XPATH);
		return latest_results.size() > 0 ? Optional.of(latest_results.get(0)) 
										 : Optional.empty();
	}

	private Optional<String> lastSnapshotVersion() {
		List<String> version_results = xml.xpath(VERSION_XPATH);
		return version_results.size() > 0 ?  Optional.of(version_results.get(version_results.size()-1)) 
										  : Optional.empty();
	}
}
