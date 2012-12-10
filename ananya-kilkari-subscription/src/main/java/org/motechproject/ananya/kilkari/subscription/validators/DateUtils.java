package org.motechproject.ananya.kilkari.subscription.validators;


import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class DateUtils {

    private static String DATE_FORMAT = "dd-MM-yyyy";
    private static String DATE_TIME_FORMAT = "dd-MM-yyyy HH-mm-ss";
    public static String DATE_TIME_FORMAT_FOR_CC = "dd-MM-yyyy HH:mm:ss";
    private static final String TIME_FORMAT = "HH:mm:ss";

    public static DateTime parseDate(String date) {
        return StringUtils.isNotEmpty(date) ? DateTimeFormat.forPattern(DATE_FORMAT).parseDateTime(date) : null;
    }

    public static String formatDate(DateTime dateTime) {
        return dateTime == null ? null : dateTime.toString(DATE_FORMAT);
    }

    public static String formatTime(DateTime dateTime) {
        return dateTime == null ? null : dateTime.toString(TIME_FORMAT);
    }

    public static DateTime parseDateTime(String dateTime) {
        return StringUtils.isNotEmpty(dateTime) ? DateTimeFormat.forPattern(DATE_TIME_FORMAT).parseDateTime(dateTime) : null;
    }

    public static String formatDateTime(DateTime dateTime) {
        return dateTime == null ? null : dateTime.toString(DATE_TIME_FORMAT);
    }

    public static DateTime parseDateTimeForCC(String dateTime) {
        return StringUtils.isNotEmpty(dateTime) ? DateTimeFormat.forPattern(DATE_TIME_FORMAT_FOR_CC).parseDateTime(dateTime) : null;
    }

    public static String formatDateTimeForCC(DateTime dateTime) {
        return dateTime == null ? null : dateTime.toString(DATE_TIME_FORMAT_FOR_CC);
    }

    public static boolean isValidForCC(String dateTime) {
        try {
            return parseDateTimeForCC(dateTime) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
