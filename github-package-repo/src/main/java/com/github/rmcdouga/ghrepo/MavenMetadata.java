package com.github.rmcdouga.ghrepo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public class MavenMetadata {
	private final String METADATA_TAG = "/metadata/";
	private final String VERSIONING_LATEST_XPATH = METADATA_TAG + "versioning/latest/text()";
	private final XmlDocument xml;
	private final String artifactExtension;
	private final String versionXPath;

	private MavenMetadata(XmlDocument xml, String artifactExtension) {
		this.xml = xml;
		this.artifactExtension = artifactExtension;
		this.versionXPath = METADATA_TAG + "versioning/snapshotVersions/snapshotVersion/extension[text()='" + artifactExtension + "']/../value/text()";
	}

	public String getLatestArtifactName(String artifactId) {
		return "%s-%s.%s".formatted(artifactId, 
									latestVersion().or(this::lastSnapshotVersion)
												   .orElseThrow(()->new NoSuchElementException("Unable to locate latest .%s name (%s) in XML (%s)".formatted(artifactExtension, artifactId, xml.toString()))),
									artifactExtension
									);
	}
	
	public String getSnapshotName(String artifactId) {
		return "%s-%s.%s".formatted(artifactId, 
									 latestVersion().or(this::version)
													.orElseThrow(()->new NoSuchElementException("Unable to locate snapshot name (%s) in XML (%s)".formatted(artifactId, xml.toString()))),
									 artifactExtension
									 );
	}
	
	public static MavenMetadata from(byte[] xml, String artifactExtension) {
		return new MavenMetadata(XmlDocument.create(xml), artifactExtension);
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
		List<String> version_results = xml.getStrings(this.versionXPath);
		return version_results.size() > 0 ?  Optional.of(version_results.get(version_results.size()-1)) 
										  : Optional.empty();
	}
}
