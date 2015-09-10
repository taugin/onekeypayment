package com.android.onekeypayment;

public class PayResult {
    public static final int PAY_SUCCESS = 0;
    public static final int PAY_FAILED = 1;
    public int mPayState = PAY_FAILED;
    public String mOrderId;
    public long mTime;
    public String mReason;
    public int mPrice;
}
