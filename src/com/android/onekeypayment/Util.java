package com.android.onekeypayment;

import android.content.Context;
import android.preference.PreferenceManager;


public class Util {
    public static final String GET_HTLM = ""
            + "function showHtmlSource(currentUrl) {"
            + "     html = '';"
            + "     try {"
            + "         html = document.getElementsByTagName('html')[0].outerHTML;"
            + "     } catch(e) {"
            + "     }"
            + "     window.local_obj.showSource(html, 'html', currentUrl);"
            + "}";

    public static final String DEFAULT_EVAL = "var fnCallback = function(){ return document.getElementsByClassName('btn_back')[0].href; }";
    public static final String GET_URL = ""
            + "function showUrl(currentUrl) {"
            + "     url = '';"
            + "     try {"
            + "         eval(\"" + DEFAULT_EVAL + "\");"
            + "         url = fnCallback();"
            + "     } catch(e) {"
            + "     }"
            + "     window.local_obj.showSource(url, 'url', currentUrl);"
            + "}";

    public static final String GET_URL2 = ""
            + "function showUrl(currentUrl) {"
            + "     url = '';"
            + "     try {"
            + "     url = document.getElementsByClassName('btn_back')[0].href;"
            + "     } catch(e) {"
            + "     }"
            + "     window.local_obj.showSource(url, 'url', currentUrl);"
            + "}";

    private static void setSucUrlFunc(Context context, String sucUrlFunc) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("sucurlfunc", sucUrlFunc).commit();
    }

    /**
     * 
     * @param context
     * @return Js for Success url
     */
    private static String getSucUrlFunc(Context context) {
        String evalString = PreferenceManager.getDefaultSharedPreferences(context).getString("sucurlfunc", DEFAULT_EVAL);
        String jsString = ""
                + "function showUrl(currentUrl) {"
                + "     url = '';"
                + "     try {"
                + "         eval(\"" + evalString + "\");"
                + "         url = fnCallback();"
                + "     } catch(e) {"
                + "     }"
                + "     window.local_obj.showSource(url, 'url', currentUrl);"
                + "}";
        return jsString;
    }

    public static final String TELREGEX = "[^\\d](13\\d{9}|14[57]\\d{8}|15[012356789]\\d{8}|18[01256789]\\d{8}|17[0678]\\d{8})[^\\d]";

    public static void setMobileReg(Context context, String mobileReg) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("mobilereg", mobileReg).commit();
    }
    /**
     * 
     * @param context
     * @return regex for phonenumber
     */
    public static String getMobileReg(Context context) {
        String regEx = PreferenceManager.getDefaultSharedPreferences(context).getString("mobilereg", TELREGEX);
        return regEx;
    }


    public static final String URL_REGEX = "href=['|\"](http://[^'|^\"]+)";
    public static void setUrlReg(Context context, String urlReg) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("urlreg", urlReg).commit();
    }
    public static String getUrlReg(Context context) {
        String urlRegex = PreferenceManager.getDefaultSharedPreferences(context).getString("urlreg", URL_REGEX);
        return urlRegex;
    }

    public static final String GET_PAY_PAGE_URL = "http://10.0.0.122:2283/reade/cm/buy/apply/";
    public static final String GET_REAL_PAY_URL = "http://10.0.0.122:2283/reade/cm/buy/parse";
}