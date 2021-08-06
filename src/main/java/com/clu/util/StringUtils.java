package com.clu.util;

import java.net.URLEncoder;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    /**
     * 驼峰转下划线
     * @param input
     * @return
     */
    public static String camelToUnderline(CharSequence input) {
        if (input == null) return null; // garbage in, garbage out
        int length = input.length();
        StringBuilder result = new StringBuilder(length * 2);
        int resultLength = 0;
        boolean wasPrevTranslated = false;
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (i > 0 || c != '_') // skip first starting underscore
            {
                if (Character.isUpperCase(c)) {
                    if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                        result.append('_');
                        resultLength++;
                    }
                    c = Character.toLowerCase(c);
                    wasPrevTranslated = true;
                } else {
                    wasPrevTranslated = false;
                }
                result.append(c);
                resultLength++;
            }
        }
        return resultLength > 0 ? result.toString() : null;
    }

    public static long getNumber(String string, long defaultValue) {
        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 某个特殊字符后面的内容，最后一次出现的后面
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringAfterLast(String source, String stringToFind) {
        int index = source.lastIndexOf(stringToFind);
        if (index > -1) {
            return source.substring(index + stringToFind.length());
        } else {
            return null;
        }
    }

    /**
     * 某个特殊字符后面的内容，最后一次出现的后面
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringAfterFirst(String source, String stringToFind) {
        int index = source.indexOf(stringToFind);
        if (index > -1) {
            return source.substring(index + stringToFind.length());
        } else {
            return null;
        }
    }

    /**
     * 某个特殊字符前面的
     * @param source
     * @param stringToFind
     * @return
     */
    public static String stringBeforeFirst(String source, String stringToFind) {
        int index = source.indexOf(stringToFind);
        if (index > -1) {
            return source.substring(0, index);
        } else {
            return null;
        }
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String format(String message, Object... params) {
        if (params == null || params.length == 0) {
            return message;
        }
        return MessageFormatter.arrayFormat(message, params).getMessage();
    }

    public static String encode(Object o) {
        if (o == null) {
            return "";
        }

        String result;
        try {
            result = URLEncoder.encode(o.toString(), "UTF-8")
                .replaceAll("\\+", "%20")
                .replaceAll("%21", "!")
                .replaceAll("%27", "'")
                .replaceAll("%28", "(")
                .replaceAll("%29", ")")
                .replaceAll("%7E", "~");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return result;
    }

    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasText(String str) {
        return (hasLength(str) && containsText(str));
    }

    /**
     * 获取两个字符串的相似度
     * @param str
     * @param target
     * @return
     */
    public static float getSimilarityRatio(String str, String target) {
        int d[][]; // 矩阵
        int n = str.length();
        int m = target.length();
        int i; // 遍历str的
        int j; // 遍历target的
        char ch1; // str的
        char ch2; // target的
        int temp; // 记录相同字符,在某个矩阵位置值的增量,不是0就是1
        if (n == 0 || m == 0) {
            return 0;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) { // 初始化第一列
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) { // 初始化第一行
            d[0][j] = j;
        }

        for (i = 1; i <= n; i++) { // 遍历str
            ch1 = str.charAt(i - 1);
            // 去匹配target
            for (j = 1; j <= m; j++) {
                ch2 = target.charAt(j - 1);
                if (ch1 == ch2 || ch1 == ch2 + 32 || ch1 + 32 == ch2) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                // 左边+1,上边+1, 左上角+temp取最小
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + temp);
            }
        }

        return (1 - (float) d[n][m] / Math.max(str.length(), target.length())) * 100F;
    }
}
