package com.jkubinyi.simplepool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Before;
import org.junit.Test;

import com.jkubinyi.simplepool.misc.InterruptibleLinkedBlockingDeque;

public class CorePoolTest {

	private PoolConfiguration config;
	private GenericPoolImpl<Object> pool;
	private LinkedBlockingDeque<Object> expectedObjectOrder = new LinkedBlockingDeque<>();
	private TestObjectPoolFactory objectFactory = new TestObjectPoolFactory();
	
	private static int INIT_POOL_SIZE = 5;
	private static boolean LiFo = false;
	
	@Before
	public void prepareTest() {
		this.config = new PoolConfiguration.Builder()
		.setInitialPoolSize(CorePoolTest.INIT_POOL_SIZE)
		.setMaxWaitInSec(2)
		.setPrefersLiFo(CorePoolTest.LiFo)
		.build();
		
		this.pool = new GenericPoolImpl<>(this.objectFactory, this.config);
	}
	
	@Test
	public void checkCorrectInitialPoolSize() {
		assertEquals(this.pool.getNumIdle(), this.config.getInitialPoolSize());
	}
	
	@Test
	public void checkBorrowAndDecrementIdle() throws Exception {
		Object fromPool = this.pool.borrowObject();
		assertNotNull(fromPool);
		assertEquals(this.config.getInitialPoolSize()-1, this.pool.getNumIdle());
		assertEquals(1, this.pool.getNumActive());
	}
	
	@Test
	public void checkBorrowAndReturn() throws Exception {
		Object fromPool = this.pool.borrowObject();
		assertNotNull(fromPool);
		assertEquals(this.config.getInitialPoolSize()-1, this.pool.getNumIdle());
		assertEquals(1, this.pool.getNumActive());
		this.pool.returnObject(fromPool);
		assertEquals(0, this.pool.getNumActive());
		assertEquals(this.pool.getNumIdle(), this.config.getInitialPoolSize());
	}
	
	@Test
	public void checkLiFoVSFiFoOrdering() {
		PoolObject<Object> object = this.pool.idleObjects.poll();
		Object local = this.expectedObjectOrder.poll();

        while (object != null) {
            assertEquals("Object " + object.getObject() + " does not equals to " + local, true, object.getObject().equals(local));
        	
            object = this.pool.idleObjects.poll();
            local = this.expectedObjectOrder.poll();
        }
	}
	
	@Test
	public void testForReturnOrder() throws Exception {
		if(this.config.prefersLiFo()) {
			Object object = this.pool.borrowObject();
			this.pool.returnObject(object);
			Object object2 = this.pool.borrowObject();
			Object object3 = this.pool.borrowObject();
			assertEquals(object, object2);
			assertNotEquals(object, object3);
		} else
			assertTrue("Skipping due to LiFo being false.", true);
	}
	
	@Test
	public void clearPool() {
		InterruptibleLinkedBlockingDeque<PoolObject<Object>> original = new InterruptibleLinkedBlockingDeque<PoolObject<Object>>(this.pool.idleObjects);
		this.pool.clear();
		assertEquals(false, this.pool.idleObjects.containsAll(original));
		assertEquals(this.config.getMinPoolIdleSize(), this.pool.getNumIdle());
	}
	
	@Test
	public void closePool() throws Exception {
		this.pool.close();
		assertEquals(0, this.pool.getNumIdle());
		
		try {
			this.pool.borrowObject();
			fail("Pool should not allow borrow object when closed.");
		} catch(IllegalStateException e) {
			assertTrue("Pool did not allow borrowing after closing.", true);
		}
	}
	
	@Test
	public void closePoolAndRecreate() {
		this.pool.close();
		assertEquals(0, this.pool.getNumIdle());
		this.pool.create();
		assertEquals(this.config.getInitialPoolSize(), this.pool.getNumIdle());
	}
	
	@Test
	public void returnAfterClosingPool() throws Exception {
		Object obj = this.pool.borrowObject();
		this.pool.close();
		this.pool.returnObject(obj);
	}
	
	class TestObjectPoolFactory implements ObjectPoolFactory<Object> {

		private Object first;
		
		@Override
		public PoolObject<Object> produceObject(GenericPool<Object> pool) {
			Object obj = new Object();
			if(this.first == null) this.first = obj;
			if(config.prefersLiFo()) expectedObjectOrder.addFirst(obj);
			else expectedObjectOrder.addLast(obj);
			return new PoolObject<Object>(obj);
		}

		@Override
		public void destroyObject(PoolObject<Object> object) throws Exception {
			//Nothing to cleanup
		}

		@Override
		public boolean validateObject(PoolObject<Object> object) {
			//Nothing to validate
			return true;
		}

		@Override
		public void activateObject(PoolObject<Object> object) throws Exception {
			expectedObjectOrder.remove(object.getObject());
		}

		@Override
		public void sleepObject(PoolObject<Object> object) throws Exception {
			if(config.prefersLiFo()) expectedObjectOrder.addFirst(object.getObject());
			else expectedObjectOrder.addLast(object.getObject());
		}
		
	}
}
