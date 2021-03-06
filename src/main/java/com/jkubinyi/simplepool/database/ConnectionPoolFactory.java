package com.jkubinyi.simplepool.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.jkubinyi.simplepool.GenericPool;
import com.jkubinyi.simplepool.ObjectPoolFactory;
import com.jkubinyi.simplepool.PoolObject;

/**
 * <p>An {@link ObjectPoolFactory} implementation used to create {@link Connection}
 * objects to the database for the {@link ConnectionPool}.</p>
 * 
 * @author jurajkubinyi
 *
 */
public class ConnectionPoolFactory implements ObjectPoolFactory<Connection> {

	private final ConnectionPoolConfiguration config;
	private AtomicInteger failureCount = new AtomicInteger(0);
	private volatile boolean inFallback = false;
	
	public ConnectionPoolFactory(ConnectionPoolConfiguration config) {
		this.config = config;
	}
	
	@Override
	public PoolObject<Connection> produceObject(GenericPool<Connection> pool) throws Exception {
		Connection connection = null;
		if(!this.inFallback) {
			if(this.failureCount.get() < this.config.getNumOfFailsToFallback()) {
				try {
					connection = this.createMainConnection(pool);
				} catch(SQLException e) { } // TODO: do logging
				if(connection == null || connection.isClosed()) {
					this.failureCount.incrementAndGet();
					return this.produceObject(pool);
				}
			} else {
				this.inFallback = true;
				return this.produceObject(pool);
			}
		} else {
			if(this.config.getFallbackJdbcUrls().size() == 0) throw new IllegalStateException("Main server is unreachable. No fallback servers are configured.");
			if(this.config.isEagerRetry()) {
				try {
					connection = this.createMainConnection(pool);
				} catch(SQLException e) { } // Do nothing, will create a fallback connection
			}
			if(connection == null) {
				try {
					connection = this.createFallbackConnection(pool);
				} catch(SQLException e) { } // TODO: do logging
			} else {
				this.inFallback = false;
			}
		}
		if(connection == null) throw new IllegalStateException("Cannot obtain Connection.");
		
		this.config.getConnectionConfiguration().configure(connection);
		return new PoolObject<Connection>(connection);
	}

	@Override
	public void destroyObject(PoolObject<Connection> object) throws Exception {
		try {
			Connection connection = object.getObject();
			connection.rollback(); // Will forcefully rollback in case of error -> Won't look at preference
			connection.superClose(); // Will close underlying Connection
		} catch(Exception e) { }
	}

	@Override
	public boolean validateObject(PoolObject<Connection> object) throws Exception {
		Connection connection = object.getObject();
		if(this.config.isEagerRetry() && !connection.getJdbcUrl().equals(this.config.getMainJdbcUrl())) {
			if(this.tryConnectivity(this.config.getMainJdbcUrl())) {
				this.inFallback = false;
				return false;
			}
		}
		
		if(this.validateConnection(connection)) return true;
		return false;
	}

	@Override
	public void activateObject(PoolObject<Connection> object) throws Exception {
	}

	@Override
	public void sleepObject(PoolObject<Connection> object) throws Exception {
		switch (this.config.getReturnStrategy()) {
		case ROLLBACK:
			object.getObject().rollback();
			break;
		case COMMIT:
			object.getObject().commit();
			break;

		default:
			break;
		}
	}
	
	// Methods directly connected to the ConnectionPoolFactory logic
	/**
	 * @return {@code true} if the pool uses a fallback (backup) connection.
	 * Should automatically recover when the main server goes back online depending on the configuration.
	 */
	public boolean inFallbackMode() {
		return this.inFallback;
	}
	
	private synchronized Connection createFallbackConnection(GenericPool<Connection> pool) throws SQLException {
		Connection connection = null;
		for(JDBCUrl jdbcUrl : this.config.getFallbackJdbcUrls()) {
			try {
				connection = this.createConnection(jdbcUrl, pool);
			} catch(Exception e) {
				// TODO: log
			}
			if(connection != null && !connection.isClosed()) break;
		}
		return connection;
	}
	
	private Connection createMainConnection(GenericPool<Connection> pool) throws SQLException {
		return this.createConnection(this.config.getMainJdbcUrl(), pool);
	}
	
	private boolean validateConnection(Connection connection) {
		return this.validateConnection(connection.getConnection(), connection.getJdbcUrl());
	}
	
	private boolean validateConnection(java.sql.Connection connection, JDBCUrl jdbcUrl) {
		try {
			if(!connection.isClosed()) {
				boolean result;
				Optional<String> optionalLinkValidityQuery = jdbcUrl.getDialect().getLinkValidityQuery();
				
				if(optionalLinkValidityQuery.isPresent()) {
					try(Statement st = connection.createStatement()) {
						st.setQueryTimeout(this.config.getMaxValidationTimeoutInS());
						result = st.execute(optionalLinkValidityQuery.get());
					}
				} else
					result = connection.isValid(this.config.getMaxValidationTimeoutInS());
				return result;
			}
		} catch(SQLException e) {
			return false;
		}
		return false;
	}
	
	/**
	 * Used to try if the JDBCUrl is reachable or not.
	 * 
	 * @param jdbcUrl JDBCUrl to test.
	 * @return {@code true} if it can be reached and the link is working.
	 */
	private boolean tryConnectivity(JDBCUrl jdbcUrl) {
		String url = jdbcUrl.getUrl();
		Optional<String> optionalUsername = jdbcUrl.getUsername();
		Optional<String> optionalPassword = jdbcUrl.getPassword();
		
		java.sql.Connection connection = null;
		try {
			if(optionalUsername.isPresent())
				connection = DriverManager.getConnection(url, optionalUsername.get(), optionalPassword.orElse(null));
			else
				connection = DriverManager.getConnection(url);
			
			if(this.validateConnection(connection, jdbcUrl)) {
				try {
					connection.close();
				} catch(SQLException e) { }
				return true;
			} else {
				try {
					connection.close();
				} catch(SQLException e) { }
			}
		} catch(SQLException e) { }
		
		return false;
	}
	
	private Connection createConnection(JDBCUrl jdbcUrl, GenericPool<Connection> pool) throws SQLException {
		String url = jdbcUrl.getUrl();
		Optional<String> optionalUsername = jdbcUrl.getUsername();
		Optional<String> optionalPassword = jdbcUrl.getPassword();
		
		java.sql.Connection connection;
		if(optionalUsername.isPresent())
			connection = DriverManager.getConnection(url, optionalUsername.get(), optionalPassword.orElse(null));
		else
			connection = DriverManager.getConnection(url);
		
		if(connection != null) return Connection.of(connection, pool, jdbcUrl);
		
		return null;
	}

}
