package com.simon.credit.toolkit.concurrent;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 可重入锁测试
 * <pre>
 * 编写一个程序, 开启3个线程, 这三个线程的ID分别为A、B、C，每个线程将自己的ID在屏幕上打印20遍, 
 * 要求输出的结果必须按顺序显示, 如: ABBCCCABBCCCABBCCC…… 依次递归
 * </pre>
 */
public class MyReentrantLockTest {
	private static volatile boolean isStop = false;

	public static void main(String[] args) throws InterruptedException {
		final int loopTimes = 20;
		AlternateLoop2 loop = new AlternateLoop2(loopTimes);
		CountDownLatch latch = new CountDownLatch(3);

		new Thread(new Runnable() {
			@Override
			public void run() {
				int count = 0;
				while (!isStop && count <= loopTimes) {
					if (count++ == loopTimes) {
						isStop = true;
					} else {
						loop.loopA("a");
					}
				}
				latch.countDown();
			}
		}, "A").start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!isStop) {
					loop.loopB("l");
				}
				latch.countDown();
			}
		}, "B").start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!isStop) {
					loop.loopC("i");
				}
				latch.countDown();
			}
		}, "C").start();

		latch.await();
		System.out.println(Arrays.toString(loop.getArray()));
	}

}

/**
 * 交替循环
 */
class AlternateLoop2 {

	private int number = 1; // 当前正在执行线程的标记

	private String[] array;
	private volatile int index = 0;

	private Lock lock = new MyReentrantLock();// 创建可重入锁

	private Condition condition1 = lock.newCondition();
	private Condition condition2 = lock.newCondition();
	private Condition condition3 = lock.newCondition();

	public AlternateLoop2(int loopTimes) {
		array = new String[loopTimes * 3];
	}

	public void loopA(String content) {
		lock.lock();

		try {
			// 1. 判断
			if (number != 1) {
				condition1.await();
			}

			array[index++] = content;

			// 3. 唤醒
			number = 2;
			condition2.signal();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	public void loopB(String content) {
		lock.lock();

		try {
			// 1. 判断
			if (number != 2) {
				condition2.await();
			}

			array[index++] = content;

			// 3. 唤醒
			number = 3;
			condition3.signal();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	public void loopC(String content) {
		lock.lock();

		try {
			// 1. 判断
			if (number != 3) {
				condition3.await();
			}

			array[index++] = content;

			// 3. 唤醒
			number = 1;
			condition1.signal();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	public String[] getArray() {
		return array;
	}

}