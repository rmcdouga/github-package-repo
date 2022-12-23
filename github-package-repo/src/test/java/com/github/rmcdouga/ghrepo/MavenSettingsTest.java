package com.github.rmcdouga.ghrepo;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.rmcdouga.ghrepo.MavenSettings.Credentials;

class MavenSettingsTest {


	@Disabled("Requires a user with a settings.xml file in their home directory.  This isn't always the case.")
	@Test
	void testCredentials_DefaultLocation() throws Exception {
		MavenSettings underTest = MavenSettings.get();
		
		Credentials credentials = underTest.credentials("github");
		
		assertThat(credentials.username().length(), greaterThan(0));
		assertThat(credentials.password().length(), greaterThan(0));
	}

	@Test
	void testCredentials() throws Exception {
		MavenSettings underTest = MavenSettings.from(TestUtils.SAMPLE_FILES_DIR.resolve("settings.xml"));
		
		Credentials credentials = underTest.credentials("github");
		
		assertEquals("users_name", credentials.username());
		assertEquals("developer_token", credentials.password());
	}
}
