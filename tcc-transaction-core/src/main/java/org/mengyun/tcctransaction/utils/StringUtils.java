package org.mengyun.tcctransaction.utils;

/**
 * Created by changmingxie on 11/11/15.
 */
public class StringUtils {

    public static boolean isNotEmpty(String value) {

        if(value == null) {
            return false;
        }

        if(value.equals("")) {
            return false;
        }

        return true;
    }

    public static boolean isEmpty(String value) {
        return !isNotEmpty(value);
    }
}
