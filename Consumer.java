package com.emc.ecd.spt.restfulsamples;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Consumer {

	public static void main(String[] args) {

		System.out.println("Consumer Started");

		ThreadFactory threadFactory = Executors.defaultThreadFactory();

		ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 10, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(2), threadFactory);

		for (int i = 0; i <= 2; i++) {
			RestfullIA task = new RestfullIA();
			executor.execute(task);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();

		}
		executor.shutdown();

	}
}
