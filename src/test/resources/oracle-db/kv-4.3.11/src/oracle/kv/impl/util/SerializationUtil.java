/*-
 * Copyright (C) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This file was distributed by Oracle as part of a version of Oracle NoSQL
 * Database made available at:
 *
 * http://www.oracle.com/technetwork/database/database-technologies/nosqldb/downloads/index.html
 *
 * Please see the LICENSE file included in the top-level directory of the
 * appropriate version of Oracle NoSQL Database for a copy of the license and
 * additional information.
 */

package oracle.kv.impl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.sleepycat.util.PackedInteger;

/**
 * Utility methods to facilitate serialization/deserialization
 */
public class SerializationUtil {

    public static final String EMPTY_STRING = new String();

    /**
     * Returns the object de-serialized from the specified byte array.
     *
     * @param <T> the type of the deserialized object
     *
     * @param bytes the serialized bytes
     * @param oclass the class associated with the deserialized object
     *
     * @return the deserialized object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObject(byte[] bytes, Class<T> oclass) {

        if (bytes == null) {
            return null;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (T)ois.readObject();
        } catch (IOException ioe) {
            throw new IllegalStateException
                ("Exception deserializing object: " + oclass.getName(), ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException
            ("Exception deserializing object: " + oclass.getName(), cnfe);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ioe) {
                    throw new IllegalStateException
                        ("Exception closing deserializing stream", ioe);
                }
            }
        }
    }

    /**
     * Returns a byte array containing the serialized form of this object.
     *
     * @param object the object to be serialized
     *
     * @return the serialized bytes
     */
    public static byte[] getBytes(Object object) {
        ObjectOutputStream oos = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalStateException
                ("Exception serializing object: " +
                 object.getClass().getName(),
                 ioe);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ioe) {
                    throw new IllegalStateException
                        ("Exception closing serializing stream", ioe);
                }
            }
        }
    }

    /**
     * Reads a packed integer from the input and returns it.
     *
     * @param in the data input
     * @return the integer that was read
     */
    public static int readPackedInt(DataInput in) throws IOException {
        final byte[] bytes = new byte[PackedInteger.MAX_LENGTH];
        in.readFully(bytes, 0, 1);
        final int len = PackedInteger.getReadIntLength(bytes, 0);
        in.readFully(bytes, 1, len - 1);
        return PackedInteger.readInt(bytes, 0);
    }

    /**
     * Writes a packed integer to the output.
     *
     * @param out the data output
     * @param value the integer to be written
     */
    public static void writePackedInt(DataOutput out, int value)
            throws IOException {
        final byte[] buf = new byte[PackedInteger.MAX_LENGTH];
        final int offset = PackedInteger.writeInt(buf, 0, value);
        out.write(buf, 0, offset);
    }

    /**
     * Reads a packed long from the input and returns it.
     *
     * @param in the data input
     * @return the long that was read
     */
    public static long readPackedLong(DataInput in) throws IOException {
        final byte[] bytes = new byte[PackedInteger.MAX_LONG_LENGTH];
        in.readFully(bytes, 0, 1);
        final int len = PackedInteger.getReadLongLength(bytes, 0);
        in.readFully(bytes, 1, len - 1);
        return PackedInteger.readLong(bytes, 0);
    }

    /**
     * Writes a packed long to the output.
     *
     * @param out the data output
     * @param value the long to be written
     */
    public static void writePackedLong(DataOutput out, long value)
            throws IOException {
        final byte[] buf = new byte[PackedInteger.MAX_LONG_LENGTH];
        final int offset = PackedInteger.writeLong(buf, 0, value);
        out.write(buf, 0, offset);
    }

    /**
     * Reads a string written by writeString. The returned string will be null
     * if the original string was null.
     *
     * @param in the data input
     * @return a string or null
     */
    public static String readString(DataInput in) throws IOException {
        final int len = readPackedInt(in);
        if (len == -1) {
            /* -1 means the original string was null */
            return null;
        }

        byte[] bytes = new byte[len];
        if (len > 0) {
            in.readFully(bytes);
        }

        return utfToString(bytes);
    }

    /**
     * Serializes a string which can be read by readString. The string may be
     * null or empty. This code differentiates between the two, maintaining
     * the ability to round-trip null and empty string values.
     *
     * This format is used rather than that of DataOutput.writeUTF() to allow
     * packing of the size of the string. For shorter strings this size
     * savings is a significant percentage of the space used.
     *
     * @param out the data output
     * @param value the string to serialize or null
     */
    public static void writeString(DataOutput out, String value)
            throws IOException {

        if (value == null) {
            writePackedInt(out, -1); /* -1 is encoded in a single byte */
            return;
        }

        final int utfLength = getUTFLength(value);

        writePackedInt(out, utfLength);

        if (utfLength > 0) {
            final byte[] bytes = stringToUTF(value, utfLength);
            out.write(bytes);
        }
    }

    /* -- The following methods are cribbed from JE's UtfOps and the JDK -- */

    /*
     * Gets the length of a UTF-8 byte array representation of the specified
     * string
     */
    private static int getUTFLength(String s) {
        int len = 0;
        final int length = s.length();
        for (int i = 0; i < length; i++) {
            int c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                len++;
            } else if (c > 0x07FF) {
                len += 3;
            } else {
                len += 2;
            }
        }
        return len;
    }

    /**
     * Converts the specified string into UTF-8 format
     */
    private static byte[] stringToUTF(String s, int utfLength) {

        final int length = s.length();
        assert length > 0;

        final byte[] bytes = new byte[utfLength];
        int byteOffset = 0;
        int i = 0;

        /* optimized loop for ascii */
        while (i < length) {
            int c = s.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) {
                break;
            }
            i++;
            bytes[byteOffset++] = (byte) c;
        }

        while (i < length) {
            int c = s.charAt(i++);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytes[byteOffset++] = (byte) c;
            } else if (c > 0x07FF) {
                bytes[byteOffset++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytes[byteOffset++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytes[byteOffset++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                bytes[byteOffset++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytes[byteOffset++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        return bytes;
    }

    /**
     * Converts byte arrays encoded as UTF-8 into strings. It converts as
     * many bytes as are in the bytes array. If offsets are ever needed, this
     * can be added.
     */
    private static String utfToString(byte[] bytes)
        throws IllegalArgumentException, IndexOutOfBoundsException {

        final int len = bytes.length;

        if (len == 0) {
            return EMPTY_STRING;
        }

        /*
         * this assumes that the number of chars in the final string is
         * <= the number of encoded bytes, which is true for UTF-8 encodings.
         * This assumption saves counting twice.
         */
        char[] chars = new char[len];
        int count = 0;
        int charArrayCount = 0;
        int char1, char2, char3;

        /*
         * Optimize for ascii
         */
        while (count < len) {
            char1 = bytes[count] & 0xff;
            if (char1 > 127) {
                break;
            }
            count++;
            chars[charArrayCount++] = (char) char1;
        }

        while (count < len) {
            char1 = bytes[count++] & 0xff;
            switch (char1 >> 4) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                /* 0xxxxxxx */
                chars[charArrayCount++] = (char) char1;
                break;
            case 12: case 13:
                /* 110x xxxx   10xx xxxx*/
                char2 = bytes[count++];
                if ((char2 & 0xC0) != 0x80) {
                    throw new IllegalArgumentException();
                }
                chars[charArrayCount++] = (char)(((char1 & 0x1F) << 6) |
                                                 (char2 & 0x3F));
                break;
            case 14:
                char2 = bytes[count++];
                char3 = bytes[count++];
                if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                    throw new IllegalArgumentException();
                }
                chars[charArrayCount++] = (char)(((char1 & 0x0F) << 12) |
                                                 ((char2 & 0x3F) << 6)  |
                                                 ((char3 & 0x3F) << 0));
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
        return new String(chars, 0, charArrayCount);
    }
}
