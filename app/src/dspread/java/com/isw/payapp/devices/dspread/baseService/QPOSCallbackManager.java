package com.isw.payapp.devices.dspread.baseService;

import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QPOSCallbackManager {
    private static QPOSCallbackManager instance;

    public final Map<Class<?>, Object> callbackMap = new ConcurrentHashMap<>();

    public static QPOSCallbackManager getInstance() {
        if (instance == null) {
            synchronized (QPOSCallbackManager.class) {
                if (instance == null) {
                    instance = new QPOSCallbackManager();
                }
            }
        }
        return instance;
    }

    /**
     * register payment callback
     */
    public void registerPaymentCallback(IPaymentServiceCallback callback) {
        callbackMap.put(IPaymentServiceCallback.class, callback);
    }

    /**
     * register connection service callback
     */
    public void registerConnectionCallback(IConnectionServiceCallback callback) {
        callbackMap.put(IConnectionServiceCallback.class, callback);
    }

    /**
     * unregister payment callback
     */
    public void unregisterPaymentCallback() {
        callbackMap.remove(IPaymentServiceCallback.class);
    }

    /**
     * unregister connection service callback
     */
    public void unregisterConnectionCallback() {
        callbackMap.remove(IConnectionServiceCallback.class);
    }

    /**
     * get payment callback
     */
    @SuppressWarnings("unchecked")
    public IPaymentServiceCallback getPaymentCallback() {
        return (IPaymentServiceCallback) callbackMap.get(IPaymentServiceCallback.class);
    }

    /**
     * get connection service callback
     */
    @SuppressWarnings("unchecked")
    public IConnectionServiceCallback getConnectionCallback() {
        return (IConnectionServiceCallback) callbackMap.get(IConnectionServiceCallback.class);
    }
}
