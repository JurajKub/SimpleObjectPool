package com.jkubinyi.simplepool.database;

import java.util.Objects;
import java.util.Optional;

import com.jkubinyi.simplepool.database.dialect.ImmutableDriverDialect;

public class JDBCUrl {
	
	private final ImmutableDriverDialect dialect;
	private final String url;
	private final String username;
	private final String password;
	
	private JDBCUrl(ImmutableDriverDialect dialect, String url, String username, String password) {
		Objects.requireNonNull(dialect, "Dialect cannot be null.");
		Objects.requireNonNull(url, "Url cannot be null.");
		
		this.dialect = dialect;
		this.url = url;
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Returns an instance of anonymous JDBC connection to the desired server using url and dialect.
	 * @param dialect Dialect for the engine running on the server.
	 * @param url JDBC url for connecting to the server
	 * @return Anonymous JDBCUrl
	 */
	public static JDBCUrl fromAnonymous(ImmutableDriverDialect dialect, String url) {
		return new JDBCUrl(dialect, url, null, null);
	}
	
	/**
	 * Returns an instance of JDBC connection with authentication using username/password combination
	 * to the desired server using url and dialect.
	 * @param dialect Dialect for the engine running on the server
	 * @param url JDBC url for connecting to the server
	 * @param username Username for authentication
	 * @param password Password for authentication
	 * @return JDBCUrl with authentication during connection
	 */
	public static JDBCUrl from(ImmutableDriverDialect dialect, String url, String username, String password) {
		return new JDBCUrl(dialect, url, username, password);
	}

	/**
	 * @return Gets the dialect used during communication with the server.
	 */
	public ImmutableDriverDialect getDialect() {
		return dialect;
	}

	/**
	 * @return Gets the url used to initiate communication with the server.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return Gets the username used to initiate communication with the server.
	 */
	public Optional<String> getUsername() {
		return Optional.ofNullable(username);
	}

	protected Optional<String> getPassword() {
		return Optional.ofNullable(password);
	}
}
