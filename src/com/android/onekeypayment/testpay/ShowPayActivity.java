package com.android.onekeypayment.testpay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.onekeypayment.OnekeyPay;
import com.android.onekeypayment.PayResult;
import com.android.onekeypayment.PaymentCallback;

public class ShowPayActivity extends Activity implements OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);

        Button button = null;
        button = new Button(this);
        button.setId(0x1234567);
        button.setText("一元支付");
        button.setOnClickListener(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(button, params);
        setContentView(layout);
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("确认支付一元");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final long time1 = System.currentTimeMillis();
                OnekeyPay.pay(ShowPayActivity.this, 1, new PaymentCallback() {
                    @Override
                    public void payStatus(PayResult result) {
                        long time2 = System.currentTimeMillis();
                        String showText = "支付消耗时间 : " + (time2 - time1) + "\n";
                        String showTitle = null;
                        if (result.mPayState == PayResult.PAY_SUCCESS) {
                            showTitle = "支付成功";
                            showText += "订单号  : \n" + result.mOrderId + "\n";
                            showText += "URL  : \n" + result.mReason + "\n";
                        } else {
                            showTitle = "支付失败";
                            showText += "原因 : " + result.mReason;
                        }
                        showAlertDialog(showTitle, showText);
                    }
                });
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    private void showAlertDialog(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ShowPayActivity.this);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton("确定", null);
                builder.create().show();
            }
        });
    }
}
