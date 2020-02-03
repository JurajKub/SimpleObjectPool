package com.jkubinyi.simplepool.common;

import com.jkubinyi.simplepool.GenericPool;

/**
 * Called by the {@link GenericPool} when some important event happens.
 * 
 * @author jurajkubinyi
 */
public interface PoolEventHandler {
	enum Severity {
		info,
		debug,
		warn,
		error
	}
	
	/**
	 * Will be called by the {@link GenericPool} when some important event happens.
	 * <b>This method should not throw any exception.</b>
	 * 
	 * @param severity Severity of the event.
	 * @param format L4J compatible format for easy logging.
	 * @param objects Arguments for the event.
	 */
	public void newEvent(Severity severity, String format, Object... objects);
}
