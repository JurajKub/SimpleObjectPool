package com.jkubinyi.simplepool.database.dialect;

import java.util.Optional;

public class MySqlDialect implements ImmutableDriverDialect {
	
	@Override
	public Optional<String> getLinkValidityQuery() {
		return Optional.of("SELECT 1");
	}

}
