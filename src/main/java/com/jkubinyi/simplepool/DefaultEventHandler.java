package com.jkubinyi.simplepool;

/**
 * Default event handler which is totally quiet. Does not do anything.
 * 
 * @author jurajkubinyi
 */
public class DefaultEventHandler implements PoolEventHandler {

	@Override
	public void newEvent(Severity severity, String format, Object... objects) {
		// Do nothing
	}

}
