package com.github.rmcdouga.ghrepo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.rmcdouga.ghrepo.MavenSettings.Credentials;

public class GithubPackages {
	private final RestClient restClient;
	private final boolean verboseMode;

	private GithubPackages(RestClient restClient, boolean verboseMode) {
		this.restClient = restClient;
		this.verboseMode = verboseMode;
	}

	// package visibility for unit tests.
	GithubPackages(RestClient restClient) {
		this(restClient, false);
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
	
	public GithubPackages verboseMode(boolean verboseMode) {
		return new GithubPackages(restClient, verboseMode);
	}
	
	public InputStream get(String userOrg, String repo, String groupId, String artifactId, String version) throws IOException {
		return internalGet(userOrg, repo, groupId, artifactId, version).resultStream();
	}

	private static record GetResult(InputStream resultStream, String jarName) {} ;

	private GetResult internalGet(String userOrg, String repo, String groupId, String artifactId, String version)
			throws IOException {
		String path = "/%s/%s/%s/%s/%s/".formatted(userOrg, repo, groupId.replace('.', '/'), artifactId, version);
		// Get the Maven Metadata first
		byte[] metadataBytes = restClient.get(path + "maven-metadata.xml").readAllBytes();
		// Determine the latest version
		MavenMetadata mavenMetadata = MavenMetadata.from(metadataBytes, "jar");
		String latestJarName = mavenMetadata.getLatestArtifactName(artifactId);
		// Get the latest version
		return new GetResult(restClient.get(path + latestJarName), mavenMetadata.getSnapshotName(artifactId));
	}
	

	public Repo repo(String userOrg, String repo) {
		return new Repo(userOrg, repo);
	}
	
	public class Repo {
		private final String userOrg;
		private final String repo;
		
		private Repo(String userOrg, String repo) {
			this.userOrg = userOrg;
			this.repo = repo;
		}

		public InputStream get(String groupId, String artifactId, String versionId) throws IOException {
			return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId);
		}
		
		public Group group(String groupId) {
			return new Group(groupId);
		}
		
		public class Group {
			private final String groupId;

			private Group(String groupId) {
				this.groupId = groupId;
			}
			
			public InputStream get(String artifactId, String versionId) throws IOException {
				return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId);
			}
			
			public Artifact artifact(String artifactId) {
				return new Artifact(artifactId);
			}
			
			public class Artifact {
				private String artifactId;

				private Artifact(String artifactId) {
					this.artifactId = artifactId;
				}

				public InputStream get(String versionId) throws IOException {
					return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId);
				}
				
				public Version version(String versionId) {
					return new Version(versionId);
				}
				
				public class Version {
					private final String versionId;

					private Version(String versionId) {
						this.versionId = versionId;
					}

					public InputStream get() throws IOException {
						return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId);
					}
					
					public long copyTo(final Path target, final CopyOption... options) throws IOException {
						if (Files.exists(target) && Files.isDirectory(target)) {
							GetResult getResult = GithubPackages.this.internalGet(userOrg, repo, groupId, artifactId, versionId);
							return copy(getResult.resultStream(), target.resolve(getResult.jarName()), options);
						} else {
							InputStream in = get();
							return copy(in, target, options);
						}
					}

					private long copy(final InputStream in, final Path target, final CopyOption... options) throws IOException {
						if (verboseMode) {
							System.out.println("Copying to '" + target.toString() + "'.");
						}
						return Files.copy(in, target, options);
					}
					
				}
			}
		}
	}
	
}
