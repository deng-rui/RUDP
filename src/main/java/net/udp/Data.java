/*
 * Copyright 2020-2022 deng-rui.
 */

package net.udp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dr
 */
public class Data {
    public static ThreadPoolExecutor thread = new ThreadPoolExecutor(4, 4, 0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
        final AtomicInteger tag = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("ReliableSocket-Closing: " + tag.getAndIncrement());
            return thread;
        }
    });
    public static final Object waitData = new Object();
}
