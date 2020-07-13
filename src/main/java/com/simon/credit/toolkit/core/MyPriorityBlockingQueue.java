package com.simon.credit.toolkit.core;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.simon.credit.toolkit.concurrent.UnsafeToolkits;
import com.simon.credit.toolkit.lang.MyStringBuilder;

@SuppressWarnings({ "restriction", "rawtypes" })
public class MyPriorityBlockingQueue<E> extends MyAbstractQueue<E> implements MyBlockingQueue<E>, Serializable {
	private static final long serialVersionUID = 5595510919245408276L;

	private static final int DEFAULT_INITIAL_CAPACITY = 11;

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private transient Object[] queue;

	private transient int size;

	private transient Comparator<? super E> comparator;

	private final ReentrantLock lock;

	private final Condition notEmpty;

	private transient volatile int allocationSpinLock;

	private PriorityQueue q;

	public MyPriorityBlockingQueue() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}

	public MyPriorityBlockingQueue(int initialCapacity) {
		this(initialCapacity, null);
	}

	public MyPriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException();
		}
		this.lock = new ReentrantLock();
		this.notEmpty = lock.newCondition();
		this.comparator = comparator;
		this.queue = new Object[initialCapacity];
	}

	@SuppressWarnings("unchecked")
	public MyPriorityBlockingQueue(Collection<? extends E> c) {
		this.lock = new ReentrantLock();
		this.notEmpty = lock.newCondition();
		boolean heapify = true; // true if not known to be in heap order
		boolean screen = true; // true if must screen for nulls
		if (c instanceof SortedSet<?>) {
			SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
			this.comparator = (Comparator<? super E>) ss.comparator();
			heapify = false;
		} else if (c instanceof MyPriorityBlockingQueue<?>) {
			MyPriorityBlockingQueue<? extends E> pq = (MyPriorityBlockingQueue<? extends E>) c;
			this.comparator = (Comparator<? super E>) pq.comparator();
			screen = false;
			if (pq.getClass() == MyPriorityBlockingQueue.class) {// exact match
				heapify = false;
			}
		}
		Object[] a = c.toArray();
		int n = a.length;
		// If c.toArray incorrectly doesn't return Object[], copy it.
		if (a.getClass() != Object[].class) {
			a = Arrays.copyOf(a, n, Object[].class);
		}
		if (screen && (n == 1 || this.comparator != null)) {
			for (int i = 0; i < n; ++i) {
				if (a[i] == null) {
					throw new NullPointerException();
				}
			}
		}
		this.queue = a;
		this.size = n;
		if (heapify) {
			heapify();
		}
	}

	private void tryGrow(Object[] array, int oldCap) {
		lock.unlock(); // must release and then re-acquire main lock
		Object[] newArray = null;
		if (allocationSpinLock == 0 && UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset, 0, 1)) {
			try {
				int newCap = oldCap + ((oldCap < 64) ? (oldCap + 2) : (oldCap >> 1));
				if (newCap - MAX_ARRAY_SIZE > 0) { // possible overflow
					int minCap = oldCap + 1;
					if (minCap < 0 || minCap > MAX_ARRAY_SIZE) {
						throw new OutOfMemoryError();
					}
					newCap = MAX_ARRAY_SIZE;
				}
				if (newCap > oldCap && queue == array) {
					newArray = new Object[newCap];
				}
			} finally {
				allocationSpinLock = 0;
			}
		}
		if (newArray == null) // back off if another thread is allocating
			Thread.yield();
		lock.lock();
		if (newArray != null && queue == array) {
			queue = newArray;
			System.arraycopy(array, 0, newArray, 0, oldCap);
		}
	}

	@SuppressWarnings("unchecked")
	private E extract() {
		E result;
		int n = size - 1;
		if (n < 0) {
			result = null;
		} else {
			Object[] array = queue;
			result = (E) array[0];
			E x = (E) array[n];
			array[n] = null;
			Comparator<? super E> cmp = comparator;
			if (cmp == null) {
				siftDownComparable(0, x, array, n);
			} else {
				siftDownUsingComparator(0, x, array, n, cmp);
			}
			size = n;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T> void siftUpComparable(int k, T x, Object[] array) {
		Comparable<? super T> key = (Comparable<? super T>) x;
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Object e = array[parent];
			if (key.compareTo((T) e) >= 0) {
				break;
			}
			array[k] = e;
			k = parent;
		}
		array[k] = key;
	}

	@SuppressWarnings("unchecked")
	private static <T> void siftUpUsingComparator(int k, T x, Object[] array, Comparator<? super T> cmp) {
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Object e = array[parent];
			if (cmp.compare(x, (T) e) >= 0) {
				break;
			}
			array[k] = e;
			k = parent;
		}
		array[k] = x;
	}

	@SuppressWarnings("unchecked")
	private static <T> void siftDownComparable(int k, T x, Object[] array, int n) {
		Comparable<? super T> key = (Comparable<? super T>) x;
		int half = n >>> 1; // loop while a non-leaf
		while (k < half) {
			int child = (k << 1) + 1; // assume left child is least
			Object c = array[child];
			int right = child + 1;
			if (right < n && ((Comparable<? super T>) c).compareTo((T) array[right]) > 0) {
				c = array[child = right];
			}
			if (key.compareTo((T) c) <= 0) {
				break;
			}
			array[k] = c;
			k = child;
		}
		array[k] = key;
	}

	@SuppressWarnings("unchecked")
	private static <T> void siftDownUsingComparator(int k, T x, Object[] array, int n, Comparator<? super T> cmp) {
		int half = n >>> 1;
		while (k < half) {
			int child = (k << 1) + 1;
			Object c = array[child];
			int right = child + 1;
			if (right < n && cmp.compare((T) c, (T) array[right]) > 0) {
				c = array[child = right];
			}
			if (cmp.compare(x, (T) c) <= 0) {
				break;
			}
			array[k] = c;
			k = child;
		}
		array[k] = x;
	}

	@SuppressWarnings("unchecked")
	private void heapify() {
		Object[] array = queue;
		int n = size;
		int half = (n >>> 1) - 1;
		Comparator<? super E> cmp = comparator;
		if (cmp == null) {
			for (int i = half; i >= 0; i--) {
				siftDownComparable(i, (E) array[i], array, n);
			}
		} else {
			for (int i = half; i >= 0; i--) {
				siftDownUsingComparator(i, (E) array[i], array, n, cmp);
			}
		}
	}

	public boolean add(E e) {
		return offer(e);
	}

	public boolean offer(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		int n, cap;
		Object[] array;
		while ((n = size) >= (cap = (array = queue).length)) {
			tryGrow(array, cap);
		}
		try {
			Comparator<? super E> cmp = comparator;
			if (cmp == null) {
				siftUpComparable(n, e, array);
			} else {
				siftUpUsingComparator(n, e, array, cmp);
			}
			size = n + 1;
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
		return true;
	}

	public void put(E e) {
		offer(e); // never need to block
	}

	public boolean offer(E e, long timeout, TimeUnit unit) {
		return offer(e); // never need to block
	}

	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		E result;
		try {
			result = extract();
		} finally {
			lock.unlock();
		}
		return result;
	}

	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ((result = extract()) == null) {
				notEmpty.await();
			}
		} finally {
			lock.unlock();
		}
		return result;
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ((result = extract()) == null && nanos > 0) {
				nanos = notEmpty.awaitNanos(nanos);
			}
		} finally {
			lock.unlock();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		E result;
		try {
			result = size > 0 ? (E) queue[0] : null;
		} finally {
			lock.unlock();
		}
		return result;
	}

	public Comparator<? super E> comparator() {
		return comparator;
	}

	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return size;
		} finally {
			lock.unlock();
		}
	}

	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	private int indexOf(Object o) {
		if (o != null) {
			Object[] array = queue;
			int n = size;
			for (int i = 0; i < n; i++) {
				if (o.equals(array[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private void removeAt(int i) {
		Object[] array = queue;
		int n = size - 1;
		if (n == i) {// removed last element
			array[i] = null;
		} else {
			E moved = (E) array[n];
			array[n] = null;
			Comparator<? super E> cmp = comparator;
			if (cmp == null) {
				siftDownComparable(i, moved, array, n);
			} else {
				siftDownUsingComparator(i, moved, array, n, cmp);
			}
			if (array[i] == moved) {
				if (cmp == null) {
					siftUpComparable(i, moved, array);
				} else {
					siftUpUsingComparator(i, moved, array, cmp);
				}
			}
		}
		size = n;
	}

	public boolean remove(Object o) {
		boolean removed = false;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int i = indexOf(o);
			if (i != -1) {
				removeAt(i);
				removed = true;
			}
		} finally {
			lock.unlock();
		}
		return removed;
	}

	private void removeEQ(Object o) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] array = queue;
			int n = size;
			for (int i = 0; i < n; i++) {
				if (o == array[i]) {
					removeAt(i);
					break;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public boolean contains(Object o) {
		int index;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			index = indexOf(o);
		} finally {
			lock.unlock();
		}
		return index != -1;
	}

	public Object[] toArray() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return Arrays.copyOf(queue, size);
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public String toString() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = size;
			if (n == 0) {
				return "[]";
			}
			MyStringBuilder builder = new MyStringBuilder();
			builder.append('[');
			for (int i = 0; i < n; ++i) {
				E e = (E) queue[i];
				builder.append(e == this ? "(this Collection)" : e);
				if (i != n - 1) {
					builder.append(',').append(' ');
				}
			}
			return builder.append(']').toString();
		} finally {
			lock.unlock();
		}
	}

	public int drainTo(Collection<? super E> c) {
		if (c == null) {
			throw new NullPointerException();
		}
		if (c == this) {
			throw new IllegalArgumentException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = 0;
			E e;
			while ((e = extract()) != null) {
				c.add(e);
				++n;
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	@Override
    public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null) {
			throw new NullPointerException();
		}
		if (c == this) {
			throw new IllegalArgumentException();
		}
		if (maxElements <= 0) {
			return 0;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = 0;
			E e;
			while (n < maxElements && (e = extract()) != null) {
				c.add(e);
				++n;
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			Object[] array = queue;
			int n = size;
			size = 0;
			for (int i = 0; i < n; i++) {
				array[i] = null;
			}
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = size;
			if (a.length < n) {
				// Make a new array of a's runtime type, but my contents:
				return (T[]) Arrays.copyOf(queue, size, a.getClass());
			}
			System.arraycopy(queue, 0, a, 0, n);
			if (a.length > n) {
				a[n] = null;
			}
			return a;
		} finally {
			lock.unlock();
		}
	}

	public Iterator<E> iterator() {
		return new Itr(toArray());
	}

	final class Itr implements Iterator<E> {
		final Object[] array; // Array of all elements
		int cursor; // index of next element to return;
		int lastRet; // index of last element, or -1 if no such

		Itr(Object[] array) {
			lastRet = -1;
			this.array = array;
		}

		public boolean hasNext() {
			return cursor < array.length;
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (cursor >= array.length) {
				throw new NoSuchElementException();
			}
			lastRet = cursor;
			return (E) array[cursor++];
		}

		public void remove() {
			if (lastRet < 0) {
				throw new IllegalStateException();
			}
			removeEQ(array[lastRet]);
			lastRet = -1;
		}
	}

	@SuppressWarnings("unchecked")
	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
		lock.lock();
		try {
			int n = size; // avoid zero capacity argument
			q = new PriorityQueue<E>(n == 0 ? 1 : n, comparator);
			q.addAll(this);
			s.defaultWriteObject();
		} finally {
			q = null;
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		try {
			s.defaultReadObject();
			this.queue = new Object[q.size()];
			comparator = q.comparator();
			addAll(q);
		} finally {
			q = null;
		}
	}

	// Unsafe mechanics
	private static final sun.misc.Unsafe UNSAFE;
	private static final long allocationSpinLockOffset;
	static {
		try {
			UNSAFE = UnsafeToolkits.getUnsafe();
			Class clazz = MyPriorityBlockingQueue.class;
			allocationSpinLockOffset = UNSAFE.objectFieldOffset(clazz.getDeclaredField("allocationSpinLock"));
		} catch (Exception e) {
			throw new Error(e);
		}
	}

}