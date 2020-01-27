package com.jkubinyi.simplepool.database;

import com.jkubinyi.simplepool.GenericPoolImpl;
import com.jkubinyi.simplepool.PoolConfiguration;

public class ConnectionPool extends GenericPoolImpl<Connection> {

	public ConnectionPool(ConnectionPoolFactory factory, PoolConfiguration config) {
		super(factory, config);
	}
	
	public ConnectionPool(ConnectionPoolConfiguration config, PoolConfiguration poolConfig) {
		super(new ConnectionPoolFactory(config), poolConfig);
	}
	
	/**
	 * @see #borrowObject()
	 * 
	 * @return Pooled instance of {@link Connection}. Suitable for try-with-resources use.
	 * @throws Exception When unrecoverable error pops up during getting Connection from the pool.
	 */
	public Connection getConnection() throws Exception {
		return this.borrowObject();
	}
	
	public ConnectionPoolFactory getFactory() {
		return (ConnectionPoolFactory) this.factory;
	}
	
}
