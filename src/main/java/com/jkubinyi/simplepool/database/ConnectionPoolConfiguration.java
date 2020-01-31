package com.jkubinyi.simplepool.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>Configuration class for the {@link ConnectionPoolFactory}. Includes possibility to have multiple
 * fail-over JDBC URLs and one main JDBC URL defined.</p>
 * 
 * @author jurajkubinyi
 *
 */
public final class ConnectionPoolConfiguration {
	
	/**
	 * Used to one time configure freshly made {@link Connection}. Useful
	 * for setting {@link Connection}'s properties like autocommit, etc.
	 * 
	 * @author jurajkubinyi
	 */
	@FunctionalInterface
	public interface ConnectionConfiguration {
		void configure(Connection connection) throws Exception;
	}
	
	enum ReturnStrategy {
		ROLLBACK,
		COMMIT,
		NO_ACTION
	}
	
	private final JDBCUrl mainJdbcUrl;
	
	private final List<JDBCUrl> fallbackJdbcUrls;
	
	private final int numOfFailsToFallback;
	
	private final int waitTimeBetweenRetryInS;
	
	private final int maxValidationTimeoutInS;
	
	private final boolean eagerRetry;
	
	private final ReturnStrategy returnStrategy;
	
	private final ConnectionConfiguration connectionConfiguration;
	
	private ConnectionPoolConfiguration(JDBCUrl mainJdbcUrl, List<JDBCUrl> fallbackJdbcUrls, int numOfFailsToFallback, int waitTimeBetweenRetryInS, int maxValidationTimeoutInS, boolean eagerRetry, ReturnStrategy returnStrategy, ConnectionConfiguration connectionConfiguration) {
		this.mainJdbcUrl = mainJdbcUrl;
		this.fallbackJdbcUrls = Collections.unmodifiableList(fallbackJdbcUrls);
		this.numOfFailsToFallback = numOfFailsToFallback;
		this.waitTimeBetweenRetryInS = waitTimeBetweenRetryInS;
		this.maxValidationTimeoutInS = maxValidationTimeoutInS;
		this.eagerRetry = eagerRetry;
		this.returnStrategy = returnStrategy;
		this.connectionConfiguration = connectionConfiguration;
	}

	/**
	 * @return Main URL for the JDBC Driver. Uses native DriverManager to find appropriate Driver on class path.
	 */
	public JDBCUrl getMainJdbcUrl() {
		return mainJdbcUrl;
	}

	/**
	 * @return Fallback URLs for the JDBC Driver. Uses native DriverManager to find appropriate Driver on class path.
	 */
	public List<JDBCUrl> getFallbackJdbcUrls() {
		return fallbackJdbcUrls;
	}

	/**
	 * @return How many fails are needed to switch to one of fallback URLs.
	 */
	public int getNumOfFailsToFallback() {
		return numOfFailsToFallback;
	}

	/**
	 * @return If true before returning fallback Connection from the pool verifies whether main server did not come online again.
	 */
	public boolean isEagerRetry() {
		return eagerRetry;
	}

	/**
	 * @return Maximum number of seconds to wait till marking the connection as not valid.
	 */
	public int getMaxValidationTimeoutInS() {
		return maxValidationTimeoutInS;
	}

	/**
	 * @return <p>Number of seconds to wait between next retry.</p> <b>Caution:</b> Higher values might slow down connection acquiring
	 * from the {@link ConnectionPoolFactory}. <i>On the other hand, using small values might trip {@link #getNumOfFailsToFallback()}
	 * and make fall into the fail-over mode prematurely.</i>
	 */
	public int getWaitTimeBetweenRetryInS() {
		return waitTimeBetweenRetryInS;
	}

	/**
	 * @return Strategy used before returning object back to the pool.
	 */
	public ReturnStrategy getReturnStrategy() {
		return returnStrategy;
	}

	/**
	 * @return {@link ConnectionConfiguration} which is applied to each {@link Connection} by
	 * {@link ConnectionPoolFactory} right after creating a new {@link java.sql.Connection}.
	 */
	public ConnectionConfiguration getConnectionConfiguration() {
		return connectionConfiguration;
	}

	public static class Builder {
		private final JDBCUrl mainJdbcUrl;
		
		private final List<JDBCUrl> fallbackJdbcUrls = new ArrayList<>();
		
		private int numOfFailsToFallback = 5;

		private int waitTimeBetweenRetryInS = 5;
		
		private int maxValidationTimeoutInS = 1;
		
		private boolean eagerRetry = true;
		
		private ReturnStrategy returnStrategy = ReturnStrategy.ROLLBACK;
		
		private ConnectionConfiguration connectionConfiguration = Builder.defaultConnectionConfiguration;
		
		private static ConnectionConfiguration defaultConnectionConfiguration = new ConnectionConfiguration() {
			@Override
			public void configure(Connection connection) {
			}
		};

		/**
		 * Builder used to create configuration for the {@link ConnectionPoolFactory}
		 * @param mainJdbcUrl Main server URL to connect to
		 */
		public Builder(JDBCUrl mainJdbcUrl) {
			Objects.requireNonNull(mainJdbcUrl, "JDBCUrl cannot be null.");
			this.mainJdbcUrl = mainJdbcUrl;
		}

		/**
		 * @param numOfFailsToFallback How many fails are needed to switch to one of fallback URLs.
		 * @return Builder instance.
		 */
		public Builder setNumOfFailsToFallback(int numOfFailsToFallback) {
			this.numOfFailsToFallback = numOfFailsToFallback;
			return this;
		}
		
		/**
		 * @param eagerRetry If true before returning fallback Connection from the pool verifies
		 * whether main server did not come online again.
		 * @return Builder instance.
		 */
		public Builder setEagerRetry(boolean eagerRetry) {
			this.eagerRetry = eagerRetry;
			return this;
		}

		/**
		 * @param url How many fails are needed to switch to one of fallback URLs.
		 * @return Builder instance.
		 */
		public Builder addFallbackUrl(JDBCUrl url) {
			this.fallbackJdbcUrls.add(url);
			return this;
		}
		
		/**
		 * @param waitTimeBetweenRetryInS <p>Number of seconds to wait between next retry.</p> <b>Caution:</b> Higher values might slow down connection acquiring
		 * from the {@link ConnectionPoolFactory}. <i>On the other hand, using small values might trip {@link #getNumOfFailsToFallback()}
		 * and make fall into the fail-over mode prematurely.</i>
		 * @return Builder instance.
		 */
		public Builder setWaitTimeBetweenRetryInS(int waitTimeBetweenRetryInS) {
			this.waitTimeBetweenRetryInS = waitTimeBetweenRetryInS;
			return this;
		}
		
		/**
		 * @param maxValidationTimeoutInS Maximum number of seconds to wait till marking the connection as not valid.
		 * @return Builder instance.
		 */
		public Builder setMaxValidationTimeoutInS(int maxValidationTimeoutInS) {
			this.maxValidationTimeoutInS = maxValidationTimeoutInS;
			return this;
		}

		/**
		 * @param returnStrategy Strategy used before returning object back to the pool.
		 * @return Builder instance.
		 */
		public Builder setReturnStrategy(ReturnStrategy returnStrategy) {
			this.returnStrategy = returnStrategy;
			return this;
		}
		
		/**
		 * @param connectionConfiguration {@link ConnectionConfiguration} which is applied to each {@link Connection} by
		 * {@link ConnectionPoolFactory} right after creating a new {@link java.sql.Connection}.
		 * @return Builder instance.
		 */
		public Builder setConnectionConfiguration(ConnectionConfiguration connectionConfiguration) {
			this.connectionConfiguration = connectionConfiguration;
			return this;
		}
		
		/**
		 * @return Creates the {@link ConnectionPoolConfiguration} instance.
		 */
		public ConnectionPoolConfiguration build() {
			return new ConnectionPoolConfiguration(this.mainJdbcUrl, this.fallbackJdbcUrls, this.numOfFailsToFallback, this.waitTimeBetweenRetryInS, this.maxValidationTimeoutInS, this.eagerRetry, this.returnStrategy, this.connectionConfiguration);
		}
	}
}
