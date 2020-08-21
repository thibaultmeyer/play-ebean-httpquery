package com.zero_x_baadf00d.ebean.utils;

/**
 * Utils class to handle String related operations.
 *
 * @author Thibault Meyer
 * @version 20.08.21
 * @since 20.08.21
 */
public final class StringUtils {

    /**
     * Check if the given string is null or empty.
     *
     * @param str Rhe string to check
     * @return {@code true} if null or empty, otherwise {@code false}
     */
    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Gets the substring before the last occurrence of a separator.
     *
     * @param str       The string to use
     * @param separator The operator to use
     * @return The substring before the last occurrence of the separator
     */
    public static String substringBeforeLast(final String str, final String separator) {
        if (StringUtils.isEmpty(str) || StringUtils.isEmpty(separator)) {
            return str;
        }

        final int idx = str.lastIndexOf(separator);
        return idx == -1 ? str : str.substring(0, idx);
    }
}
