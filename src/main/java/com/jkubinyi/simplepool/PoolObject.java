package com.jkubinyi.simplepool;

public final class PoolObject<T> {
	
	enum ObjectState {
		IDLE,
		ALLOCATED,
		RETURNED,
		INVALID
	}

    private final T object;
    private ObjectState state = ObjectState.IDLE;
    private final long creationTime = System.currentTimeMillis();
    
    /**
     * Creates a wrapper instance of the object for the pool.
     * @param object Object which will be stored in pool.
     */
    public PoolObject(final T object) {
        this.object = object;
    }
    
    /**
     * @return Returns underlying wrapped object.
     */
    public T getObject() {
        return this.object;
    }
    
    /**
     * @return Returns creation timestamp in miliseconds.
     */
    public long getCreationTime() {
        return this.creationTime;
    }
    
    /**
     * @return Gets current state of the object in pool. Under normal condition
     * you should never need to check the state manually.
     */
    public synchronized ObjectState getState() {
        return this.state;
    }
    
    /**
     * Marks the object as currently under use by the {@link GenericPool}.
     * 
     * @return {@code true} if the previous state was {@link ObjectState#IDLE}
     */
    protected synchronized boolean allocate() {
        if (this.state == ObjectState.IDLE) {
            this.state = ObjectState.ALLOCATED;
            return true;
        }
        return false;
    }

    /**
     * Marks the object as unused by the pool to make it available for later reuse
     * by the {@link GenericPool}.
     *
     * @return {@code true} if the previous state was {@link ObjectState#ALLOCATED} or {@link ObjectState#RETURNED}
     */
    protected synchronized boolean deallocate() {
        if (this.state == ObjectState.RETURNED || this.state == ObjectState.ALLOCATED) {
            this.state = ObjectState.IDLE;
            return true;
        }
        return false;
    }

    /**
     * Marks the object as invalid making it not reusable.
     */
    protected synchronized void invalidate() {
        this.state = ObjectState.INVALID;
    }

    /**
     * Marks the object as returned to the pool but not directly available to be reused.
     */
    protected synchronized boolean returned() {
        if(this.state == ObjectState.ALLOCATED) {
        	this.state = ObjectState.RETURNED;
        	return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("[wrapped: ");
        result.append(object.toString());
        result.append("; state: ");
        synchronized (this) {
            result.append(state.toString());
        }
        result.append("; created: ");
        result.append(this.creationTime);
        result.append("]");
        return result.toString();
    }
}