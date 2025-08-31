package org.lucentrix.metaframe.util;

public class FormatUtil {
    public static final long msInSecond = 1000;
    public static final long msInMinute = msInSecond * 60;
    public static final long msInHour = msInMinute * 60;
    public static final long msInDay = msInHour * 24;
    public static final long msInYear = msInDay * 365;
    public static final long msInMonth = msInDay * 30;

    public static String toHumanReadable(long millis) {
        long years = millis / msInYear;
        millis %= msInYear;

        long months = millis / msInMonth;
        millis %= msInMonth;

        long days = millis / msInDay;
        millis %= msInDay;

        long hours = millis / msInHour;
        millis %= msInHour;

        long minutes = millis / msInMinute;
        millis %= msInMinute;

        long seconds = millis / msInSecond;
        millis %= msInSecond;

        long milliseconds = millis;

        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append("y ");
        if (months > 0) sb.append(months).append("mo ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");
        if (milliseconds > 0 || sb.length() == 0) sb.append(milliseconds).append("ms");

        return sb.toString().trim();
    }
}
