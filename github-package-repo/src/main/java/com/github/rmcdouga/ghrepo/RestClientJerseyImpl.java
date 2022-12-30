package com.github.rmcdouga.ghrepo;

import java.io.InputStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

public class RestClientJerseyImpl {
	private static final Client CLIENT = ClientBuilder.newClient();
	private final WebTarget baseTarget;
	private final String githubToken;
	
	public RestClientJerseyImpl(String baseUrl, String githubToken){
		 this.baseTarget = CLIENT.target(baseUrl);
		 this.githubToken = githubToken;
	}
	
	public InputStream get(String path) {
		Response response = baseTarget.path(path)
									  .request()
									  .header("Authorization", "Bearer %s".formatted(githubToken))
									  .get();
		return response.readEntity(InputStream.class);
	}
}
