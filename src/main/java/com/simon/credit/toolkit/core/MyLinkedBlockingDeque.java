package com.simon.credit.toolkit.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyLinkedBlockingDeque<E> extends MyAbstractQueue<E> implements BlockingDeque<E>, Serializable {
    private static final long serialVersionUID = -387911632671998426L;

    static final class Node<E> {
        E item;

        Node<E> prev;

        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    transient Node<E> first;

    transient Node<E> last;

    /** Number of items in the deque */
    private transient int count;

    /** Maximum number of items in the deque */
    private final int capacity;

    /** Main lock guarding all access */
    final ReentrantLock lock = new ReentrantLock();

    /** Condition for waiting takes */
    private final Condition notEmpty = lock.newCondition();

    /** Condition for waiting puts */
    private final Condition notFull = lock.newCondition();

    public MyLinkedBlockingDeque() {
        this(Integer.MAX_VALUE);
    }

    public MyLinkedBlockingDeque(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    public MyLinkedBlockingDeque(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock lock = this.lock;
        lock.lock(); // Never contended, but necessary for visibility
        try {
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (!linkLast(new Node<E>(e))) {
                    throw new IllegalStateException("Deque full");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Basic linking and unlinking operations, called only while holding lock

    private boolean linkFirst(Node<E> node) {
        // assert lock.isHeldByCurrentThread();
        if (count >= capacity) {
            return false;
        }
        Node<E> f = first;
        node.next = f;
        first = node;
        if (last == null) {
            last = node;
        } else {
            f.prev = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }

    private boolean linkLast(Node<E> node) {
        // assert lock.isHeldByCurrentThread();
        if (count >= capacity) {
            return false;
        }
        Node<E> l = last;
        node.prev = l;
        last = node;
        if (first == null) {
            first = node;
        } else {
            l.next = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }

    private E unlinkFirst() {
        // assert lock.isHeldByCurrentThread();
        Node<E> f = first;
        if (f == null) {
            return null;
        }
        Node<E> n = f.next;
        E item = f.item;
        f.item = null;
        f.next = f; // help GC
        first = n;
        if (n == null) {
            last = null;
        } else {
            n.prev = null;
        }
        --count;
        notFull.signal();
        return item;
    }

    private E unlinkLast() {
        // assert lock.isHeldByCurrentThread();
        Node<E> l = last;
        if (l == null) {
            return null;
        }
        Node<E> p = l.prev;
        E item = l.item;
        l.item = null;
        l.prev = l; // help GC
        last = p;
        if (p == null) {
            first = null;
        } else {
            p.next = null;
        }
        --count;
        notFull.signal();
        return item;
    }

    void unlink(Node<E> x) {
        // assert lock.isHeldByCurrentThread();
        Node<E> p = x.prev;
        Node<E> n = x.next;
        if (p == null) {
            unlinkFirst();
        } else if (n == null) {
            unlinkLast();
        } else {
            p.next = n;
            n.prev = p;
            x.item = null;
            // Don't mess with x's links.  They may still be in use by
            // an iterator.
            --count;
            notFull.signal();
        }
    }

    // BlockingDeque methods

    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public void addLast(E e) {
        if (!offerLast(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public boolean offerFirst(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkFirst(node);
        } finally {
            lock.unlock();
        }
    }

    public boolean offerLast(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkLast(node);
        } finally {
            lock.unlock();
        }
    }

    public void putFirst(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkFirst(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void putLast(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkLast(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean offerFirst(E e, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (e == null)  {
        	throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (!linkFirst(node)) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null)  {
        	throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (!linkLast(node)) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public E removeFirst() {
        E x = pollFirst();
        if (x == null) {
        	throw new NoSuchElementException();
        }
        return x;
    }

    public E removeLast() {
        E x = pollLast();
        if (x == null) {
        	throw new NoSuchElementException();
        }
        return x;
    }

    public E pollFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }

    public E pollLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkLast();
        } finally {
            lock.unlock();
        }
    }

    public E takeFirst() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ( (x = unlinkFirst()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E takeLast() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ( (x = unlinkLast()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E pollFirst(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while ( (x = unlinkFirst()) == null) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E pollLast(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while ( (x = unlinkLast()) == null) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E getFirst() {
        E x = peekFirst();
        if (x == null) throw new NoSuchElementException();
        return x;
    }

    public E getLast() {
        E x = peekLast();
        if (x == null) {
        	throw new NoSuchElementException();
        }
        return x;
    }

    public E peekFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (first == null) ? null : first.item;
        } finally {
            lock.unlock();
        }
    }

    public E peekLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (last == null) ? null : last.item;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeFirstOccurrence(Object o) {
        if (o == null) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeLastOccurrence(Object o) {
        if (o == null) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = last; p != null; p = p.prev) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // BlockingQueue methods

    public boolean add(E e) {
        addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return offerLast(e);
    }

    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offerLast(e, timeout, unit);
    }

    public E remove() {
        return removeFirst();
    }

    public E poll() {
        return pollFirst();
    }

    public E take() throws InterruptedException {
        return takeFirst();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return pollFirst(timeout, unit);
    }

    public E element() {
        return getFirst();
    }

    public E peek() {
        return peekFirst();
    }

    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return capacity - count;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            for (int i = 0; i < n; i++) {
                c.add(first.item);   // In this order, in case add() throws.
                unlinkFirst();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    // Stack methods

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    // Collection methods

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(Object o) {
        if (o == null) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[count];
            int k = 0;
            for (Node<E> p = first; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (a.length < count) {
				a = (T[]) Array.newInstance(a.getClass().getComponentType(), count);
            }

            int k = 0;
            for (Node<E> p = first; p != null; p = p.next) {
                a[k++] = (T)p.item;
            }
            if (a.length > k) {
                a[k] = null;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Node<E> p = first;
            if (p == null) {
                return "[]";
            }

            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (;;) {
                E e = p.item;
                builder.append(e == this ? "(this Collection)" : e);
                p = p.next;
                if (p == null) {
                    return builder.append(']').toString();
                }
                builder.append(',').append(' ');
            }
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> f = first; f != null; ) {
                f.item = null;
                Node<E> n = f.next;
                f.prev = null;
                f.next = null;
                f = n;
            }
            first = last = null;
            count = 0;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
         Node<E> next;

        E nextItem;

        private Node<E> lastRet;

        abstract Node<E> firstNode();
        abstract Node<E> nextNode(Node<E> n);

        AbstractItr() {
            // set to initial position
            final ReentrantLock lock = MyLinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                next = firstNode();
                nextItem = (next == null) ? null : next.item;
            } finally {
                lock.unlock();
            }
        }

        private Node<E> succ(Node<E> n) {
            // Chains of deleted nodes ending in null or self-links
            // are possible if multiple interior nodes are removed.
            for (;;) {
                Node<E> s = nextNode(n);
                if (s == null) {
                    return null;
                } else if (s.item != null) {
                    return s;
                } else if (s == n) {
                    return firstNode();
                } else {
                    n = s;
                }
            }
        }

        void advance() {
            final ReentrantLock lock = MyLinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                // assert next != null;
                next = succ(next);
                nextItem = (next == null) ? null : next.item;
            } finally {
                lock.unlock();
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public E next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            lastRet = next;
            E x = nextItem;
            advance();
            return x;
        }

        public void remove() {
            Node<E> n = lastRet;
            if (n == null) {
                throw new IllegalStateException();
            }
            lastRet = null;
            final ReentrantLock lock = MyLinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                if (n.item != null) {
                    unlink(n);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /** Forward iterator */
    private class Itr extends AbstractItr {
        Node<E> firstNode() { return first; }
        Node<E> nextNode(Node<E> n) { return n.next; }
    }

    /** Descending iterator */
    private class DescendingItr extends AbstractItr {
        Node<E> firstNode() { return last; }
        Node<E> nextNode(Node<E> n) { return n.prev; }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // Write out capacity and any hidden stuff
            oos.defaultWriteObject();
            // Write out all elements in the proper order.
            for (Node<E> p = first; p != null; p = p.next) {
                oos.writeObject(p.item);
            }
            // Use trailing null as sentinel
            oos.writeObject(null);
        } finally {
            lock.unlock();
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        count = 0;
        first = null;
        last = null;
        // Read in all elements and place in queue
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E)ois.readObject();
            if (item == null) {
                break;
            }
            add(item);
        }
    }

}