package com.android.onekeypayment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Base64;


public class Util {
    public static final String GET_PAY_PAGE_URL = "http://123.57.27.125/read/cm/buy/apply/";
    public static final String GET_REAL_PAY_URL = "http://123.57.27.125/read/cm/buy/parse";

    public static final String JS_SHOW_HTML = ""
            + "function showHtmlSource(currentUrl) {"
            + "     html = '';"
            + "     try {"
            + "         html = document.getElementsByTagName('html')[0].outerHTML;"
            + "     } catch(e) {"
            + "     }"
            + "     window.local_obj.showSource(html, 'html', currentUrl);"
            + "}";

    private static final String TELREGEX = "[^\\d](13\\d{9}|14[57]\\d{8}|15[012356789]\\d{8}|18[01256789]\\d{8}|17[0678]\\d{8})[^\\d]";
    public static String getMobileReg(Context context) {
        String regEx = PreferenceManager.getDefaultSharedPreferences(context).getString("mobilereg", TELREGEX);
        return regEx;
    }
    public static void setMobileReg(Context context, String mobileReg) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("mobilereg", mobileReg).commit();
    }


    private static final String URL_REGEX = "href=['|\"](http://[^'|^\"]+)";
    public static void setUrlReg(Context context, String urlReg) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("urlreg", urlReg).commit();
    }
    public static String getUrlReg(Context context) {
        String urlRegex = PreferenceManager.getDefaultSharedPreferences(context).getString("urlreg", URL_REGEX);
        return urlRegex;
    }

    private static final String VERIFYCODE_CSSQUERY = "img[src^=/rdo/vc/avi]";

    public static void setVerifyCodeCssQuery(Context context, String cssQuery) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("verify_code_query", cssQuery).commit();
    }
    public static String getVerifyCodeCssQuery(Context context) {
        String urlRegex = PreferenceManager.getDefaultSharedPreferences(context).getString("verify_code_query", VERIFYCODE_CSSQUERY);
        return urlRegex;
    }

    private static final String ANSWER_CSSQUERY = "img[src^=/rdo/images/verification/%s.png]";

    public static void setAnswerCssQuery(Context context, String cssQuery) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("answer_query", cssQuery).commit();
    }
    public static String getAnswerCssQuery(Context context) {
        String urlRegex = PreferenceManager.getDefaultSharedPreferences(context).getString("answer_query", ANSWER_CSSQUERY);
        return urlRegex;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        String result = "";
        ByteArrayOutputStream bos = null;
        try {
            if (bitmap != null) {
                bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);// 将bitmap放入字节数组流中

                bos.flush();// 将bos流缓存在内存中的数据全部输出，清空缓存
                bos.close();

                byte[] bitmapByte = bos.toByteArray();
                result = Base64.encodeToString(bitmapByte, Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static byte[] bitmapToArray(Bitmap bitmap) {
        ByteArrayOutputStream bos = null;
        try {
            if (bitmap != null) {
                bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);// 将bitmap放入字节数组流中
                bos.flush();// 将bos流缓存在内存中的数据全部输出，清空缓存
                bos.close();
                byte[] bitmapByte = bos.toByteArray();
                bos.close();
                return bitmapByte;
            }
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    public static void saveBitmap(Bitmap bitmap) {
        Log.d(Log.TAG, "保存图片");
        File f = new File("/sdcard/", "verify.jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Log.d(Log.TAG, "已经保存");
        } catch (FileNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }
}