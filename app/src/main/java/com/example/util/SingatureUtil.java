package com.example.util;

import android.text.TextUtils;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class SingatureUtil {
    private static final String FANS_SALT = "382700b563f4";

    public static String genSignature(Map<String, String> params) {
        if (params == null) {
            return null;
        }
        String sign = "";
        StringBuffer sb = new StringBuffer();
        try {
            // 1. 字典升序排序
            SortedMap<String, String> sortedMap = new TreeMap<>(params);
            // 2. 拼按URL键值对
            Set<String> keySet = sortedMap.keySet();
            for (String key : keySet) {
                // sign不参与算法
                if (key.equals("sig") || key.equals("__NStokensig")) {
                    continue;
                }
                String value = sortedMap.get(key);
                sb.append(key + "=" + URLDecoder.decode(value, "UTF-8"));
            }
            String uriString = sb.toString();
            uriString = uriString + FANS_SALT;
            System.out.println("My String: \n" + uriString);
            // 3. MD5运算得到请求签名
            sign = MD5Util.MD5(uriString);
            System.out.println("My Sign:\n" + sign);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sign;
    }

    public static Map<String, String> getMapFromStr(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] arr = str.split("\\&");
        Map<String, String> map = new HashMap<>();
        for (String item : arr) {
            String[] itemArr = item.split("=", 2);
            map.put(itemArr[0], itemArr[1]);
        }
        return map;
    }
}
