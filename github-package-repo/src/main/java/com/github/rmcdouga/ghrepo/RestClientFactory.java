package com.github.rmcdouga.ghrepo;

public interface RestClientFactory {
	static RestClient createJersey(String baseUrl, String githubToken) {
		return new RestClientJerseyImpl(baseUrl, githubToken);
	}
}
