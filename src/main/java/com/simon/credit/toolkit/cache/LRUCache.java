package com.simon.credit.toolkit.cache;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.simon.credit.toolkit.concurrent.MyReentrantLock;
import com.simon.credit.toolkit.core.MyLinkedHashMap;

/**
 * LRU(Least Recently Used:最近最少使用)缓存
 * @author XUZIMING 2019-11-19
 */
public class LRUCache<K, V> extends MyLinkedHashMap<K, V> {
	private static final long serialVersionUID = -5167631809472116969L;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private static final int  DEFAULT_MAX_CAPACITY = 1000;

	private final Lock lock = new MyReentrantLock();
	private volatile int maxCapacity = DEFAULT_MAX_CAPACITY;

	public LRUCache() {
		this(DEFAULT_MAX_CAPACITY);
	}

	public LRUCache(int maxCapacity) {
		// 第3个参数设置为true ，代表linkedlist按访问顺序排序，可作为LRU缓存
		// 第3个参数设置为false，代表linkedlist按插入顺序排序，可作为FIFO缓存
		super(16, DEFAULT_LOAD_FACTOR, true);
		this.maxCapacity = maxCapacity;
	}

	/**
	 * 移除年龄最大的键值对
	 */
	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxCapacity;// 判断当前容量是否大于最大容量
	}

	@Override
	public boolean containsKey(Object key) {
		try {
			lock.lock();
			return super.containsKey(key);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public V get(Object key) {
		try {
			lock.lock();
			return super.get(key);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		try {
			lock.lock();
			return super.put(key, value);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public V remove(Object key) {
		try {
			lock.lock();
			return super.remove(key);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		try {
			lock.lock();
			return super.size();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		try {
			lock.lock();
			super.clear();
		} finally {
			lock.unlock();
		}
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

}