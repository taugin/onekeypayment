package com.android.onekeypayment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class CmReadUrlGenerator {

    public String generateUrl() {
        StringBuilder build = new StringBuilder();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String mcpid = "jy0002", orderNo = "46", feeCode = "86000001", reqTime = sdf
                    .format(new Date());
            String key, redirectUrl, cm, vt;
            key = "fenghuang888";
            redirectUrl = URLEncoder.encode("http://www.baidu.com", "UTF-8");
            cm = "J0080002";
            vt = "2";

            String sign = string2MD5(mcpid + feeCode + orderNo + reqTime + key)
                    .toUpperCase();

            build.append("http://wap.cmread.com/rdo/order?mcpid=")
                    .append(mcpid).append("&orderNo=").append(orderNo)
                    .append("&feeCode=").append(feeCode).append("&reqTime=")
                    .append(reqTime).append("&sign=").append(sign)
                    .append("&redirectUrl=").append(redirectUrl).append("&cm=")
                    .append(cm).append("&vt=").append(vt);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return build.toString();
    }

    public static String string2MD5(String inStr) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = md5Bytes[i] & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
