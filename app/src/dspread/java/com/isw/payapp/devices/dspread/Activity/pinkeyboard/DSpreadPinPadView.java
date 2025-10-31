package com.isw.payapp.devices.dspread.Activity.pinkeyboard;

import android.content.Context;
import android.util.AttributeSet;

import com.isw.payapp.devices.interfaces.IPosService;
import com.isw.payapp.views.pinkeyboard.BasePinPadView;

public class DSpreadPinPadView extends BasePinPadView {

    public DSpreadPinPadView(Context context) {
        super(context);
    }

    public DSpreadPinPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DSpreadPinPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected IPosService createPinPadService() {
        // Remove the Context parameter since the base class method doesn't have it
        // You can access mContext from the base class if needed
        return new DSpreadPinPadServiceImpl(mContext);
    }
}