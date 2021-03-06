package com.simon.credit.toolkit.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MyThreadPoolExecutorTest {

	public static void main(String[] args) {
		MyThreadPoolExecutor executor = null;
		// BlockingThreadPool blockingThreadPool = null;
		try {
			// 创建线程池
			executor = new MyThreadPoolExecutor(2, 5, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2));
			// blockingThreadPool = BlockingThreadPool.newBlockingThreadPool(Runtime.getRuntime().availableProcessors());

			for (int i = 1; i <= 10; i++) {
				Task task = new Task("task-" + i);
				executor.execute(task);
				// blockingThreadPool.execute(task);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!executor.isShutdown()) {
				executor.shutdown();
			}
//			if (!blockingThreadPool.isShutdown()) {
//				blockingThreadPool.shutdown();
//			}
		}
	}

}

class Task extends Thread {

	private String taskName;

	Task(String taskName) {
		this.taskName = taskName;
	}

	public void run() {
		try {
			System.out.println(taskName + " is running...");
			Thread.sleep(10000);// 线程睡眠一秒
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}