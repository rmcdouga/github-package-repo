package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GithubPackagesTest {

	private static final String METADATA_NAME = "maven-metadata.xml";
	private static final String USER_ORG_ID = "4PointSolutions";
	private static final String REPO = "WatchedFolderUtils";
	private static final String GROUP_ID = "com._4point.aem.watchedfolder";
	private static final String ARTIFACT_ID = "watched-folder-poster";
	private static final String VERSION = "0.0.1-SNAPSHOT";
	
	private static final String EXPECTED_METADATA_LOCATION = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1-SNAPSHOT/" + METADATA_NAME;
	private static final String EXPECTED_JAR_LOCATION = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1-SNAPSHOT/watched-folder-poster-0.0.1-20221221.221800-4.jar";

	@Captor ArgumentCaptor<String> restEndpoint;
	private final RestClient mockRestClient; 
	
	private final GithubPackages underTest;

	public GithubPackagesTest(@Mock RestClient mockRestClient) throws FileNotFoundException {
		this.mockRestClient = mockRestClient;
		this.underTest = new GithubPackages(mockRestClient);
	}

	@DisplayName("Test GithinPackages.get() method.")
	@Test
	void testGet(@Mock InputStream expectedResult) throws Exception {
		Mockito.when(mockRestClient.get(Mockito.endsWith(METADATA_NAME))).thenReturn(Files.newInputStream(TestUtils.SAMPLE_FILES_DIR.resolve(METADATA_NAME)));
		Mockito.when(mockRestClient.get(Mockito.endsWith(".jar"))).thenReturn(expectedResult);
		InputStream result = underTest.get(USER_ORG_ID, REPO, GROUP_ID, ARTIFACT_ID, VERSION);
		assertSame(expectedResult, result);
		Mockito.verify(mockRestClient).get(EXPECTED_METADATA_LOCATION);
		Mockito.verify(mockRestClient).get(EXPECTED_JAR_LOCATION);
	}

	// This test utilizes credentials from the user's .m2/settings.xml file which must
	// have a "github" id with a suitable github personal access token.  That token must
	// have access to the 4PointSolutions WatchedFolderUtils packages.
	@Tag("Integration")
	@DisplayName("Integration test - test GithinPackages.get() method.")
	@Test
	void testGet() throws Exception {
		String failureResultsFilename = "GithubPackagesTest_testGet_IntTest_results.txt";
		InputStream result = GithubPackages.create().get(USER_ORG_ID, REPO, GROUP_ID, ARTIFACT_ID, VERSION);
		byte[] resultBytes = result.readAllBytes();
		boolean isArchive = isArchive(resultBytes);
		if (!isArchive) {
			Files.write(TestUtils.ACTUAL_RESULTS_DIR.resolve(failureResultsFilename), resultBytes);
		}
		assertTrue(isArchive, "Expected response to be a .zip/.jar but is was not. Results written to actualResults directory.");
	}

	@Disabled("Not implemented yet.")
	@Test
	void testRepository() {
		fail("Not yet implemented");
	}

	private static boolean isArchive(byte[] bytes) {
	    int fileSignature = ByteBuffer.wrap(bytes).getInt();
	    return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
	}
}
