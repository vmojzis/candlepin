/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.util;

import org.candlepin.model.CuratorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.ClosureUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Genuinely random utilities.
 */
public class Util {

    public static final String UTC_STR = "UTC";
    private static Logger log = LoggerFactory.getLogger(Util.class);
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Invokes the close() method of any given object, if present.
     */
    private static Closure closeInvoker = ClosureUtils.invokerClosure("close");

    private Util() {
        // default ctor
    }

    /**
     * Generates a random UUID.
     *
     * @return a random UUID.
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a 32-character UUID to use with object creation/migration.
     * <p></p>
     * The UUID is generated by creating a "standard" UUID and removing the hyphens. The UUID may be
     * standardized by reinserting the hyphens later, if necessary.
     *
     * @return
     *  a 32-character UUID
     */
    public static String generateDbUUID() {
        return generateUUID().replace("-", "");
    }

    public static <T> Set<T> asSet(T... values) {
        Set<T> output = new HashSet<>(values.length);

        for (T value : values) {
            output.add(value);
        }

        return output;
    }

    public static Date tomorrow() {
        return addDaysToDt(1);
    }

    public static Date yesterday() {
        return addDaysToDt(-1);
    }

    public static Date midnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Date addDaysToDt(int dayField) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, dayField);
        return calendar.getTime();
    }

    public static Date addMinutesToDt(int minuteField) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minuteField);
        return calendar.getTime();
    }

    public static Date toDate(String dt) {
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
        try {
            return fmt.parse(dt);
        }
        catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T assertNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static boolean equals(String str, String str1) {
        if (str == str1) {
            return true;
        }

        if ((str == null) ^ (str1 == null)) {
            return false;
        }

        return str.equals(str1);
    }

    /**
     * Invokes the close() method on the given closable object, and logs that
     * it is closing "msg". If closable is null, the function simply returns.
     *
     * For example, if msg = AMQPSession, the logs will show something like
     * this:  INFO Going to close: AMQPSession
     *
     * @param closable Object with a close() method.
     * @param msg indicates what the closable is and used to log informational
     * messages.
     */
    public static void closeSafely(Object closable, String msg) {
        if (closable == null) {
            return;
        }
        try {
            log.info("Going to close: " + msg);
            closeInvoker.execute(closable);
        }
        catch (Exception e) {
            log.warn(msg + ".close() was not successful!", e);
        }
    }

    public static String capitalize(String str) {
        char[] chars = str.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static long generateUniqueLong() {
        /*
          This deserves explanation.

          A random positive Long has 63 bits of hash space.  We want
          to have a given amount of certainty about the probability of
          collisions within this space.  This is an instance of the
          Birthday Problem[1].  We can get the probability that any
          two random numbers collide with the approximation:

          1-e**((-(N**2))/(2H))

          Where e is Euler's number, N is the number of random numbers
          generated, and H is the number of possible random outcomes.

          Suppose then that we generated one billion serials, with
          each serial being a 63-bit positive Long.  Then our
          probability of having a collision would be:

          irb(main):001:0> 1-Math.exp((-(1000000000.0**2))/(2.0*(2**63)))
          => 0.052766936243662

          So, if we generated a *billion* such serials, there is only
          a 5% chance that any two of them would be the same.  In
          other words, there is 95% chance that we would not have a
          single collision in one billion entries.

          The chances obviously get even less likely with smaller
          numbers.  With one million, the probability of a collision
          is:

          irb(main):002:0> 1-Math.exp((-(1000000.0**2))/(2.0*(2**63)))
          => 5.42101071809853e-08

          Or, 1 in 18,446,744.

          [1] http://en.wikipedia.org/wiki/Birthday_problem
         */

        long rnd;

        // Impl note:
        // Math.abs cannot negate MIN_VALUE, so we'll generate a new value when that happens.
        do {
            rnd = new SecureRandom().nextLong();
        }
        while (rnd == Long.MIN_VALUE);

        return Math.abs(rnd);
    }

    public static String toBase64(byte[] data) {
        try {
            // to be thread-safe, we should create it from the static method
            // If we don't specify the line separator, it will use CRLF
            return new String(new Base64(64, "\n".getBytes()).encode(data), "ASCII");
        }
        catch (UnsupportedEncodingException e) {
            log.warn("Unable to convert binary data to string", e);
            return new String(data);
        }
    }

    public static SimpleDateFormat getUTCDateFormat() {
        SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
        iso8601DateFormat.setTimeZone(TimeZone.getTimeZone(UTC_STR));
        return iso8601DateFormat;
    }

    public static String readFile(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
        }
        catch (IOException e) {
            throw new CuratorException(e);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                log.warn("problem closing BufferedReader", e);
            }
        }
        return builder.toString();
    }

    public static String hash(String password) {
        //This is secure because even if the salt is known, a cracker
        //would still need to generate their own rainbow table, which
        //is the same as brute-forcing the password in the first place.

        String salt = "b669e3274a43f20769d3dedf03e9ac180e160f92";
        String combined = salt + password;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }

        try {
            md.update(combined.getBytes("UTF-8"), 0, combined.length());
        }
        catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }

        byte[] sha1hash = md.digest();
        return new String(Hex.encodeHex(sha1hash));
    }


    public static String toJson(Object anObject) throws JsonProcessingException {
        return mapper.writeValueAsString(anObject);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        T output = null;
        try {
            output = mapper.readValue(json, clazz);
        }
        catch (Exception e) {
            log.error("Could no de-serialize the following json " + json, e);
        }
        return output;
    }

    @SuppressWarnings("rawtypes")
    public static String getClassName(Class c) {
        return getClassName(c.getName());
    }

    public static String getClassName(String fullClassName) {
        int firstChar = fullClassName.lastIndexOf('.') + 1;
        if (firstChar > 0) {
            fullClassName = fullClassName.substring(firstChar);
        }
        return fullClassName;
    }

    public static String reverseEndian(String in) {
        in = (in.length() % 2 != 0) ? "0" + in : in;
        StringBuilder sb = new StringBuilder();
        for (int i = in.length() - 2; i >= 0; i += (i % 2 == 0) ? 1 : -3) {
            sb.append(in.charAt(i));
        }
        return sb.toString();
    }

    public static String transformUuid(String uuid) {
        String[] partitions = uuid.split("-");
        List<String> newPartitions = new LinkedList<>();
        // We only want to revese the first three partitions
        for (int i = 0; i < partitions.length; i++) {
            newPartitions.add(i < 3 ? reverseEndian(partitions[i]) : partitions[i]);
        }
        return StringUtils.join(newPartitions, '-');
    }

    /*
     * Gets possible guest uuids regardless of endianness. When given a non-uuid,
     * this should return a list of length 1, with the given value.  All values
     * returned should be lower case
     */
    public static List<String> getPossibleUuids(String... ids) {
        List<String> results = new LinkedList<>();
        for (String id : ids) {
            if (id != null) {
                // We want to use lower case everywhere we can in order
                // to do less work at query time.
                id = id.toLowerCase();
            }
            results.add(id);
            if (isUuid(id)) {
                results.add(transformUuid(id));
            }
        }
        return results;
    }

    private static final String UUID_REGEX = "[a-fA-F0-9]{8}-" +
        "[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    public static boolean isUuid(String uuid) {
        return uuid != null && uuid.matches(UUID_REGEX);
    }

    public static String collectionToString(Collection c) {
        StringBuffer buf = new StringBuffer();
        for (Object o : c) {
            buf.append(o.toString());
            buf.append(" ");
        }
        return buf.toString();
    }

    /**
     * Compares two collections for equality without using the collection's equals method. This is
     * primarily only useful when working with collections that may actually be Hibernate bags, as
     * bags and proxies do not properly implement the equals method, which tends to lead to
     * incorrect results and unnecessary work.
     * <p></p>
     * WARNING: This method will not work with collections which use iterators that return its
     * elements in an inconsistent order. The order does not need to be known, but it must be
     * consistent and repeatable for a given collection state.
     *
     * @param c1
     *  A collection to compare to c2
     *
     * @param c2
     *  A collection to compare to c1
     *
     * @return
     *  true if both collections are the same instance, are both null or contain the same elements;
     *  false otherwise
     */
    public static <T> boolean collectionsAreEqual(Collection<T> c1, Collection<T> c2) {
        if (c1 == c2) {
            return true;
        }

        if (c1 == null || c2 == null || c1.size() != c2.size()) {
            return false;
        }

        Set<Integer> indexes = new HashSet<>();
        for (T lhs : c1) {
            boolean found = false;
            int offset = -1;

            for (T rhs : c2) {
                if (indexes.contains(++offset)) {
                    continue;
                }

                if (lhs == rhs || (lhs != null && lhs.equals(rhs))) {
                    indexes.add(offset);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares two collections for equality without using the collection's equals method. This is
     * primarily only useful when working with collections that may actually be Hibernate bags, as
     * bags and proxies do not properly implement the equals method, which tends to lead to
     * incorrect results and unnecessary work.
     * <p></p>
     * WARNING: This method will not work with collections which use iterators that return its
     * elements in an inconsistent order. The order does not need to be known, but it must be
     * consistent and repeatable for a given collection state.
     *
     * @param c1
     *  A collection to compare to c2
     *
     * @param c2
     *  A collection to compare to c1
     *
     * @param comp
     *  A comparator to use to compare elements of c1 and c2 for equality
     *
     * @throws IllegalArgumentException
     *  if the provided compator is null
     *
     * @return
     *  true if both collections are the same instance, are both null or contain the same elements;
     *  false otherwise
     */
    public static <T> boolean collectionsAreEqual(Collection<T> c1, Collection<T> c2, Comparator<T> comp) {
        if (comp == null) {
            throw new IllegalArgumentException("comp is null");
        }

        if (c1 == c2) {
            return true;
        }

        if (c1 == null || c2 == null || c1.size() != c2.size()) {
            return false;
        }

        Set<Integer> indexes = new HashSet<>();
        for (T lhs : c1) {
            boolean found = false;
            int offset = -1;

            for (T rhs : c2) {
                if (indexes.contains(++offset)) {
                    continue;
                }

                if (lhs == rhs || (lhs != null && comp.compare(lhs, rhs) == 0)) {
                    indexes.add(offset);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fetches the hostname for this system without going through the network stack and DNS
     *
     * @return
     *  the hostname of this system
     */
    public static String getHostname() {
        try {
            Field implField = InetAddress.class.getDeclaredField("impl");
            implField.setAccessible(true);

            Object impl = implField.get(null);
            Method method = impl.getClass().getDeclaredMethod("getLocalHostName");
            method.setAccessible(true);

            return (String) method.invoke(impl);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the first non-null non-empty value from the given array

     * @param values values to be searched
     * @return first non-null non-empty value or null
     */
    public static String firstOf(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return null;
    }

    /**
     * Split a given string and return in as a List
     * @param list a string to be split
     * @return a list of values
     */
    public static List<String> toList(String list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(list.split(","));
    }

}
