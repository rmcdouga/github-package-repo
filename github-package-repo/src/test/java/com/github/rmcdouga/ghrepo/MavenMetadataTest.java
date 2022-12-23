package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class MavenMetadataTest {
	
	private final MavenMetadata underTest;
	{
		try {
			underTest = MavenMetadata.from(Files.readAllBytes(TestUtils.SAMPLE_FILES_DIR.resolve("maven-metadata.xml")));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void testGetLatestJarName() {
		assertEquals("watched-folder-poster-0.0.1-20221221.221800-4.jar", underTest.getLatestJarName());
	}

	@Test
	void testGetSnapshotName() {
		assertEquals("watched-folder-poster-0.0.1-SNAPSHOT.jar", underTest.getSnapshotName());
	}

}
