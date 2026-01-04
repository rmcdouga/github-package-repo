package com.github.rmcdouga.ghrepo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.rmcdouga.ghrepo.MavenSettings.Credentials;
import com.github.rmcdouga.ghrepo.XmlDocument.XmlDocumentException;

public class GithubPackages {
	private static final String DEFAULT_ARTIFACT_EXTENSION = "jar";
	
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
	
	public InputStream get(String userOrg, String repo, String groupId, String artifactId, String version, String artifactExtension) throws IOException {
		return internalGet(userOrg, repo, groupId, artifactId, version, artifactExtension).resultStream();
	}

	public InputStream get(String userOrg, String repo, String groupId, String artifactId, String version) throws IOException {
		return get(userOrg, repo, groupId, artifactId, version, DEFAULT_ARTIFACT_EXTENSION);
	}

	private static record GetResult(InputStream resultStream, String artifactName) {} ;

	private GetResult internalGet(String userOrg, String repo, String groupId, String artifactId, String version, String artifactExtension)
			throws IOException {
		String path = "/%s/%s/%s/%s/%s/".formatted(userOrg, repo, groupId.replace('.', '/'), artifactId, version);
		return version.endsWith("SNAPSHOT") ? getSnapshot(artifactId, artifactExtension, path) 
											: getFinal(artifactId, version, artifactExtension, path);
	}

	private GetResult getFinal(String artifactId, String version, String artifactExtension, String path) throws IOException {
		// https://maven.pkg.github.com/4PointSolutions/FluentFormsAPI/com/_4point/aem/fluentforms.core/0.0.3/fluentforms.core-0.0.3.jar
		return new GetResult(restClient.get("%s%s-%s.%s".formatted(path, artifactId, version, artifactExtension)), "%s-%s.%s".formatted(artifactId, version, artifactExtension));
	}

	private GetResult getSnapshot(String artifactId, String artifactExtension, String path) throws IOException {
		// Get the Maven Metadata first
		byte[] metadataBytes = restClient.get(path + "maven-metadata.xml").readAllBytes();
		try {
			// Determine the latest version
			MavenMetadata mavenMetadata = MavenMetadata.from(metadataBytes, artifactExtension);
			String latestJarName = mavenMetadata.getLatestArtifactName(artifactId);
			// Get the latest version
			return new GetResult(restClient.get(path + latestJarName), mavenMetadata.getSnapshotName(artifactId));
		} catch (XmlDocumentException e) {
			String metaDataXml = new String(metadataBytes, StandardCharsets.UTF_8);
			throw new XmlDocumentException("Error parsing Metadata, Xml=''.".formatted(metaDataXml), e);
		}
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
		
		public InputStream get(String groupId, String artifactId, String versionId, String artifactExtension) throws IOException {
			return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, artifactExtension);
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
			
			public InputStream get(String artifactId, String versionId, String artifactExtension) throws IOException {
				return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, artifactExtension);
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
				
				public InputStream get(String versionId, String artifactExtension) throws IOException {
					return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, artifactExtension);
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
						return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, DEFAULT_ARTIFACT_EXTENSION);
					}
					
					public InputStream get(String artifactExtension) throws IOException {
						return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, artifactExtension);
					}

					public long copyTo(final Path target, final CopyOption... options) throws IOException {
						return extension(DEFAULT_ARTIFACT_EXTENSION).copyTo(target, options);
					}

					public Extension extension(String artifactExtension) {
						return new Extension(artifactExtension);
					}
					
					public class Extension {
						private final String extension;

						public Extension(String artifactExtension) {
							this.extension = artifactExtension;
						}
						
						public InputStream get() throws IOException {
							return GithubPackages.this.get(userOrg, repo, groupId, artifactId, versionId, extension);
						}
						
						public long copyTo(final Path target, final CopyOption... options) throws IOException {
							if (Files.exists(target) && Files.isDirectory(target)) {
								GetResult getResult = GithubPackages.this.internalGet(userOrg, repo, groupId, artifactId, versionId, extension);
								return copy(getResult.resultStream(), target.resolve(getResult.artifactName()), options);
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
}
