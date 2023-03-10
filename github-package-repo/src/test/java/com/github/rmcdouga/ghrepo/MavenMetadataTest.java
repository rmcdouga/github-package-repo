package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MavenMetadataTest {
	private static final String ARTIFACT_ID = "watched-f older-poster";
	
	enum TestScenario {
		scenario1("maven-metadata.xml", ARTIFACT_ID + "-0.0.1-20221221.221800-4.jar", ARTIFACT_ID + "-0.0.1-SNAPSHOT.jar"),
		scenario2("maven-metadata_2.xml", ARTIFACT_ID + "-0.0.1-SNAPSHOT.jar", ARTIFACT_ID + "-0.0.1-SNAPSHOT.jar")
		;
		
		final MavenMetadata underTest;
		final String expectedGetLatestJarNameResult;
		final String expectedGetSnapshotNameResult;
		
		private TestScenario(String testFile, String expectedGetLatestJarNameResult, String expectedGetSnapshotNameResult) {
			try {
				this.underTest = MavenMetadata.from(Files.readAllBytes(TestUtils.SAMPLE_FILES_DIR.resolve(testFile)));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			this.expectedGetLatestJarNameResult = expectedGetLatestJarNameResult;
			this.expectedGetSnapshotNameResult = expectedGetSnapshotNameResult;
		}
		
	}
	
	@ParameterizedTest
	@EnumSource
	void testGetLatestJarName(TestScenario scenario) {
		assertEquals(scenario.expectedGetLatestJarNameResult, scenario.underTest.getLatestJarName(ARTIFACT_ID));
	}

	@ParameterizedTest
	@EnumSource
	void testGetSnapshotName(TestScenario scenario) {
		assertEquals(scenario.expectedGetSnapshotNameResult, scenario.underTest.getSnapshotName(ARTIFACT_ID));
	}

}
