package com.isw.payapp.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkExecutor {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static ExecutorService getExecutor() {
        return executor;
    }
}
