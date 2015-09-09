package com.android.onekeypayment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

public class HttpManager {

    private Context mContext;
    private DefaultHttpClient mDefaultHttpClient;

    private static HttpManager sHttpManager;
    public static HttpManager get(Context context) {
        if (sHttpManager == null) {
            sHttpManager = new HttpManager();
        }
        sHttpManager.setContext(context);
        return sHttpManager;
    }

    private void setContext(Context context) {
        mContext = context;
    }

    private HttpManager() {
        mDefaultHttpClient = new DefaultHttpClient();
        // mDefaultHttpClient.setRedirectHandler(new CustomRedirectHandler());
    }

    class CustomRedirectHandler implements RedirectHandler {
        @Override
        public boolean isRedirectRequested(HttpResponse response,
                HttpContext context) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == 302) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public URI getLocationURI(HttpResponse response, HttpContext context)
                throws ProtocolException {
            String redirectedUrl = null;
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == 302) {
                    redirectedUrl = response.getFirstHeader("Location").getValue();
                    Log.d(Log.TAG, "redirectedUrl : " + redirectedUrl);
                }
            }
            return URI.create(redirectedUrl);
        }
    }

    public String sendHttpGet(String url) {
        return sendHttpGet(url, null);
    }

    public String sendHttpGet(String url, String cookies) {
        // Log.d(Log.TAG, "url : " + url);
        HttpUriRequest request = new HttpGet(url);

        HttpResponse httpResponse = null;
        try {
            if (!TextUtils.isEmpty(cookies)) {
                request.addHeader("Cookie", cookies);
            }
            // HttpHost proxy = new HttpHost("10.0.0.172", 80, "http");  
            // HttpHost target = new HttpHost(url, 80, "http");  
            // mDefaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

            request.addHeader("Content-type", "text/plain");
            httpResponse = mDefaultHttpClient.execute(request);

            // Log.d(Log.TAG, "httpResponse : " + httpResponse);
            if (httpResponse != null) {
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine != null) {
                    int statusCode = statusLine.getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity httpEntity = httpResponse.getEntity();
                        parseCookies();
                        if (httpEntity != null) {
                            boolean isStreaming = httpEntity.isStreaming();
                            // Log.d(Log.TAG, "isStreaming : " + isStreaming);
                            if (isStreaming) {
                                /*
                                InputStream is = httpResponse.getEntity()
                                        .getContent();
                                byte[] buf = new byte[4096];
                                int read = 0;
                                StringBuilder builder = new StringBuilder();
                                String tmp = null;
                                while ((read = is.read(buf)) > 0) {
                                    tmp = new String(buf, 0, read);
                                    builder.append(tmp);
                                }
                                return builder.toString();
                                */
                                try {
                                    String type = httpResponse.getFirstHeader("Content-Type").getValue();
                                    // Log.d(Log.TAG, "type : " + type);
                                } catch(Exception e) {
                                }
                                return EntityUtils.toString(httpEntity, "utf-8");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.d(Log.TAG, "e : " + e);
        }
        return null;
    }

    public String sendHttpPost(String url, HashMap<String, String> hashMap) {
        HttpPost httpPost = new HttpPost(url);
        List<BasicNameValuePair> listValuePair = null;
        if (hashMap != null) {
            Set<String> set = hashMap.keySet();
            if (set != null) {
                Iterator<String> iterator = set.iterator();
                if (iterator != null) {
                    listValuePair = new ArrayList<BasicNameValuePair>();
                    String key = null;
                    String value = null;
                    while(iterator.hasNext()) {
                        key = iterator.next();
                        value = hashMap.get(key);
                        listValuePair.add(new BasicNameValuePair(key, value));
                    }
                }
            }
        }
        // Log.d(Log.TAG, "listValuePair : " + listValuePair);
        if (listValuePair != null) {
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(listValuePair, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.d(Log.TAG, "error : " + e);
            }
        }
        try {
            HttpResponse httpResponse = mDefaultHttpClient.execute(httpPost);
            if (httpResponse != null) {
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    return EntityUtils.toString(entity, "utf-8");
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap sendHttpGetBitmap(String url, String cookies) {
        Log.d(Log.TAG, "url : " + url);
        HttpUriRequest request = new HttpGet(url);

        HttpResponse httpResponse = null;
        try {
            if (!TextUtils.isEmpty(cookies)) {
                request.setHeader("Cookie", cookies);
                Log.d(Log.TAG, "cookies : " + cookies);
            }
            httpResponse = mDefaultHttpClient.execute(request);
            if (httpResponse != null) {
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine != null) {
                    int statusCode = statusLine.getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity httpEntity = httpResponse.getEntity();
                        if (httpEntity != null) {
                            boolean isStreaming = httpEntity.isStreaming();
                            if (isStreaming) {
                                return BitmapFactory.decodeStream(httpEntity
                                        .getContent());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.d(Log.TAG, "e : " + e);
        }
        return null;
    }

    public String sendHttpPostByteArray(String url, byte[] byteArray) {
        HttpPost httpPost = new HttpPost(url);
        ByteArrayEntity bae = new ByteArrayEntity(byteArray);
        httpPost.setEntity(bae);
        try {
            HttpResponse httpResponse = mDefaultHttpClient.execute(httpPost);
            if (httpResponse != null) {
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    return EntityUtils.toString(entity, "utf-8");
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void parseCookies() {
        List<Cookie> lists = mDefaultHttpClient.getCookieStore().getCookies();
        CookieManager.get(mContext).storeCookies(lists);
        /*
        String cookies = "";
        if (lists != null) {
            for (Cookie cookie : lists) {
                cookies += cookie.getName() + "=" + cookie.getValue() + ";";
            }
        }
        Log.d(Log.TAG, "cookies : " + cookies);
        if (!TextUtils.isEmpty(cookies)) {
            PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString("cookies", cookies).commit();
        }
        */
    }
}
