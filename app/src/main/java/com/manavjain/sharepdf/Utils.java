package com.manavjain.sharepdf;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by YourFather on 25-12-2017.
 */

public class Utils {

    public final static String DEEP_LINK_DB_NAME = "pdf_deep_links";
    public final static String DEEP_LINK_URL = "https://fgvm8.app.goo.gl/";

    public static String getEncodedString(String string){
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDecodedString(String string){
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
