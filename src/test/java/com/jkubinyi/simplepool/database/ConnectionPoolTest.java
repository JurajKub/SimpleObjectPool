package com.jkubinyi.simplepool.database;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jkubinyi.simplepool.PoolConfiguration;
import com.jkubinyi.simplepool.database.dialect.MySqlDialect;

public class ConnectionPoolTest {

	private static ConnectionPool pool;
	private static ConnectionPoolConfiguration connectionConfig;
	private static PoolConfiguration poolConfig;
	
	@BeforeClass
	public static void prepareTest() {
		ConnectionPoolTest.connectionConfig = 
				new ConnectionPoolConfiguration.Builder(
						JDBCUrl.from(new MySqlDialect(), "jdbc:mysql://localhost:3305/springTest?useSSL=false", "root", "")
				)
				.addFallbackUrl(
						JDBCUrl.from(new MySqlDialect(), "jdbc:mysql://localhost:3306/SpringTest?useSSL=false", "r00t", "")
				)
				.addFallbackUrl(
						JDBCUrl.from(new MySqlDialect(), "jdbc:mysql://localhost:3306/SpringTest?useSSL=false", "root", "password")
				)
				.setConnectionConfiguration(connection -> {
					System.out.println(connection);
					connection.setAutoCommit(false);
				})
				.setEagerRetry(true)
				.setNumOfFailsToFallback(10)
				.setWaitTimeBetweenRetryInS(5)
				.build();
		
		ConnectionPoolTest.poolConfig = new PoolConfiguration.Builder()
				.setMaxPoolSize(10)
				.setInitialPoolSize(5)
				.build();
		
		ConnectionPoolTest.pool = new ConnectionPool(ConnectionPoolTest.connectionConfig, ConnectionPoolTest.poolConfig);
		ConnectionPoolTest.pool.create();
	}
	
	@Test
	public void autoReturnConnectionTryWithResources() throws Exception {
		int numOfIdleObjects = ConnectionPoolTest.pool.getNumIdle();
		try(Connection connection = ConnectionPoolTest.pool.getConnection()) {
			assertEquals(false, connection.isClosed());
		}
		assertEquals(numOfIdleObjects, ConnectionPoolTest.pool.getNumIdle());
	}

}
