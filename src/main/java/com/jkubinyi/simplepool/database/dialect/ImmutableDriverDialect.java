package com.jkubinyi.simplepool.database.dialect;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Optional;

/**
 * Used to implement {@link Driver} specific configuration for the connection.
 * 
 * @author jurajkubinyi
 */
public interface ImmutableDriverDialect {
	/**
	 * Should return an engine specific lightweight query used to check for the Connection status
	 * wrapped in Optional or in case of empty Optional the system will use default verification
	 * for the driver (See {@link Connection#isValid(int)})
	 * 
	 * @return Representation of the query or empty optional in case of default driver test.
	 */
	public Optional<String> getLinkValidityQuery();
}
