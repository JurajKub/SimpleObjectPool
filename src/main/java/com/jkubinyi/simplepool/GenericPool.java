package com.jkubinyi.simplepool;

public interface GenericPool<T> {
	
	/**
	 * @return Gets current number of active (allocated) objects in use.
	 */
	public int getNumActive();

	/**
	 * @return Gets current number of sleeping (deallocated) objects prepared to be used.
	 */
	public int getNumIdle();

	/**
	 * @return Gets total number of created objects (including already destroyed) counting
	 * from initiating the pool. No methods can reset this counter, nor {@link #clear()}.
	 */
	public long getNumCreated();
	
	// Main methods API - create, borrow, return and destroy
	/**
	 * <p>Used to start the object pool ensuring minimum number of objects are available at the disposal.</p>
	 * <b>Blocking operation</b>. Should be ideally called once per application
	 * lifecycle (at the startup, change of application configuration, etc.)
	 */
	public void create();
	
	/**
	 * <p>Requests an object from the pool by reusing idle objects or creating a new one depending on the pool
	 * allocation and configuration.</p>
	 * The returned object needs to be returned back to the pool or <b>memory leak could happen.</b>
	 * 
	 * @return Prepared object to be used.
	 * @throws Exception Throws exception in case of fatal failure during gathering object.
	 */
	public T borrowObject() throws Exception;
	
	/**
	 * <p>Returns a <u>borrowed</u> object back to the pool. After returning it the pool will regain control over
	 * the object's lifecycle so you should not reference it down the line without requesting a new object again.</p>
	 * If the method raises exception it is unlikely calling the method again with the same object will succeed.
	 * 
	 * @param object <u>Previously borrowed</u> object from the pool.
	 * @throws Exception Throws exception in case of fatal failure during returning object.
	 */
	public void returnObject(T object) throws Exception;
	
	/**
	 * Removes all idle objects from the pool and could trigger creation of
	 * new objects depending on the configuration.
	 */
	public void clear();
	
	/**
	 * <p>Removes all idle objects from the pool and will close the pool. All objects returned after closure will
	 * be automatically removed as well. Objects which was borrowed before closure will continue to work
	 * as usual till returning to the pool.</p>
	 * <p>The pool can be again started by calling {@link #create()}.</p>
	 */
	public void close();
	
	/**
	 * <p>Returns state of the pool. If the method returns false you should <u>not</u> use the pool without
	 * starting it again.</p>
	 * <p>The pool can throw exception while trying to use when closed.</p>
	 * 
	 * @return Pool state, if {@code false} the pool is not ready to be used.
	 */
	public boolean isClosed();
}
