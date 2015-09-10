package com.android.onekeypayment.testpay;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.android.onekeypayment.HttpCookie;
import com.android.onekeypayment.HttpManager;
import com.android.onekeypayment.HttpParser;
import com.android.onekeypayment.Log;
import com.android.onekeypayment.PayDialog;
import com.android.onekeypayment.R;
import com.android.onekeypayment.Util;
import com.android.onekeypayment.R.id;
import com.android.onekeypayment.R.layout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener {

    private PayDialog mWaitingDialog = null;
    private WebView mWebView;
    private ImageView mImageView;
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mWebView = (WebView) findViewById(R.id.webview);
        mImageView = (ImageView) findViewById(R.id.imageview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");

        Button button = null;
        button = (Button) findViewById(R.id.get_phonenumber);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.webview_refresh);
        button.setOnClickListener(this);

        mWebView.loadUrl("file:///android_asset/mm.html");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.get_phonenumber) {
            mWaitingDialog = new PayDialog(this, 1);
            mWaitingDialog.show();
        } else if (v.getId() == R.id.webview_refresh) {
            loadBitmap();
        }
    }

    private void postPage() {
        new Thread() {
            public void run() {
                try {
                    FileInputStream is = new FileInputStream(
                            "/sdcard/mm.html");
                    byte[] buf = new byte[4096];
                    int read = 0;
                    StringBuilder builder = new StringBuilder();
                    String tmp = null;
                    while ((read = is.read(buf)) > 0) {
                        tmp = new String(buf, 0, read);
                        builder.append(tmp);
                    }
                    String cookies = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("cookies", "");
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("html", builder.toString());
                    hashMap.put("cookies", cookies);
                    hashMap.put("orderId", "45454454454545445445");
                    String result = HttpManager.get(MainActivity.this).sendHttpPost(Util.GET_REAL_PAY_URL, hashMap);
                    // Log.d(Log.TAG, "result : " + result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void getorderurl() {
        new Thread() {
            public void run() {
                String result = HttpManager.get(MainActivity.this).sendHttpGet(
                        Util.GET_PAY_PAGE_URL + "1");
                Log.d(Log.TAG, "result : " + result);
            }
        }.start();
    }

    final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(String content, String state, String url) {
            Log.d(Log.TAG, "content : " + content);
        }
    }

    private void test() {
        new Thread(){
            public void run() {
                Log.d(Log.TAG, "");
                String result = HttpManager.get(MainActivity.this).sendHttpGet(Util.GET_PAY_PAGE_URL + 1);
                if (TextUtils.isEmpty(result)) {
                    Log.d(Log.TAG, "result : " + result);
                }
                try {
                    JSONObject jobj = new JSONObject(result);
                    if (jobj.has("url")) {
                        Log.d(Log.TAG, "url : " + jobj.getString("url"));
                    }
                    if (jobj.has("mobileReg")) {
                        String mobileReg = jobj.getString("mobileReg");
                        Log.d(Log.TAG, "mobileReg : " + mobileReg);
                        Util.setMobileReg(MainActivity.this, mobileReg);
                    }
                    if (jobj.has("sucUrlFunc")) {
                        String sucUrlFunc = jobj.getString("sucUrlFunc");
                        Util.setUrlReg(MainActivity.this, sucUrlFunc);
                        Log.d(Log.TAG, "sucUrlFunc : " + sucUrlFunc);
                    }
                    if (jobj.has("orderId")) {
                        Log.d(Log.TAG, "mOrderId : " + jobj.getString("orderId"));
                    }
                    
                    processNotifyUrl();
                } catch(Exception e) {
                    Log.d(Log.TAG, "error : " + e);
                }
            }
        }.start();
    }

    private void processNotifyUrl() {
        try {
            FileInputStream is = new FileInputStream(
                    "/sdcard/ss.html");
            byte[] buf = new byte[4096];
            int read = 0;
            StringBuilder builder = new StringBuilder();
            String tmp = null;
            while ((read = is.read(buf)) > 0) {
                tmp = new String(buf, 0, read);
                builder.append(tmp);
            }
            String html = builder.toString();
            Log.d(Log.TAG, "");
            if (TextUtils.isEmpty(html)) {
                Log.d(Log.TAG, "getNotifiyUrl html is empty");
                return;
            }
            String urlRegex = Util.getUrlReg(MainActivity.this);
            Pattern p = Pattern.compile(urlRegex);
            Matcher m = p.matcher(html);
            String notifyUrl = null;
            if (m != null && m.find()) {
                notifyUrl = m.group(m.groupCount());
            }
            Log.d(Log.TAG, "notifyUrl : " + notifyUrl);
            if (!TextUtils.isEmpty(notifyUrl)) {
                final String finalurl = notifyUrl;
                mWebView.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        mWebView.loadUrl(finalurl);
                    }
                });
            }
        } catch(Exception e) {
            
        }
    }

    private void loadBitmap() {
        new Thread() {
            public void run() {
                String url = "http://wap.cmread.com/rdo/vc/avi?ln=1579_11244__2_&amp;t1=16687&amp;pftype=RDOOrder&amp;cm=J0080002&amp;picw=80&amp;pich=26&amp;picfs=20&amp;vt=2";
                url = url.replaceAll("&amp;", "&");
                url = url.replaceFirst("picw=\\d*", "picw=220")
                        .replaceFirst("pich=\\d*", "pich=90")
                        .replaceFirst("picfs=\\d*", "picfs=60");
                // url = url.replaceAll("&amp;", "&");
                String cookies = HttpCookie.get(MainActivity.this)
                        .getCookies();
                final Bitmap bitmap = HttpManager.get(MainActivity.this)
                        .sendHttpGetBitmap(url, cookies);
                url = "http://123.57.27.125/read/cm/buy/parse";
                final String result = HttpManager.get(MainActivity.this)
                        .sendHttpPostByteArray(url, Util.bitmapToArray(bitmap));
                mImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(bitmap);
                        mWebView.loadDataWithBaseURL(null, result, "text/html",
                                "utf-8", null);
                    }
                });
            }
        }.start();
    }

    private void htmlParse() {
        try {
            FileInputStream is = new FileInputStream("/sdcard/mm.html");
            byte[] buf = new byte[4096];
            int read = 0;
            StringBuilder builder = new StringBuilder();
            String tmp = null;
            while ((read = is.read(buf)) > 0) {
                tmp = new String(buf, 0, read);
                builder.append(tmp);
            }
            String html = builder.toString();
            Log.d(Log.TAG,
                    "verifyUrl : " + HttpParser.parseVerifyUrl(this, html));
            Log.d(Log.TAG,
                    "answerUrl : " + HttpParser.parseAnswerUrl(this, html, "3"));
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private void upload() {
        new Thread() {
            public void run() {
                uploadImage();
            }
        }.start();
    }

    private void uploadImage() {
        try {
            FileInputStream fis = new FileInputStream("/sdcard/verify.jpg");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = 0;
            byte[] buffer = new byte[1024];
            while ((read = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
            fis.close();
            byte byteArray[] = baos.toByteArray();
            baos.close();
            String result = HttpManager.get(this).sendHttpPostByteArray(
                    "http://10.0.0.122:2283/read/cm/buy/parse", byteArray);
            Log.d(Log.TAG, "result : " + result);
        } catch (FileNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }

    }
}
