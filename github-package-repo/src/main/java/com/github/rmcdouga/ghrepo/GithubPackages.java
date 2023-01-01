package com.github.rmcdouga.ghrepo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import com.github.rmcdouga.ghrepo.MavenSettings.Credentials;

public class GithubPackages {
	RestClient restClient;
	
	GithubPackages(RestClient restClient) {
		this.restClient = restClient;
	}

	public static GithubPackages withToken(String githubToken) {
		return new GithubPackages(RestClientFactory.createJersey("https://maven.pkg.github.com", githubToken));
	}

	public static GithubPackages withServerId(String serverId, Path settings) throws FileNotFoundException {
		return withToken(getToken(serverId, MavenSettings.from(settings)));
	}

	public static GithubPackages withServerId(String serverId) throws FileNotFoundException {
		return withToken(getToken(serverId, MavenSettings.get()));
	}

	private static String getToken(String serverId, MavenSettings settings) {
		Credentials creds = settings.credentials(serverId);
		return creds.password();
	}
	
	public static GithubPackages create() throws FileNotFoundException {
		return withServerId("github");
	}
	
	
	public InputStream get(String userOrg, String repo, String groupId, String artifactId, String version) throws IOException {
		String path = "/%s/%s/%s/%s/%s/".formatted(userOrg, repo, groupId.replace('.', '/'), artifactId, version);
		// Get the Maven Metadata first
		byte[] metadataBytes = restClient.get(path + "maven-metadata.xml").readAllBytes();
		// Determine the latest version
		String latestJarName = MavenMetadata.from(metadataBytes).getLatestJarName();
		// Get the latest version
		return restClient.get(path + latestJarName);
	}
	
	public Repo repository(String userOrg, String repo) {
		return new Repo(userOrg, repo);
	}
	
	public class Repo {
		String userOrg;
		String repo;
		
		private Repo(String userOrg, String repo) {
			this.userOrg = userOrg;
			this.repo = repo;
		}

		InputStream get(String groupId, String artifactId, String version) {
			return null;
		}
	}
}
