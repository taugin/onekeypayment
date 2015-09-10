package com.android.onekeypayment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

public class OnekeyPay {

    public static void pay(Context context, int price,
            final PaymentCallback callback) {
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException(
                    "Context must be an instance of Activity");
        }
        final PayDialog payDialog = new PayDialog(context, price);
        payDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (callback != null) {
                    callback.payStatus(payDialog.getPayResult());
                }
            }
        });
        payDialog.show();
    }
}
