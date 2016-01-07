package org.loader.liteplayer.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
	private static ExecutorService sThreadPool;
	
	public synchronized static ExecutorService obtain() {
		if(sThreadPool == null) {
			sThreadPool = Executors.newCachedThreadPool();
		}
		
		return sThreadPool;
	}
}
