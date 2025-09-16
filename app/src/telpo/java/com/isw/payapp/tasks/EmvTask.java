package com.isw.payapp.tasks;

import android.content.Context;

import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvServiceListener;

import java.util.concurrent.Callable;
import java.util.logging.Handler;

public class EmvTask implements Callable<String> {

//    CardReaderJob jobs = new CardReaderJob(Context context, Handler na);
    @Override
    public String call() throws Exception {
        return null;
    }
}
