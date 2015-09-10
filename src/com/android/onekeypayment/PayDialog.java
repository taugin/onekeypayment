package com.android.onekeypayment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

@SuppressLint("SetJavaScriptEnabled")
public class PayDialog extends ProgressDialog {
    public static final String URL_CM_WAP_A = "http://a.10086.cn/pams2/l/s.do?j=l&c=1426&ver=2&p=72";
    public static final String URL_CM_WAP = "http://wap.10086.cn/js/index.html";
    public static final String URL_CM_READ;
    static {
        CmReadUrlGenerator generator = new CmReadUrlGenerator();
        URL_CM_READ = generator.generateUrl();
    }

    enum RequestStep {
        REQ_PAGE, REQ_PAY
    }

    private static final int MAX_RETRY_TIMES = 3;
    private MyReceiver mMyReceiver;
    private Context mContext;
    private boolean mRequesting = false;
    private NetworkState mNetworkState = new NetworkState();
    private Handler mHandler;
    private String mRequestUrl;
    private WebView mWebViewForPay;
    private ProgressBar mProgressBar;
    private String mOrderId;
    private int mRetryTimes = 0;
    private RequestStep mRequestStep;
    private int mPayPrice = -1;
    private PayResult mPayResult;

    private long DEBUGTIME1;
    private long DEBUGTIME2;
    private long DEBUGTIME3;
    private long DEBUGTIME4;

    public PayDialog(Context context, int price) {
        super(context);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        mContext = context;
        mPayPrice = price;
        setMessage("正在支付，请稍后 ...");
        mPayResult = new PayResult();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebViewForPay = new WebView(mContext);
        mWebViewForPay.getSettings().setJavaScriptEnabled(true);
        mWebViewForPay.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        mWebViewForPay.setWebViewClient(mWebViewClient);
        RelativeLayout layout = new RelativeLayout(mContext);
        RelativeLayout.LayoutParams rParams1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.addView(mWebViewForPay, rParams1);
        mProgressBar = new ProgressBar(mContext);
        RelativeLayout.LayoutParams rParams2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rParams2.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(mProgressBar, rParams2);
        // setContentView(layout);
        /*
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = (int) (0.6f * dm.heightPixels);
        params.width = (int) (0.8f * dm.widthPixels);
        params.dimAmount = 0.5f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(params);
        */
        init();
    }

    private void init() {
        mHandler = new Handler();
        mHandler.postDelayed(mDismissRunnable, 120 * 1000);
        if (mPayPrice <= 0) {
            setPayResult(PayResult.PAY_FAILED, "支付金额必须大于0");
            dismissDialog();
            return;
        }

        mMyReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mMyReceiver, filter);
        // mRequestUrl = URL_CM_READ;
        // startRequestIfReady();
        getOrder(mPayPrice);
    }

    private void getOrder(final int price) {
        setDialogMessage("正在获取订单号");
        new Thread(){
            public void run() {
                Log.d(Log.TAG, "");
                String result = HttpManager.get(mContext).sendHttpGet(Util.GET_PAY_PAGE_URL + price);
                if (TextUtils.isEmpty(result)) {
                    Log.d(Log.TAG, "result : " + result);
                    setPayResult(PayResult.PAY_FAILED, "无法获取订单号");
                    dismissDialog();
                    return;
                }
                try {
                    JSONObject jobj = new JSONObject(result);
                    if (jobj.has("url")) {
                        mRequestUrl = jobj.getString("url");
                    }
                    if (jobj.has("mobileReg")) {
                        String mobileReg = jobj.getString("mobileReg");
                        Util.setMobileReg(mContext, mobileReg);
                    }
                    if (jobj.has("sucUrlFunc")) {
                        String sucUrlFunc = jobj.getString("sucUrlFunc");
                        Util.setUrlReg(mContext, sucUrlFunc);
                    }
                    if (jobj.has("orderId")) {
                        mOrderId = jobj.getString("orderId");
                    }
                    if (jobj.has("verifyCodeQuery")) {
                        String cssQuery = jobj.getString("verifyCodeQuery");
                        Util.setVerifyCodeCssQuery(mContext, cssQuery);
                    }
                    if (jobj.has("answerQuery")) {
                        String cssQuery = jobj.getString("answerQuery");
                        Util.setAnswerCssQuery(mContext, cssQuery);
                    }
                    Log.d(Log.TAG, "mRequestUrl : " + mRequestUrl);
                    if (!TextUtils.isEmpty(mRequestUrl)) {
                        DEBUGTIME1 = System.currentTimeMillis();
                        startRequestIfReady();
                    } else {
                        dismissDialog();
                    }
                } catch(Exception e) {
                    Log.d(Log.TAG, "error : " + e);
                }
            }
        }.start();
    }

    public PayResult getPayResult() {
        return mPayResult;
    }

    private boolean isPaying() {
        return isShowing();
    }

    private void setPayResult(int payState, String payReason) {
        setPayResult(payState, payReason, null);
    }

    private void setPayResult(int payState, String payReason, String orderId) {
        mPayResult.mPayState = payState;
        mPayResult.mReason = payReason;
        mPayResult.mTime = System.currentTimeMillis();
        mPayResult.mOrderId = orderId;
        mPayResult.mPrice = mPayPrice;
    }

    private void setDialogMessage(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setMessage(message);
            }
        });
    }

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mRequestStep == RequestStep.REQ_PAY) {
                requestPayResultUrl(url);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Log.d(Log.TAG, "url : " + url);
            view.loadUrl("javascript:" + Util.JS_SHOW_HTML);
            view.loadUrl("javascript:showHtmlSource('" + url + "')");
        }
    };

    /**
     * 通知服务器，支付完成
     * @param url
     */
    private void notifyServerState(final String url) {
        Log.d(Log.TAG, "支付已经完成，开始通知服务器支付状态");
        new Thread(){
            public void run() {
                String result = null;
                final String notifyUrl = url.replaceAll("&amp;", "&");
                for (int count = 0; count < 3; count++) {
                    result = HttpManager.get(mContext).sendHttpGet(notifyUrl);
                    Log.d(Log.TAG, "result : " + result);
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
                // 查询支付状态
                if (!TextUtils.isEmpty(result)
                        && result.equalsIgnoreCase("success")) {
                    setPayResult(PayResult.PAY_SUCCESS, notifyUrl, mOrderId);
                } else {
                    setPayResult(PayResult.PAY_FAILED, "支付已成功，但通知服务器订单状态失败");
                }
                dismissDialog();
            }
        }.start();
    }

    /**
     * 获取支付成功页面，取得通知服务器的url地址
     * @param url
     */
    private void requestPayResultUrl(final String url) {
        Log.d(Log.TAG, "重定向至支付结果页面");
        new Thread(){
            public void run() {
                String result = HttpManager.get(mContext).sendHttpGet(url);
                processNotifyUrl(result);
            }
        }.start();
    }

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(String content, String state, String url) {
            Log.d(Log.TAG, "mRequestStep : " + mRequestStep);
            if (mRequestStep == RequestStep.REQ_PAGE) {
                if (!TextUtils.isEmpty(content) && isPaying()) {
                    setDialogMessage("正在解析手机号");
                    String phoneNumber = getMobileNumber(content);
                    Log.d(Log.TAG, "phoneNumber : " + phoneNumber);
                    if (!TextUtils.isEmpty(phoneNumber) && isPaying()) {
                        setDialogMessage("正在分析验证码答案");
                        String result = requestVerifyCodeAnswer(content);
                        if (!TextUtils.isEmpty(result) && isPaying()) {
                            setDialogMessage("正在分析支付地址");
                            String payUrl = parsePayUrl(content, result);
                            // Log.d(Log.TAG, "payUrl : " + payUrl);
                            if (!TextUtils.isEmpty(payUrl) && isPaying()) {
                                setDialogMessage("开始支付");
                                execPay(payUrl);
                            } else {
                                retry();
                            }
                        } else {
                            retry();
                        }
                    } else {
                        retry();
                    }
                } else {
                    if (mRetryTimes > MAX_RETRY_TIMES) {
                        retry();
                    }
                }
            }
            mRequesting = false;
        }
    }

    private void retry() {
        mRetryTimes++;
        if (mRetryTimes > MAX_RETRY_TIMES) {
            setPayResult(PayResult.PAY_FAILED, "无法识别验证码");
            dismissDialog();
            return;
        }
        Log.d(Log.TAG, "正在进行第" + mRetryTimes + "次重试");
        setDialogMessage("正在进行第" + mRetryTimes + "次重试");
        if (isPaying()) {
            requestPaymentUrl();
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

    private void dismissDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.INVISIBLE);
                dismiss();
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
     * 获取实际支付页面，自动由服务器识别验证码
     */
    private void requestPaymentUrl() {
        if (TextUtils.isEmpty(mRequestUrl)) {
            return;
        }
        mRequestStep = RequestStep.REQ_PAGE;
        setDialogMessage("开始请求支付页面");
        loadUrlOnUiThread(mRequestUrl, null);
        mRequesting = true;
    }

    private String requestVerifyCodeAnswer(String content) {
        Log.d(Log.TAG, "");
        String verifyUrl = HttpParser.parseVerifyUrl(mContext, content);
        if (TextUtils.isEmpty(verifyUrl)) {
            return null;
        }
        String httpHost = null;
        try {
            URL url = new URL(mRequestUrl);
            String host = url.getHost();
            String protocol = url.getProtocol();
            httpHost = protocol + "://" + host;
        } catch (MalformedURLException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        if (TextUtils.isEmpty(httpHost)) {
            return null;
        }
        String imgUrl = httpHost + verifyUrl;
        Log.d(Log.TAG, "imgUrl : " + imgUrl);
        String cookies = CookieManager.getInstance().getCookie(mRequestUrl);
        Bitmap bitmap = HttpManager.get(mContext).sendHttpGetBitmap(imgUrl,
                cookies);
        // TODO: 测试下载图片
        // Util.saveBitmap(bitmap);
        // Log.d(Log.TAG, "bitmap : " + bitmap);
        if (bitmap == null) {
            return null;
        }
        byte[] byteArray = Util.bitmapToArray(bitmap);
        bitmap.recycle();
        if (byteArray == null) {
            return null;
        }
        String result = HttpManager.get(mContext).sendHttpPostByteArray(
                Util.GET_REAL_PAY_URL, byteArray);
        DEBUGTIME4 = System.currentTimeMillis();
        Log.d(Log.TAG, "result : " + result);
        return result;
    }

    private String parsePayUrl(String content, String res) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        if (TextUtils.isEmpty(res)) {
            Log.d(Log.TAG, "Verification Code Answer is Null");
            return null;
        }

        String answer = null;
        try {
            JSONObject jobject = new JSONObject(res);
            String status = null;
            String msg = null;
            String data = null;
            if (jobject.has("status")) {
                status = jobject.getString("status");
            }
            if (jobject.has("msg")) {
                msg = jobject.getString("msg");
            }
            if (jobject.has("data")) {
                data = jobject.getString("data");
            }
            if (!"1".equalsIgnoreCase(status)) {
                return null;
            }
            answer = data;
        } catch (JSONException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        Log.d(Log.TAG, "answer : " + answer);
        String urlPath = HttpParser.parseAnswerUrl(mContext, content, answer);
        // Log.d(Log.TAG, "urlPath : " + urlPath);
        if (TextUtils.isEmpty(urlPath)) {
            return null;
        }
        String httpHost = null;
        try {
            URL url = new URL(mRequestUrl);
            String host = url.getHost();
            String protocol = url.getProtocol();
            httpHost = protocol + "://" + host;
        } catch (MalformedURLException e) {
            Log.d(Log.TAG, "error : " + e);
        }
        if (TextUtils.isEmpty(httpHost)) {
            return null;
        }
        return httpHost + urlPath;
    }

    private void execPay(String url) {
        Log.d(Log.TAG, "开始支付");
        mRequestStep = RequestStep.REQ_PAY;
        loadUrlOnUiThread(url, null);
    }

    private String getMobileNumber(String html) {
        return HttpParser.parsePhoneNumber(mContext, html);
    }

    private void processNotifyUrl(String html) {
        String notifyUrl = HttpParser.parseNotifyUrl(mContext, html);
        if (!TextUtils.isEmpty(notifyUrl) && isPaying()) {
            notifyServerState(notifyUrl);
        } else {
            Log.d(Log.TAG, "notifyUrl : " + notifyUrl);
            setPayResult(PayResult.PAY_FAILED, "无法获取商户通知地址");
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
            // Toast.makeText(context, tmp, Toast.LENGTH_SHORT).show();
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
        if (mHandler != null) {
            mHandler.removeCallbacks(mDismissRunnable);
        }
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
            setPayResult(PayResult.PAY_FAILED, "支付支付超时");
            dismissDialog();
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
