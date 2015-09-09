package com.android.onekeypayment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HttpParser {

    public static String parseVerifyUrl(String html) {
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

    public static String parseAnswerUrl(String html, String answer) {
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
}
