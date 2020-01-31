package com.jkubinyi.simplepool;

public interface ObjectPoolFactory<T> {

	/**
	 * Used to produce object to be used by the pool wrapped by {@link PoolObject} class instance.
	 * 
	 * @param pool {@link GenericPool} which is requesting to create a new object.
	 * @return Object wrapped by PoolObject
	 * @throws Exception Thrown if the object creation is not successful.
	 */
	PoolObject<T> produceObject(GenericPool<T> pool) throws Exception;
	
	/**
	 * <p>Called during destroying a reference to an instance of the object in the pool. Please keep
	 * in mind the object's state is not guaranteed. Please be prepared to handle errors as
	 * object can be in several states.</p>
	 * <p><b>Example usage:</b> close the open resources.</p>
	 * 
	 * @param object Object to be destroyed wrapped in {@link PoolObject}
	 * @throws Exception Unrecoverable exception thrown during validating object.
	 */
	void destroyObject(PoolObject<T> object) throws Exception;

	/**
	 * <p>Validates whether object could be reused by the pool again or should be discarded immediately.</p>
	 * <p><b>Example usage:</b> verify referencing resource is still reachable and opened, access to the
	 * resource is still possible, etc.</p>
	 * 
	 * @param object Object to be tested wrapped in {@link PoolObject}
	 * @return {@code false} if the object cannot be reused by the pool again
	 * @throws Exception Unrecoverable exception thrown during validating object.
	 */
	boolean validateObject(PoolObject<T> object) throws Exception;
	
	/**
	 * <p>Transforms object from idle to active state before returning it from the pool.</p>
	 * <p><b>Example usage:</b> individually track usage statistics of each independent object,
	 * claim ownership of resource, etc.</p>
	 * <b>Only unexpected, worst-case scenarios should throw Exception here. Often-goes-wrong
	 * conditions should be handled during {@link #validateObject(PoolObject)} life cycle.</b>
	 * 
	 * @param object Object to be returned by the poll wrapped in {@link PoolObject}
	 * @throws Exception Unrecoverable exception thrown during preparing object before returning.
	 */
	void activateObject(PoolObject<T> object) throws Exception;
	
	/**
	 * <p>Called before marking the object as idle.</p>
	 * <p><b>Example usage:</b> rollback changes, release shared resources etc.</p>
	 * <b>All shared resources should be released and/or closed in this phase. Objects can
	 * spend indefinite time as idle till other part of the code requests enough objects
	 * from the pool to release it.</b>
	 * 
	 * @param object Object to be marked as idle wrapped by {@link PoolObject}
	 * @throws Exception Unrecoverable exception thrown during preparing object to mark idle.
	 */
	 void sleepObject(PoolObject<T> object) throws Exception;
}
