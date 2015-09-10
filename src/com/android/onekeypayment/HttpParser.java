package com.android.onekeypayment;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.text.TextUtils;

public class HttpParser {

    public static String parseVerifyUrl(Context context, String html) {
        try {
            Document doc = Jsoup.parse(html);
            Elements imgAvi = doc.select("img[src^=/rdo/vc/avi");
            Element element = imgAvi.first();
            String verifyUrl = element.attr("src");
            verifyUrl = verifyUrl.replaceFirst("picw=\\d*", "picw=220")
                    .replaceFirst("pich=\\d*", "pich=90")
                    .replaceFirst("picfs=\\d*", "picfs=60");
            return verifyUrl;
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    public static String parseAnswerUrl(Context context, String html,
            String answer) {
        try {
            Document doc = Jsoup.parse(html);
            Elements imgAvi = doc.select("img[src^=/rdo/images/verification/"
                    + answer + ".png");
            String ansUrl = imgAvi.first().parent().attr("href");
            return ansUrl;
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    public static String parsePhoneNumber(Context context, String html) {
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getMobileNumber html is empty");
            return null;
        }
        String telRegex = Util.getMobileReg(context);
        Pattern p = Pattern.compile(telRegex);
        Matcher m = p.matcher(html);
        String phoneNumber = null;
        if (m != null && m.find()) {
            phoneNumber = m.group(m.groupCount());
        }
        return phoneNumber;
    }

    public static String parseOrderId(String notifyUrl) {
        List<NameValuePair> list = URLEncodedUtils.parse(
                URI.create(notifyUrl), "UTF-8");
        for (NameValuePair pair : list) {
            if ("orderNo".equalsIgnoreCase(pair.getName())) {
                return pair.getValue();
            }
        }
        return null;
    }

    public static String parseNotifyUrl(Context context, String html) {
        if (TextUtils.isEmpty(html)) {
            Log.d(Log.TAG, "getNotifiyUrl html is empty");
            return null;
        }
        String urlRegex = Util.getUrlReg(context);
        Pattern p = Pattern.compile(urlRegex);
        Matcher m = p.matcher(html);
        String notifyUrl = null;
        if (m != null && m.find()) {
            notifyUrl = m.group(m.groupCount());
        }
        return notifyUrl;
    }
}
