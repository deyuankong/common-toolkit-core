package com.simon.credit.toolkit.core;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class MyLinkedHashMap<K, V> extends MyHashMap<K, V> implements Map<K, V> {
	private static final long serialVersionUID = 3801124242820219131L;

	private transient Entry<K, V> header;

	private final boolean accessOrder;

	public MyLinkedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		accessOrder = false;
	}

	public MyLinkedHashMap(int initialCapacity) {
		super(initialCapacity);
		accessOrder = false;
	}

	public MyLinkedHashMap() {
		super();
		accessOrder = false;
	}

	public MyLinkedHashMap(Map<? extends K, ? extends V> m) {
		super(m);
		accessOrder = false;
	}

	public MyLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
		super(initialCapacity, loadFactor);
		this.accessOrder = accessOrder;
	}

	void init() {
		header = new Entry<>(-1, null, null, null);
		header.before = header.after = header;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void transfer(MyHashMap.Entry[] newTable) {
		int newCapacity = newTable.length;
		for (Entry<K, V> e = header.after; e != header; e = e.after) {
			int index = indexFor(e.hash, newCapacity);
			e.next = newTable[index];
			newTable[index] = e;
		}
	}

	@SuppressWarnings("rawtypes")
	public boolean containsValue(Object value) {
		// Overridden to take advantage of faster iterator
		if (value == null) {
			for (Entry e = header.after; e != header; e = e.after) {
				if (e.value == null) {
					return true;
				}
			}
		} else {
			for (Entry e = header.after; e != header; e = e.after) {
				if (value.equals(e.value)) {
					return true;
				}
			}
		}

		return false;
	}

	public V get(Object key) {
		Entry<K, V> e = (Entry<K, V>) getEntry(key);
		if (e == null) {
			return null;
		}
		e.recordAccess(this);
		return e.value;
	}

	public void clear() {
		super.clear();
		header.before = header.after = header;
	}

	private static class Entry<K, V> extends MyHashMap.Entry<K, V> {
		// These fields comprise the doubly linked list used for iteration.
		Entry<K, V> before, after;

		Entry(int hash, K key, V value, MyHashMap.Entry<K, V> next) {
			super(hash, key, value, next);
		}

		private void remove() {
			before.after = after;
			after.before = before;
		}

		private void addBefore(Entry<K, V> existingEntry) {
			after = existingEntry;
			before = existingEntry.before;
			before.after = this;
			after.before = this;
		}

		void recordAccess(MyHashMap<K, V> m) {
			MyLinkedHashMap<K, V> lm = (MyLinkedHashMap<K, V>) m;
			if (lm.accessOrder) {
				lm.modCount++;
				remove();
				addBefore(lm.header);
			}
		}

		@SuppressWarnings("unused")
		void recordRemoval(HashMap<K, V> m) {
			remove();
		}
	}

	private abstract class LinkedHashIterator<T> implements Iterator<T> {
		Entry<K, V> nextEntry = header.after;
		Entry<K, V> lastReturned = null;

		int expectedModCount = modCount;

		public boolean hasNext() {
			return nextEntry != header;
		}

		public void remove() {
			if (lastReturned == null) {
				throw new IllegalStateException();
			}

			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}

			MyLinkedHashMap.this.remove(lastReturned.key);
			lastReturned = null;
			expectedModCount = modCount;
		}

		Entry<K, V> nextEntry() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}

			if (nextEntry == header) {
				throw new NoSuchElementException();
			}

			Entry<K, V> e = lastReturned = nextEntry;
			nextEntry = e.after;
			return e;
		}
	}

	private class KeyIterator extends LinkedHashIterator<K> {
		public K next() {
			return nextEntry().getKey();
		}
	}

	private class ValueIterator extends LinkedHashIterator<V> {
		public V next() {
			return nextEntry().value;
		}
	}

	private class EntryIterator extends LinkedHashIterator<Map.Entry<K, V>> {
		public Map.Entry<K, V> next() {
			return nextEntry();
		}
	}

	Iterator<K> newKeyIterator() {
		return new KeyIterator();
	}

	Iterator<V> newValueIterator() {
		return new ValueIterator();
	}

	Iterator<Map.Entry<K, V>> newEntryIterator() {
		return new EntryIterator();
	}

	void addEntry(int hash, K key, V value, int bucketIndex) {
		createEntry(hash, key, value, bucketIndex);

		// Remove eldest entry if instructed, else grow capacity if appropriate
		Entry<K, V> eldest = header.after;
		if (removeEldestEntry(eldest)) {
			removeEntryForKey(eldest.key);
		} else {
			if (size >= threshold) {
				resize(2 * table.length);
			}
		}
	}

	void createEntry(int hash, K key, V value, int bucketIndex) {
		MyHashMap.Entry<K, V> old = table[bucketIndex];
		Entry<K, V> e = new Entry<>(hash, key, value, old);
		table[bucketIndex] = e;
		e.addBefore(header);
		size++;
	}

	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return false;
	}

}
