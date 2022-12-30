package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

@WireMockTest
class RestClientJerseyImplTest {

private static final String TARGET_URL = "/4PointSolutions/WatchedFolderUtils/com/_4point/aem/watchedfolder/watched-folder-poster/0.0.1-SNAPSHOT/watched-folder-poster-0.0.1-20221221.221800-4.jar";
	private static final String MOCK_GITHUB_TOKEN = "mock_github_token";
	private static final boolean WIREMOCK_RECORDING = false;	// true tells WIREMOCK to call AEM and record the result
	private static final boolean SAVE_RESULTS = false;			// true saves the resuts in the actualResults directory
	private final RestClientJerseyImpl underTest;

	private RestClientJerseyImplTest(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = new RestClientJerseyImpl("http://localhost:%d".formatted(wmRuntimeInfo.getHttpPort()), MOCK_GITHUB_TOKEN);
	}

	@BeforeEach
	void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		if (WIREMOCK_RECORDING) {
			String realServiceBaseUri = "https://maven.pkg.github.com";
			WireMock.startRecording(realServiceBaseUri);
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		if (WIREMOCK_RECORDING) {
			SnapshotRecordResult recordings = WireMock.stopRecording();
			List<StubMapping> mappings = recordings.getStubMappings();
			System.out.println("Found " + mappings.size() + " recordings.");
			for (StubMapping mapping : mappings) {
				ResponseDefinition response = mapping.getResponse();
				var jsonBody = response.getJsonBody();
				System.out.println(jsonBody == null ? "JsonBody is null" : jsonBody.toPrettyString());
			}
		}
	}

	@Test
	void testGet() throws Exception {
		Path saveLocation = TestUtils.ACTUAL_RESULTS_DIR.resolve("RestClientJerseyImplTest_testGet.jar");
		
		InputStream resultStream = underTest.get(TARGET_URL);
		
		assertNotNull(resultStream);
		byte[] resultBytes = resultStream.readAllBytes();
		
		if (SAVE_RESULTS) {
			Files.write(saveLocation, resultBytes);
		}
		assertTrue(isArchive(resultBytes), "Expected response to be a .zip/.jar but is was not.");
		
		WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(TARGET_URL))
		        .withHeader("Authorization", WireMock.equalTo("Bearer " + MOCK_GITHUB_TOKEN))
		        );
	}
	
	private static boolean isArchive(byte[] bytes) {
	    int fileSignature = ByteBuffer.wrap(bytes).getInt();
	    return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
	}
}
