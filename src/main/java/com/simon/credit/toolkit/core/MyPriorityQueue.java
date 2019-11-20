package com.simon.credit.toolkit.core;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

public class MyPriorityQueue<E> extends MyAbstractQueue<E> implements Serializable {
	private static final long serialVersionUID = -7720805057305804111L;

	private static final int DEFAULT_INITIAL_CAPACITY = 11;

	private transient Object[] queue;

	private int size = 0;

	private final Comparator<? super E> comparator;

	private transient int modCount = 0;

	public MyPriorityQueue() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}

	public MyPriorityQueue(int initialCapacity) {
		this(initialCapacity, null);
	}

	public MyPriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
		// Note: This restriction of at least one is not actually needed, but continues for 1.5 compatibility
		if (initialCapacity < 1) {
			throw new IllegalArgumentException();
		}
		this.queue = new Object[initialCapacity];
		this.comparator = comparator;
	}

	@SuppressWarnings("unchecked")
	public MyPriorityQueue(Collection<? extends E> c) {
		if (c instanceof SortedSet<?>) {
			SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
			this.comparator = (Comparator<? super E>) ss.comparator();
			initElementsFromCollection(ss);
		} else if (c instanceof MyPriorityQueue<?>) {
			MyPriorityQueue<? extends E> pq = (MyPriorityQueue<? extends E>) c;
			this.comparator = (Comparator<? super E>) pq.comparator();
			initFromPriorityQueue(pq);
		} else {
			this.comparator = null;
			initFromCollection(c);
		}
	}

	@SuppressWarnings("unchecked")
	public MyPriorityQueue(MyPriorityQueue<? extends E> c) {
		this.comparator = (Comparator<? super E>) c.comparator();
		initFromPriorityQueue(c);
	}

	@SuppressWarnings("unchecked")
	public MyPriorityQueue(SortedSet<? extends E> c) {
		this.comparator = (Comparator<? super E>) c.comparator();
		initElementsFromCollection(c);
	}

	private void initFromPriorityQueue(MyPriorityQueue<? extends E> c) {
		if (c.getClass() == MyPriorityQueue.class) {
			this.queue = c.toArray();
			this.size = c.size();
		} else {
			initFromCollection(c);
		}
	}

	private void initElementsFromCollection(Collection<? extends E> c) {
		Object[] a = c.toArray();
		// If c.toArray incorrectly doesn't return Object[], copy it.
		if (a.getClass() != Object[].class) {
			a = Arrays.copyOf(a, a.length, Object[].class);
		}
		int len = a.length;
		if (len == 1 || this.comparator != null) {
			for (int i = 0; i < len; i++) {
				if (a[i] == null) {
					throw new NullPointerException();
				}
			}
		}
		this.queue = a;
		this.size = a.length;
	}

	private void initFromCollection(Collection<? extends E> c) {
		initElementsFromCollection(c);
		heapify();
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private void grow(int minCapacity) {
		int oldCapacity = queue.length;
		// Double size if small; else grow by 50%
		int newCapacity = oldCapacity + ((oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1));
		// overflow-conscious code
		if (newCapacity - MAX_ARRAY_SIZE > 0) {
			newCapacity = hugeCapacity(minCapacity);
		}
		queue = Arrays.copyOf(queue, newCapacity);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) {// overflow
			throw new OutOfMemoryError();
		}
		return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
	}

	public boolean add(E e) {
		return offer(e);
	}

	public boolean offer(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		modCount++;
		int i = size;
		if (i >= queue.length) {
			grow(i + 1);
		}
		size = i + 1;
		if (i == 0) {
			queue[0] = e;
		} else {
			siftUp(i, e);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public E peek() {
		if (size == 0) {
			return null;
		}
		return (E) queue[0];
	}

	private int indexOf(Object o) {
		if (o != null) {
			for (int i = 0; i < size; i++) {
				if (o.equals(queue[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	public boolean remove(Object o) {
		int i = indexOf(o);
		if (i == -1) {
			return false;
		} else {
			removeAt(i);
			return true;
		}
	}

	boolean removeEq(Object o) {
		for (int i = 0; i < size; i++) {
			if (o == queue[i]) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}

	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	public Object[] toArray() {
		return Arrays.copyOf(queue, size);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a.length < size) {
			// Make a new array of a's runtime type, but my contents:
			return (T[]) Arrays.copyOf(queue, size, a.getClass());
		}
		System.arraycopy(queue, 0, a, 0, size);
		if (a.length > size) {
			a[size] = null;
		}
		return a;
	}

	public Iterator<E> iterator() {
		return new Itr();
	}

	private final class Itr implements Iterator<E> {
		private int cursor = 0;

		private int lastRet = -1;

		private ArrayDeque<E> forgetMeNot = null;

		private E lastRetElt = null;

		private int expectedModCount = modCount;

		public boolean hasNext() {
			return cursor < size || (forgetMeNot != null && !forgetMeNot.isEmpty());
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
			if (cursor < size) {
				return (E) queue[lastRet = cursor++];
			}
			if (forgetMeNot != null) {
				lastRet = -1;
				lastRetElt = forgetMeNot.poll();
				if (lastRetElt != null) {
					return lastRetElt;
				}
			}
			throw new NoSuchElementException();
		}

		public void remove() {
			if (expectedModCount != modCount) {
				throw new ConcurrentModificationException();
			}
			if (lastRet != -1) {
				E moved = MyPriorityQueue.this.removeAt(lastRet);
				lastRet = -1;
				if (moved == null) {
					cursor--;
				} else {
					if (forgetMeNot == null) {
						forgetMeNot = new ArrayDeque<>();
					}
					forgetMeNot.add(moved);
				}
			} else if (lastRetElt != null) {
				MyPriorityQueue.this.removeEq(lastRetElt);
				lastRetElt = null;
			} else {
				throw new IllegalStateException();
			}
			expectedModCount = modCount;
		}
	}

	public int size() {
		return size;
	}

	public void clear() {
		modCount++;
		for (int i = 0; i < size; i++) {
			queue[i] = null;
		}
		size = 0;
	}

	public E poll() {
		if (size == 0) {
			return null;
		}
		int s = --size;
		modCount++;
		@SuppressWarnings("unchecked")
		E result = (E) queue[0];
		@SuppressWarnings("unchecked")
		E x = (E) queue[s];
		queue[s] = null;
		if (s != 0) {
			siftDown(0, x);
		}
		return result;
	}

	private E removeAt(int i) {
		assert i >= 0 && i < size;
		modCount++;
		int s = --size;
		if (s == i) {// removed last element
			queue[i] = null;
		} else {
			@SuppressWarnings("unchecked")
			E moved = (E) queue[s];
			queue[s] = null;
			siftDown(i, moved);
			if (queue[i] == moved) {
				siftUp(i, moved);
				if (queue[i] != moved) {
					return moved;
				}
			}
		}
		return null;
	}

	private void siftUp(int k, E x) {
		if (comparator != null) {
			siftUpUsingComparator(k, x);
		} else {
			siftUpComparable(k, x);
		}
	}

	@SuppressWarnings("unchecked")
	private void siftUpComparable(int k, E x) {
		Comparable<? super E> key = (Comparable<? super E>) x;
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Object e = queue[parent];
			if (key.compareTo((E) e) >= 0) {
				break;
			}
			queue[k] = e;
			k = parent;
		}
		queue[k] = key;
	}

	@SuppressWarnings("unchecked")
	private void siftUpUsingComparator(int k, E x) {
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Object e = queue[parent];
			if (comparator.compare(x, (E) e) >= 0) {
				break;
			}
			queue[k] = e;
			k = parent;
		}
		queue[k] = x;
	}

	private void siftDown(int k, E x) {
		if (comparator != null) {
			siftDownUsingComparator(k, x);
		} else {
			siftDownComparable(k, x);
		}
	}

	@SuppressWarnings("unchecked")
	private void siftDownComparable(int k, E x) {
		Comparable<? super E> key = (Comparable<? super E>) x;
		int half = size >>> 1; // loop while a non-leaf
		while (k < half) {
			int child = (k << 1) + 1; // assume left child is least
			Object c = queue[child];
			int right = child + 1;
			if (right < size && ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0) {
				c = queue[child = right];
			}
			if (key.compareTo((E) c) <= 0) {
				break;
			}
			queue[k] = c;
			k = child;
		}
		queue[k] = key;
	}

	@SuppressWarnings("unchecked")
	private void siftDownUsingComparator(int k, E x) {
		int half = size >>> 1;
		while (k < half) {
			int child = (k << 1) + 1;
			Object c = queue[child];
			int right = child + 1;
			if (right < size && comparator.compare((E) c, (E) queue[right]) > 0) {
				c = queue[child = right];
			}
			if (comparator.compare(x, (E) c) <= 0) {
				break;
			}
			queue[k] = c;
			k = child;
		}
		queue[k] = x;
	}

	@SuppressWarnings("unchecked")
	private void heapify() {
		for (int i = (size >>> 1) - 1; i >= 0; i--) {
			siftDown(i, (E) queue[i]);
		}
	}

	public Comparator<? super E> comparator() {
		return comparator;
	}

	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
		// Write out element count, and any hidden stuff
		s.defaultWriteObject();

		// Write out array length, for compatibility with 1.5 version
		s.writeInt(Math.max(2, size + 1));

		// Write out all elements in the "proper order".
		for (int i = 0; i < size; i++)
			s.writeObject(queue[i]);
	}

	private void readObject(ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		// Read in size, and any hidden stuff
		s.defaultReadObject();

		// Read in (and discard) array length
		s.readInt();

		queue = new Object[size];

		// Read in all elements.
		for (int i = 0; i < size; i++)
			queue[i] = s.readObject();

		// Elements are guaranteed to be in "proper order", but the
		// spec has never explained what that might be.
		heapify();
	}

}
