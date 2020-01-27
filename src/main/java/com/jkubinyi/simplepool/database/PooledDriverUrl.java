package com.jkubinyi.simplepool.database;

public interface PooledDriverUrl {
	
	/**
	 * Should return a valid JDBC URL for use with Driver.
	 * @return String representation of the valid JDBC URL.
	 */
	public String getJdbcUrl();
	
	/**
	 * Should return an engine specific lightweight query used to check for the Connection status.
	 * @return String representation of the query.
	 */
	public String getLinkValidityQuery();
}
