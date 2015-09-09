package com.android.onekeypayment;

import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class CookieManager {

    private Context mContext;
    private static CookieManager sCookieManager;

    private CookieManager(Context context) {
        mContext = context;
    }

    public static CookieManager get(Context context) {
        if (sCookieManager == null) {
            sCookieManager = new CookieManager(context);
        }
        return sCookieManager;
    }

    public void storeCookies(List<Cookie> lists) {
        if (lists != null) {
            SharedPreferences sharedPreferences = mContext
                    .getSharedPreferences("wap.cmdread.com",
                            Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();
            Log.d(Log.TAG, "++++++++++++++++++++++++++++++++++++++++++++");
            for (Cookie cookie : lists) {
                PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                        .putString(cookie.getName(), cookie.getValue())
                        .commit();
                editor.putString(cookie.getName(),
                        cookie.getValue());
                Log.d(Log.TAG, cookie.getName() + " : " + cookie.getValue());
            }
            Log.d(Log.TAG, "============================================");
            editor.commit();
        }
    }

    @SuppressWarnings("unchecked")
    public String getCookies() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                "wap.cmdread.com", Context.MODE_PRIVATE);
        Map<String, String> map = (Map<String, String>) sharedPreferences
                .getAll();

        String cookies = "";
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                cookies += entry.getKey() + "=" + entry.getValue() + ";";
            }
        }
        Log.d(Log.TAG, "cookies : " + cookies);
        return cookies;
    }
}
