package com.jkubinyi.simplepool.misc;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;

public class InterruptibleLinkedBlockingDeque<E> extends LinkedBlockingDeque<E> {
	
    private static final long serialVersionUID = 1L;
    private volatile boolean interrupted = false;

    public InterruptibleLinkedBlockingDeque(int capacity) { 
    	super(capacity);
    }
    
    public InterruptibleLinkedBlockingDeque() {
    	super();
    }
    
    public InterruptibleLinkedBlockingDeque(Collection<? extends E> c) {
    	super(c);
    }

    public void interruptWaitingOnTake() {
    	this.interrupted = true;
    }

    public boolean isInterrupted() {
    	return this.interrupted;
    }
    
    /**
     * Blocks until it is populated or interrupted.
     */
    @Override
    public E take() throws InterruptedException {
        E res = null;
        while(!this.interrupted && (res = super.poll()) == null) {
            synchronized(this) {
                wait();
            }
        }
        return res;
    }
}
