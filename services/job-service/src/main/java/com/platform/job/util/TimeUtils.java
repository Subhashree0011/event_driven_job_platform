package com.platform.job.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Time utility for consistent timestamp handling across the service.
 */
public final class TimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TimeUtils() {
        // Utility class
    }

    public static long nowEpochMillis() {
        return System.currentTimeMillis();
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static String formatIso(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
