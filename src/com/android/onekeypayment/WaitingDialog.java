package com.android.onekeypayment;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

public class WaitingDialog extends ProgressDialog {


    public static final String URL_CM_WAP_A = "http://a.10086.cn/pams2/l/s.do?j=l&c=1426&ver=2&p=72";
    public static final String URL_CM_WAP = "http://wap.10086.cn/js/index.html";
    public static final String URL_CM_READ;
    static {
        CmReadUrlGenerator generator = new CmReadUrlGenerator();
        URL_CM_READ = generator.generateUrl();
    }
    private HttpManager mHttpManager;
    private String mFullHttpContent;
    private MyReceiver mMyReceiver;
    private Context mContext;
    private String mPhoneNumber;
    private boolean mRequesting = false;
    private NetworkState mNetworkState = new NetworkState();
    private Handler mHandler;
    private String mRequestUrl;
    private OnHttpRequestComplete mOnHttpRequestComplete;

    public WaitingDialog(Context context) {
        super(context);
        mRequestUrl = URL_CM_READ;
        setMessage("���Ժ�...");
        // setCancelable(false);
        setCanceledOnTouchOutside(false);
        mContext = context;
        mHandler = new Handler();
        mHttpManager = HttpManager.get(context);
        mMyReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mMyReceiver, filter);

        startRequestIfReady();
        mHandler.postDelayed(mDismissRunnable, 30 * 1000);
    }

    public void setOnHttpRequestComplete(OnHttpRequestComplete l) {
        mOnHttpRequestComplete = l;
    }

    private void requestPhoneNumberUrl() {
        new Thread(){
            public void run() {
                mRequesting = true;
                mFullHttpContent = mHttpManager.sendHttpGet(mRequestUrl);
                mPhoneNumber = getMobileNumber(mFullHttpContent);
                mRequesting = false;
                if (mOnHttpRequestComplete != null) {
                    mOnHttpRequestComplete.onRequestComplete();
                }
            }
        }.start();
    }

    public String getUrl() {
        return mRequestUrl;
    }

    public String getContent() {
        return mFullHttpContent;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    private String getMobileNumber(String html) {
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getMobileNumber html is empty");
            return null;
        }
        // String telRegex = "13\\d{9}|14[57]\\d{8}|15[012356789]\\d{8}|18[01256789]\\d{8}|17[0678]\\d{8}";
        String telRegex = "[^\\d](13\\d{9}|14[57]\\d{8}|15[012356789]\\d{8}|18[01256789]\\d{8}|17[0678]\\d{8})[^\\d]";
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
                    requestPhoneNumberUrl();
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
                requestPhoneNumberUrl();
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

    private void requestComplete() {
        
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

    
    @SuppressWarnings("deprecation")
    public static boolean isHostAvailable(Context context, String urlString) {
        boolean ret = false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            URL url;
            try {
                Log.d(Log.TAG, "urlString : " + urlString);
                url = new URL(urlString);
                Log.d(Log.TAG, "url : " + url.getHost());
                InetAddress iAddress = InetAddress.getByName(url.getHost());
                Log.d(Log.TAG, "iAddress : " + iAddress.getHostAddress());
                ret = cm.requestRouteToHost(ConnectivityManager.TYPE_MOBILE, ipToInt(iAddress.getHostAddress()));
            } catch (MalformedURLException e) {
                Log.d(Log.TAG, "error : " + e);
            } catch (UnknownHostException e) {
                Log.d(Log.TAG, "error : " + e);
            }
        }
        return ret;
    }

    public static int ipToInt(String addr) {
        String[] addrArray = addr.split("\\.");
        int num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            num += ((Integer.parseInt(addrArray[i]) % 256 * Math
                    .pow(256, power)));
        }
        return num;
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
}
