package com.jkubinyi.simplepool;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.jkubinyi.simplepool.common.PoolEventHandler.Severity;
import com.jkubinyi.simplepool.misc.InterruptibleLinkedBlockingDeque;

public class GenericPoolImpl<T> implements GenericPool<T> {

	/** Flag whether pool has been initialzed and is prepared to be used. **/
	private AtomicBoolean prepared = new AtomicBoolean(false);

	/** 
	 * Counter how many objects were created by the pool. All objects except
	 * ones that did throw exception during producing are counted.
	 **/
	private AtomicLong createdObjects = new AtomicLong(0);

	/** 
	 * Counter how many objects were destroyed by the pool. All objects even
	 * those which threw exception are counted.
	 **/
	private AtomicLong destroyedObjects = new AtomicLong(0);

	/** Map used to store references to all objects currently known by the pool. **/
	private final Map<ObjectId<T>, PoolObject<T>> allObjects = new ConcurrentHashMap<>();

	/** Lock object to synchronize during creating objects **/
	private final Object objectCreationLock = new Object();

	/** Holds all idle objects prepared and waiting to be used **/
	protected final InterruptibleLinkedBlockingDeque<PoolObject<T>> idleObjects = new InterruptibleLinkedBlockingDeque<>();

	/** Configuration which is used by the pool. **/
	protected final PoolConfiguration config;

	/** Reference to the factory used by the pool. */
	protected final ObjectPoolFactory<T> factory;

	public GenericPoolImpl(ObjectPoolFactory<T> factory, PoolConfiguration config) {
		this.factory = factory;
		this.config = config;

		if(this.config.shouldAutostart()) this.create();
	}

	public int getNumActive() {
		return this.allObjects.size() - this.idleObjects.size();
	}

	public int getNumIdle() {
		return this.idleObjects.size();
	}

	public long getNumCreated() {
		return this.createdObjects.get();
	}

	public long getNumDestroyed() {
		return this.destroyedObjects.get();
	}

	// Main methods API - create, borrow, return and destroy
	/**
	 * <p>Used to start the object pool ensuring minimum number of objects are available at the disposal.</p>
	 * <b>Blocking operation</b>. Should be ideally called once per application
	 * lifecycle (at the startup, change of application configuration, etc.).
	 */
	public void create() {
		if(this.prepared.compareAndSet(false, true)) {
			int toCreate = this.config.getInitialPoolSize();
			boolean prefersLiFo = this.config.prefersLiFo();
			synchronized(this.objectCreationLock) {
				for (int i = 0; i < toCreate; i++) {
					final PoolObject<T> object;
					try {
						object = this.factory.produceObject(this);
						this.createdObjects.incrementAndGet();
						if(this.factory.validateObject(object)) {
							this.allObjects.put(new ObjectId<>(object.getObject()), object);
							if(prefersLiFo) this.idleObjects.addFirst(object);
							else this.idleObjects.addLast(object);
						}
					} catch(Exception e) {
						this.caughtException(e);
					}
				}
			}
		}
	}

	public T borrowObject() throws Exception {
		return borrowObject(this.config.getMaxWaitInSec());
	}

	public void returnObject(T object) throws Exception {
		final PoolObject<T> newObject = allObjects.get(new ObjectId<>(object));
		int maxIdleSize = this.config.getMaxPoolIdleSize();
		boolean prefersLiFo = this.config.prefersLiFo();

		if(newObject == null) 
			throw new IllegalStateException("Returned object was not created by this pool.");

		synchronized(object) {
			this.markPoolObjectReturned(newObject);
			try {
				this.factory.sleepObject(newObject);
			} catch(final Exception e) {
				this.newEvent(Severity.error, "Exception during sleeping object {}: ", newObject, e);
				try {
					this.destroy(newObject);
				} catch(final Exception ee) {
					this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", newObject);
				}
				try {
					this.checkForMinimumIdles();
				} catch(final Exception ee) {
					this.newEvent(Severity.error, "Pool could not autocreate minimum objects: ", ee);
				}
				return;
			}

			if(!newObject.deallocate())
				throw new IllegalStateException("Object has already been returned to the pool.");

			if(this.isClosed() || (maxIdleSize > -1 && maxIdleSize <= this.idleObjects.size())) {
				try {
					this.destroy(newObject);
				} catch(final Exception e) {
					this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", newObject);
				}
				try {
					this.checkForMinimumIdles();
				} catch(final Exception e) {
					this.newEvent(Severity.error, "Pool could not autocreate minimum objects: ", e);
				}
			} else {
				if(prefersLiFo) this.idleObjects.addFirst(newObject);
				else this.idleObjects.addLast(newObject);
				this.newEvent(Severity.info, "Object {} returned back to the idle objects.", newObject);

				if(this.isClosed()) this.clear();
			}
		}
	}

	protected void markPoolObjectReturned(final PoolObject<T> object) {
		if(!object.returned())
			throw new IllegalStateException("Object has already been returned or is invalid.");
	}

	private T borrowObject(int maxWaitTime) throws Exception {
		if(this.isClosed())
			throw new IllegalStateException("Pool is closed.");

		PoolObject<T> object = null;
		boolean createdObject = false;

		while(object == null) {
			object = this.idleObjects.pollFirst();
			if(object == null) { // We don't have any available idle object
				object = this.createOneObjectInPool();
				if(object != null) {
					createdObject = true;
				} else { // Could not get idle object and create a new one -> Let's give it time
					if(maxWaitTime < 0) { // A negative number -> block on queue indefinitely till has some idle object or interrupted
						object = this.idleObjects.take();
					} else { // We will wait only maximum defined time
						object = this.idleObjects.pollFirst(maxWaitTime, TimeUnit.SECONDS);
						if(object == null)
							throw new NoSuchElementException("Timeout during waiting for idle object.");
					}
				}
			}

			if(object == null)
				throw new NoSuchElementException("Cannot obtain object from the pool.");
			else {
				boolean valid = false;
				try {
					valid = this.factory.validateObject(object);
				} catch(final Exception e) {
					this.newEvent(Severity.warn, "Object {} could not be validated and was destroyed.", object);
					try {
						this.destroy(object);
					} catch(final Exception ee) {
						this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", object);
					}
					try {
						this.checkForMinimumIdles();
					} catch(final Exception ee) {
						this.newEvent(Severity.error, "Pool could not autocreate minimum objects: ", ee);
					}
					object = null;
					if(createdObject) {
						final Exception issExc = new IllegalStateException("Unable to validate object.");
						issExc.initCause(e);
						throw issExc;
					}
				}
				
				if(!valid && object != null) { // Object exists, but validation failed
					try {
						this.destroy(object);
					} catch(final Exception ee) {
						this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", object);
					}
					try {
						this.checkForMinimumIdles();
					} catch(final Exception ee) {
						this.newEvent(Severity.error, "Pool could not autocreate minimum objects: ", ee);
					}
					object = null;
					if(createdObject) {
						final Exception issExc = new IllegalStateException("Unable to validate object. (Validation not successful)");
						throw issExc;
					}
				} else if(valid) {
					object.allocate();
					
					try {
						this.factory.activateObject(object);
					} catch(final Exception e) {
						try {
							this.destroy(object);
						} catch(final Exception e1) {
							this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", object);
						}
						try {
							this.checkForMinimumIdles();
						} catch(final Exception ee) {
							this.newEvent(Severity.error, "Pool could not autocreate minimum objects: ", ee);
						}
						
						if(createdObject) {
							final Exception issExc = new IllegalStateException("Unable to activate object.");
							issExc.initCause(e);
							throw issExc;
						} else {
							this.newEvent(Severity.warn, "Object {} could not be activated and was destroyed.", object);
						}
						object = null;
					}
				}
			}
		}
		
		this.newEvent(Severity.info, "Object {} borrowed from the pool.", object);
		return object.getObject();
	}

	private void destroy(final PoolObject<T> object) throws Exception {
		object.invalidate();
		this.idleObjects.remove(object);
		this.allObjects.remove(new ObjectId<T>(object.getObject()));
		try {
			this.factory.destroyObject(object);
		} finally {
			this.newEvent(Severity.info, "Object {} was destroyed.", object);
			this.destroyedObjects.incrementAndGet();
		}
	}

	private void checkForMinimumIdles() throws Exception {
		if(this.isClosed())
			throw new IllegalStateException("Pool is closed.");

		int minIdle = this.config.getMinPoolIdleSize();
		boolean prefersLiFo = this.config.prefersLiFo();

		if(this.idleObjects.size() >= minIdle) {
			return;
		}

		while(this.idleObjects.size() < minIdle) {
			final PoolObject<T> object = this.createOneObjectInPool();
			if(object == null) break;

			if(prefersLiFo) this.idleObjects.addFirst(object);
			else this.idleObjects.addLast(object);
		}

		if(this.isClosed()) { // Pool has closed in the meantime
			this.clear(false); // Clean it or memory leak can occur
		}
	}

	public void clear() {
		this.clear(true);
	}

	@Override
	public void close() {
		this.close(true);
	}

	public final boolean isClosed() {
		return !this.prepared.get();
	}

	protected void close(boolean clear) {
		if(this.prepared.compareAndSet(true, false)) {
			if(clear) this.clear(false);
			this.idleObjects.interruptWaitingOnTake();
		}
	}

	protected void clear(boolean restart) {
		PoolObject<T> object = this.idleObjects.poll();

		while(object != null) {
			try {
				this.destroy(object);
			} catch(final Exception e) {
				this.newEvent(Severity.warn, "Object {} could not be destroyed. (Already destroyed?)", object);
			}
			object = this.idleObjects.poll();
		}

		if(restart) {
			try {
				this.checkForMinimumIdles();
			} catch(Exception e) {
				this.close(true); // Will call this method again but without restarting
				IllegalStateException iss = new IllegalStateException("Exception occured during recreation of objects.");
				iss.initCause(e);
				throw iss;
			}
		}
	}

	private synchronized PoolObject<T> createOneObjectInPool() {
		if(this.isClosed())
			throw new IllegalStateException("Pool is closed.");

		if(this.allObjects.size() >= this.config.getMaxPoolSize()) // Check for maximum objects
			return null;

		try {
			PoolObject<T> newObject = this.factory.produceObject(this);
			this.createdObjects.incrementAndGet();
			this.allObjects.put(new ObjectId<>(newObject.getObject()), newObject);
			return newObject;
		} catch(Exception e) {
			this.caughtException(e);
		}

		return null;

	}
	
	private void caughtException(Exception e) {
		this.newEvent(Severity.error, "Exception during execution: ", e);
	}
	
	private void newEvent(Severity severity, String format, Object... objects) {
		this.config.getEventHandler().newEvent(severity, format, objects);
	}

	/**
	 * Class is used to uniquely distinguish between objects for safe storage inside pool. Has unique
	 * hashcode depending on the underlying object to maintain proper pool object management.
	 * 
	 * @author jurajkubinyi
	 *
	 * @param <T>
	 */
	static class ObjectId<T> {
		private final T object;

		/**
		 * Creates the ObjectId of the object for the pool.
		 * @param object
		 */
		private ObjectId(final T object) {
			this.object = object;
		}

		@Override
		public int hashCode() {
			/* Uses default implementation of the hashcode generation for the object.
			 * Rogue object can have a hashCode method overriden and improperly implemented thus
			 * causing pool management issues.
			 */
			return System.identityHashCode(this.object);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public boolean equals(final Object obj) {
			return  obj instanceof ObjectId &&
					((ObjectId)obj).object == object;
		}

		/**
		 * @return the wrapped object
		 */
		public T getObject() {
			return this.object;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("ObjectId [object: ");
			builder.append(this.object);
			builder.append("]");
			return builder.toString();
		}
	}
}
