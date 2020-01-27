package com.jkubinyi.simplepool;

// TODO: min/max idlePoolSize check if needed
public class PoolConfiguration {

	private int initialPoolSize;
	private int maxWaitInSec;
	private int maxPoolSize;
	private int maxPoolIdleSize;
	private int minPoolIdleSize;
	private int maxObjectIdleTime;
	private boolean prefersLiFo;
	private boolean autostart;

	/**
	 * @param initialPoolSize
	 * @param maxWaitInSec
	 * @param maxPoolSize
	 * @param maxPoolIdleSize
	 * @param minPoolIdleSize
	 * @param maxObjectIdleTime
	 * @param prefersLiFo
	 */
	private PoolConfiguration(int initialPoolSize, int maxWaitInSec, int maxPoolSize, int maxPoolIdleSize,
			int minPoolIdleSize, int maxObjectIdleTime, boolean prefersLiFo, boolean autostart) {
		super();
		this.initialPoolSize = initialPoolSize;
		
		if(maxPoolSize < initialPoolSize) this.maxPoolSize = initialPoolSize;
		else this.maxPoolSize = maxPoolSize;
		
		if(maxPoolIdleSize < initialPoolSize) this.maxPoolIdleSize = initialPoolSize;
		else this.maxPoolIdleSize = maxPoolIdleSize;
		
		this.maxWaitInSec = maxWaitInSec;
		this.minPoolIdleSize = minPoolIdleSize;
		this.maxObjectIdleTime = maxObjectIdleTime;
		this.prefersLiFo = prefersLiFo;
		this.autostart = autostart;
	}

	public int getInitialPoolSize() {
		return initialPoolSize;
	}

	public int getMaxPoolIdleSize() {
		return maxPoolIdleSize;
	}

	public int getMinPoolIdleSize() {
		return minPoolIdleSize;
	}

	public int getMaxObjectIdleTime() {
		return maxObjectIdleTime;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public int getMaxWaitInSec() {
		return maxWaitInSec;
	}

	public void setMaxWaitInSec(int maxWaitInSec) {
		this.maxWaitInSec = maxWaitInSec;
	}

	public void setInitialPoolSize(int initialPoolSize) {
		if(this.maxPoolSize < initialPoolSize) this.maxPoolSize = initialPoolSize;
		if(this.maxPoolIdleSize < initialPoolSize) this.maxPoolIdleSize = initialPoolSize;
		this.initialPoolSize = initialPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public void setMaxPoolIdleSize(int maxPoolIdleSize) {
		this.maxPoolIdleSize = maxPoolIdleSize;
	}

	public void setMinPoolIdleSize(int minPoolIdleSize) {
		this.minPoolIdleSize = minPoolIdleSize;
	}

	public void setMaxObjectIdleTime(int maxObjectIdleTime) {
		this.maxObjectIdleTime = maxObjectIdleTime;
	}

	public boolean prefersLiFo() {
		return prefersLiFo;
	}

	public void setPrefersLiFo(boolean prefersLiFo) {
		this.prefersLiFo = prefersLiFo;
	}
	
	public boolean shouldAutostart() {
		return autostart;
	}

	public static class Builder {

		private int initialPoolSize = 1;
		private int maxWaitInSec = 10;
		private int maxPoolSize = 1;
		private int maxPoolIdleSize = 1;
		private int minPoolIdleSize = 1;
		private int maxObjectIdleTime = 100;
		private boolean prefersLiFo = false;
		private boolean autostart = true;
		
		public Builder setInitialPoolSize(int initialPoolSize) {
			this.initialPoolSize = initialPoolSize;
			return this;
		}
		public Builder setMaxWaitInSec(int maxWaitInSec) {
			this.maxWaitInSec = maxWaitInSec;
			return this;
		}
		public Builder setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
			return this;
		}
		public Builder setMaxPoolIdleSize(int maxPoolIdleSize) {
			this.maxPoolIdleSize = maxPoolIdleSize;
			return this;
		}
		public Builder setMinPoolIdleSize(int minPoolIdleSize) {
			this.minPoolIdleSize = minPoolIdleSize;
			return this;
		}
		public Builder setMaxObjectIdleTime(int maxObjectIdleTime) {
			this.maxObjectIdleTime = maxObjectIdleTime;
			return this;
		}
		public Builder setPrefersLiFo(boolean prefersLiFo) {
			this.prefersLiFo = prefersLiFo;
			return this;
		}
		public Builder setAutostart(boolean autostart) {
			this.autostart = autostart;
			return this;
		}
		public PoolConfiguration build() {
			return new PoolConfiguration(initialPoolSize, maxWaitInSec, maxPoolSize, maxPoolIdleSize,
					minPoolIdleSize, maxObjectIdleTime, prefersLiFo, autostart);
		}
	}
}
