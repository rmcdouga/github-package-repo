package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
	private static final String SNAPSHOT_VERSION = "0.0.1-SNAPSHOT";
	private static final String FINAL_VERSION = "0.0.1";
	private static final String JAR_EXTENSION = "jar";
	
	private static final String EXPECTED_METADATA_LOCATION = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1-SNAPSHOT/" + METADATA_NAME;
	private static final String EXPECTED_SNAPSHOT_JAR_LOCATION = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1-SNAPSHOT/watched-folder-poster-0.0.1-20221221.221800-4.jar";
	private static final String EXPECTED_FINAL_JAR_LOCATION = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1/watched-folder-poster-0.0.1.jar";

	@Mock InputStream expectedResult;
	@Captor ArgumentCaptor<String> restEndpoint;
	private final RestClient mockRestClient; 
	
	private final GithubPackages underTest;

	public GithubPackagesTest(@Mock RestClient mockRestClient) throws FileNotFoundException {
		this.mockRestClient = mockRestClient;
		this.underTest = new GithubPackages(mockRestClient);
	}

    @FunctionalInterface
    public interface Function_WithExceptions<T, R, E extends Exception> {
        R apply(T t) throws E;
    }
    
	@DisplayName("Test GithinPackages.get() method in all it's forms with SNAPSHOT version.")
	@ParameterizedTest
	@MethodSource("testSnapshotFns")
	void testSnapshotGet(Function_WithExceptions<GithubPackages, InputStream, IOException> underTestFn) throws Exception {
		Mockito.when(mockRestClient.get(Mockito.endsWith(METADATA_NAME))).thenReturn(Files.newInputStream(TestUtils.SAMPLE_FILES_DIR.resolve(METADATA_NAME)));
		Mockito.when(mockRestClient.get(Mockito.endsWith(".jar"))).thenReturn(expectedResult);
		InputStream result = underTestFn.apply(underTest);
		assertSame(expectedResult, result);
		Mockito.verify(mockRestClient).get(EXPECTED_METADATA_LOCATION);
		Mockito.verify(mockRestClient).get(EXPECTED_SNAPSHOT_JAR_LOCATION);
	}

	// Iterate through all the fluent functions.
	static List<Function_WithExceptions<GithubPackages, InputStream, IOException>> testSnapshotFns() {
		return List.of(
				(ghp)->ghp.get(USER_ORG_ID, REPO, GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).get(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).get(ARTIFACT_ID, SNAPSHOT_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).get(SNAPSHOT_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).version(SNAPSHOT_VERSION).get(),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).version(SNAPSHOT_VERSION).extension(JAR_EXTENSION).get()
				); 
	}
	
	@DisplayName("Test GithinPackages.get() method in all it's forms with final version.")
	@ParameterizedTest
	@MethodSource("testFinalFns")
	void testGet(Function_WithExceptions<GithubPackages, InputStream, IOException> underTestFn) throws Exception {
		Mockito.when(mockRestClient.get(Mockito.endsWith(".jar"))).thenReturn(expectedResult);
		InputStream result = underTestFn.apply(underTest);
		assertSame(expectedResult, result);
		Mockito.verify(mockRestClient).get(EXPECTED_FINAL_JAR_LOCATION);
	}

	// Iterate through all the fluent functions.
	static List<Function_WithExceptions<GithubPackages, InputStream, IOException>> testFinalFns() {
		return List.of(
				(ghp)->ghp.get(USER_ORG_ID, REPO, GROUP_ID, ARTIFACT_ID, FINAL_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).get(GROUP_ID, ARTIFACT_ID, FINAL_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).get(ARTIFACT_ID, FINAL_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).get(FINAL_VERSION),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).version(FINAL_VERSION).get(),
				(ghp)->ghp.repo(USER_ORG_ID, REPO).group(GROUP_ID).artifact(ARTIFACT_ID).version(FINAL_VERSION).extension(JAR_EXTENSION).get()
				); 
	}
	
	// This test utilizes credentials from the user's .m2/settings.xml file which must
	// have a "github" id with a suitable github personal access token.  That token must
	// have access to the 4PointSolutions WatchedFolderUtils packages.
	@Tag("Integration")
	@DisplayName("Integration test - test GithinPackages.get() method.")
	@Test
	void testGet_Integration() throws Exception {
		String failureResultsFilename = "GithubPackagesTest_testGet_IntTest_results.txt";
		InputStream result = GithubPackages.create().get(USER_ORG_ID, REPO, GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION);
		byte[] resultBytes = result.readAllBytes();
		boolean isArchive = isArchive(resultBytes);
		if (!isArchive) {
			Files.write(TestUtils.ACTUAL_RESULTS_DIR.resolve(failureResultsFilename), resultBytes);
		}
		assertTrue(isArchive, "Expected response to be a .zip/.jar but is was not. Results written to actualResults directory.");
	}

	@Tag("Integration")
	@DisplayName("Integration test - test GithinPackages.fluent copyTo file method.")
	@Test
	void testCopyToFile(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
		Path resultsFilename = tempDir.resolve("GithubPackagesTest_testCopyTo_IntTest_results.txt");
		GithubPackages.create()
					  .repo(USER_ORG_ID, REPO)
					  .group(GROUP_ID)
					  .artifact(ARTIFACT_ID)
					  .version(SNAPSHOT_VERSION)
					  .copyTo(resultsFilename);
		assertTrue(Files.exists(resultsFilename), "Expected '" + resultsFilename + "' to exist but it does not.");
		assertTrue(isArchive(Files.readAllBytes(resultsFilename)), "Expected response to be a .zip/.jar but is was not. Results in '" + resultsFilename + "'.");
	}

	@Tag("Integration")
	@DisplayName("Integration test - test GithinPackages.fluent copyTo directory method.")
	@ParameterizedTest
	@CsvSource(value = {
			"4PointSolutions,FluentFormsAPI,com._4point.aem,fluentforms.core,0.0.2-SNAPSHOT",
			"4PointSolutions,FluentFormsAPI,com._4point.aem.docservices,rest-services.client,0.0.2-SNAPSHOT",
			"4PointSolutions,FluentFormsAPI,com._4point.aem,fluentforms.core,0.0.3",
			"4PointSolutions,FluentFormsAPI,com._4point.aem.docservices,rest-services.client,0.0.3"
	})
	void testCopyToDir(String userOrg, String repo, String groupId, String artifactId, String version, @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
		Path expectedResultsFilename = tempDir.resolve("%s-%s.jar".formatted(artifactId, version));
		GithubPackages.create()
					  .repo(userOrg, repo)
					  .group(groupId)
					  .artifact(artifactId)
					  .version(version)
					  .copyTo(tempDir);
		assertTrue(Files.exists(expectedResultsFilename), "Expected '" + expectedResultsFilename + "' to exist but it does not.");
		assertTrue(isArchive(Files.readAllBytes(expectedResultsFilename)), "Expected response to be a .zip/.jar but is was not. Results in '" + expectedResultsFilename + "'.");
	}

	@Tag("Integration")
	@DisplayName("Integration test - test GithinPackages.fluent copyTo directory method with other repositories.")
	@ParameterizedTest
	@CsvSource(value = {
			"4PointSolutions,FluentFormsAPI,com._4point.aem,fluentforms.core,0.0.2-SNAPSHOT",
			"4PointSolutions,FluentFormsAPI,com._4point.aem.docservices,rest-services.client,0.0.2-SNAPSHOT",
			"4PointSolutions,FluentFormsAPI,com._4point.aem,fluentforms.core,0.0.3",
			"4PointSolutions,FluentFormsAPI,com._4point.aem.docservices,rest-services.client,0.0.3"
	})
	void testCopyToDirOtherRepos(String userOrg, String repo, String groupId, String artifactId, String version, @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
		Path expectedResultsFilename = tempDir.resolve("%s-%s.jar".formatted(artifactId, version));
		GithubPackages.create()
					  .repo(userOrg, repo)
					  .group(groupId)
					  .artifact(artifactId)
					  .version(version)
					  .copyTo(tempDir.resolve(expectedResultsFilename));
		assertTrue(Files.exists(expectedResultsFilename), "Expected '" + expectedResultsFilename + "' to exist but it does not.");
		assertTrue(isArchive(Files.readAllBytes(expectedResultsFilename)), "Expected response to be a .zip/.jar but is was not. Results in '" + expectedResultsFilename + "'.");
	}

	private static boolean isArchive(byte[] bytes) {
	    int fileSignature = ByteBuffer.wrap(bytes).getInt();
	    return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
	}
}
