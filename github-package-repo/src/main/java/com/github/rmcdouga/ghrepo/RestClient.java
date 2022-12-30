package com.github.rmcdouga.ghrepo;

import java.io.InputStream;

public interface RestClient {

	InputStream get(String path);

}