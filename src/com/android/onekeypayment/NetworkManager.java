package com.android.onekeypayment;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class NetworkManager {

    public static void setWifiState(Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        wifiManager.setWifiEnabled(enabled);
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    /**
     * �����ֻ���ƶ����
     */
    public static void setMobileData(Context context, boolean enabled) {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Class ownerClass = mConnectivityManager.getClass();
            Class[] argsClass = new Class[1];
            argsClass[0] = boolean.class;
            Method method = ownerClass.getMethod("setMobileDataEnabled",
                    argsClass);
            method.invoke(mConnectivityManager, enabled);
        } catch (Exception e) {
            Log.d(Log.TAG, "e : " + e);
        }
    }

    /**
     * �����ֻ��ƶ���ݵ�״̬
     * 
     * @param pContext
     * @param arg
     *            Ĭ����null
     * @return true ���� false δ����
     */
    public static boolean getMobileDataState(Context pContext, Object[] arg) {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Class ownerClass = mConnectivityManager.getClass();
            Class[] argsClass = null;
            if (arg != null) {
                argsClass = new Class[1];
                argsClass[0] = arg.getClass();
            }
            Method method = ownerClass.getMethod("getMobileDataEnabled",
                    argsClass);
            Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);
            return isOpen;
        } catch (Exception e) {
            return false;
        }
    }
}
