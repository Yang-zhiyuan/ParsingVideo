package com.example.util;
import java.util.Random;


public class TextUtil {
    /**
     * 取两个文本之间的文本值
     *
     * @param text
     * @param left
     * @param right
     * @return
     */

    public static String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }


    /**
     * 取随机数字
     *
     * @param num
     * @return
     */
    public static String getRandomNum(int num) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            Random rnd = new Random();
            sb.append(rnd.nextInt(9));
        }
        return sb.toString();
    }

    /**
     * 根据范围取随机数字
     *
     * @param
     * @return
     */
    public static int getRandomNumRang(int end) {
        Random rnd = new Random();
        return rnd.nextInt(end);
    }


    public static String makeImei() {
        String imeiString = TextUtil.getRandomNum(14);
        char[] imeiChar = imeiString.toCharArray();
        int resultInt = 0;
        for (int i = 0; i < imeiChar.length; i++) {
            int a = Integer.parseInt(String.valueOf(imeiChar[i]));
            i++;
            final int temp = Integer.parseInt(String.valueOf(imeiChar[i])) * 2;
            final int b = temp < 10 ? temp : temp - 9;
            resultInt += a + b;
        }
        resultInt %= 10;
        resultInt = resultInt == 0 ? 0 : 10 - resultInt;
        return imeiString + resultInt;
    }
}
