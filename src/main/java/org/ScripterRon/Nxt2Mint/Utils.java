/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Mint;

import org.ScripterRon.JSON.JSONAware;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility routines
 */
public class Utils {

    /** Hex conversion alphabet */
    private static final char[] hexChars = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

    /**
     * Encode an integer as a variable-length byte stream
     *
     * @param       value                   Value to encode
     * @return                              Encoded byte stream
     */
    public static byte[] encodeInteger(int value) {
        byte[] bytes;
        if ((value&0xFFFF0000) != 0) {
            // 1 marker + 4 data bytes
            bytes = new byte[] {(byte)254, (byte)value, (byte)(value>>8), (byte)(value>>16), (byte)(value>>24)};
        } else if (value >= 253) {
            // 1 marker + 2 data bytes
            bytes = new byte[]{(byte)253, (byte)value, (byte)(value>>8)};
        } else {
            // Single data byte
            bytes = new byte[]{(byte)value};
        }
        return bytes;
    }

    /**
     * Decode a variable-length encoded integer
     *
     * @param       inStream                Input stream
     * @return                              Decoded length
     * @throws      EOFException            End-of-data while processing serialized length
     * @throws      IOException             Unable to read input stream
     */
    public static int decodeInteger(InputStream inStream) throws EOFException, IOException {
        int value;
        int count;
        byte[] bytes = new byte[4];
        int ctrl = inStream.read();
        if (ctrl < 0)
            throw new EOFException("End-of-data while processing encoded integer");
        switch (ctrl) {
            case 254:
                // 1 marker + 4 data bytes
                count = inStream.read(bytes, 0, 4);
                if (count != 4)
                    throw new EOFException("End-of-data while processing encoded integer");
                value = (int)bytes[0]&0xff + (((int)bytes[1]&0xff)<<8) + (((int)bytes[2]&0xff)<<16) +
                                         (((int)bytes[3]&0xff)<<24);
                break;
            case 253:
                // 1 marker + 2 data bytes
                count = inStream.read(bytes, 0, 2);
                if (count != 2)
                    throw new EOFException("End-of-data while processing encoded integer");
                value = (int)bytes[0]&0xff + (((int)bytes[1]&0xff)<<8);
                break;
            default:
                // Single data byte
                value = ctrl;
        }
        return value;
    }

    /**
     * Convert from NQT to a decimal string (1 NQT = 0.00000001 NXT)
     * We will keep at least 4 decimal places in the result.
     *
     * @param       value           Value to be converted
     * @return                      A formatted decimal string
     */
    public static String nqtToString(long value) {
        //
        // Format the amount
        //
        long bvalue = value;
        boolean negative = (bvalue < 0);
        if (negative)
            bvalue = -bvalue;
        //
        // Get the amount as a formatted string with 8 decimal places
        //
        String valueString = String.format("%09d", bvalue);
        int decimalPoint = valueString.length() - 8;
        //
        // Drop tailing zeros beyond 4 decimal places
        //
        int toDelete = 0;
        for (int i=valueString.length()-1; i>decimalPoint+3; i--) {
            if (valueString.charAt(i) != '0')
                break;
            toDelete++;
        }
        //
        // Create the formatted decimal string
        //
        StringBuilder formatted = new StringBuilder(valueString.substring(0, valueString.length()-toDelete));
        formatted.insert(decimalPoint, '.');
        //
        // Insert commas as needed
        //
        int index = decimalPoint;
        while (index > 3) {
            index -= 3;
            formatted.insert(index, ',');
        }
        //
        // Add the sign if the value is negative
        //
        if (negative)
            formatted.insert(0, '-');
        return formatted.toString();
    }

    /**
     * Convert a decimal string to NQT (1 NQT = 0.00000001 NXT)
     *
     * @param       value           String to be converted
     * @return                      Amount expressed in NQT
     */
    public static long stringToNQT(String value) throws NumberFormatException {
        if (value == null)
            throw new IllegalArgumentException("No string value provided");
        if (value.isEmpty())
            return 0;
        BigDecimal decValue = new BigDecimal(value);
        return decValue.scaleByPowerOfTen(8).longValue();
    }

    /**
     * Get the account identifier associated with a public key
     *
     * @param       publicKey               Public key
     * @return                              Account identifier
     */
    public static long getAccountId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.singleDigest(publicKey);
        return fullHashToId(publicKeyHash);
    }

    /**
     * Convert a string to an object identifier
     *
     * @param       number                  Object identifier string
     * @return                              Object identifier
     * @throws      IdentifierException     Invalid object identifier
     */
    public static long stringToId(String number) throws IdentifierException {
        long id;
        try {
            id = number.length() != 0 ? Long.parseUnsignedLong(number) : 0;
        } catch (NumberFormatException exc) {
            throw new IdentifierException("Invalid object identifier '" + number + "'", exc);
        }
        return id;
    }

    /**
     * Convert a full hash to an object identifier using the first 8 bytes of the hash
     *
     * @param       hash                    Full hash
     * @return                              Object identifier
     */
    public static long fullHashToId(byte[] hash) {
        if (hash == null || hash.length < 8)
            throw new IllegalArgumentException("Invalid hash: " + Arrays.toString(hash));
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5],
                                                              hash[4], hash[3], hash[2],
                                                              hash[1], hash[0]});
        return bigInteger.longValue();
    }

    /**
     * Get the Reed-Solomon string for an account identifier
     *
     * @param       accountId               Account identifier
     * @return                              Reed-Solomon account identifier string
     */
    public static String getAccountRsId(long accountId) {
        return "NXT-" + ReedSolomon.encode(accountId);
    }

    /**
     * Parse the Reed-Solomon account identifier string and return the account identifier
     *
     * @param       accountRsId             Reed-Solomon account string
     * @return                              Account identifier
     * @throws      IdentifierException     Invalid account identifier string
     */
    public static long parseAccountRsId(String accountRsId) throws IdentifierException {
        if (!accountRsId.startsWith("NXT-"))
            throw new IdentifierException("Invalid Reed-Solomon Nxt account identifier '"
                    + accountRsId + "'");
        return ReedSolomon.decode(accountRsId.substring(4));
    }

    /**
     * Convert an object identifier to a string
     *
     * @param       id                      Object identifier
     * @return                              Identifier string
     */
    public static String idToString(long id) {
        return Long.toUnsignedString(id);
    }

    /**
     * Convert a byte array to a hex string
     *
     * @param       bytes                   Bytes to encode
     * @return                              Hex string
     */
    public static String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = hexChars[((bytes[i] >> 4) & 0xF)];
            chars[i * 2 + 1] = hexChars[(bytes[i] & 0xF)];
        }
        return String.valueOf(chars);
    }

    /**
     * Parse a hex string and return the decoded bytes
     *
     * @param       hex                     String to parse
     * @return                              Decoded bytes
     * @throws      NumberFormatException   String contains an invalid hex character
     */
    public static byte[] parseHexString(String hex) throws NumberFormatException {
        try {
            if ((hex.length()&0x01) == 1)
                throw new NumberFormatException("Hex string length is not a multiple of 2");
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int char1 = hex.charAt(i * 2);
                char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
                int char2 = hex.charAt(i * 2 + 1);
                char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
                if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15)
                    throw new NumberFormatException("Invalid hex number: " + hex);
                bytes[i] = (byte)((char1 << 4) + char2);
            }
            return bytes;
        } catch (Exception exc) {
            Main.log.debug("Invalid hex string: '" + hex + "'");
            throw exc;
        }
    }

    /**
     * Create a formatted string for a JSON array
     *
     * @param       array                   JSON array
     * @return                              Formatted string
     */
    public static String formatJSON(List<Object> array) {
        StringBuilder builder = new StringBuilder(512);
        formatJSON(builder, "", array);
        return new String(builder);
    }

    /**
     * Create a formatted string for a JSON object
     *
     * @param       map                     JSON object
     * @return                              Formatted string
     */
    public static String formatJSON(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder(512);
        formatJSON(builder, "", map);
        return new String(builder);
    }

    /**
     * Create a formatted string for a JSON structure
     *
     * @param       builder                 String builder
     * @param       indent                  Output indentation
     * @param       object                  The JSON object
     */
    @SuppressWarnings("unchecked")
    private static void formatJSON(StringBuilder builder, String indent, Object object) {
        String itemIndent = indent+"  ";
        if (object instanceof List) {
            List<Object> array = (List<Object>)object;
            builder.append(indent).append("[\n");
            array.forEach((value) -> {
                if (value == null) {
                    builder.append(itemIndent).append("null").append('\n');
                } else if (value instanceof Boolean) {
                    builder.append(itemIndent).append((Boolean)value ? "true\n" : "false\n");
                } else if (value instanceof Long) {
                    builder.append(itemIndent).append(((Long)value).toString()).append('\n');
                } else if (value instanceof Double) {
                    builder.append(itemIndent).append(((Double)value).toString()).append('\n');
                } else if (value instanceof String) {
                    builder.append(itemIndent).append('"').append((String)value).append("\"\n");
                } else if ((value instanceof List) || (value instanceof Map)) {
                    formatJSON(builder, itemIndent, (JSONAware)value);
                } else {
                    builder.append(itemIndent).append("Unknown\n");
                }
            });
            builder.append(indent).append("]\n");
        } else {
            builder.append(indent).append("{\n");
            Map<String, Object> map = (Map<String, Object>)object;
            Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                builder.append(itemIndent).append("\"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value == null) {
                    builder.append("null").append('\n');
                } else if (value instanceof Boolean) {
                    builder.append((Boolean)value ? "true\n" : "false\n");
                } else if (value instanceof Long) {
                    builder.append(((Long)value).toString()).append('\n');
                } else if (value instanceof Double) {
                    builder.append(((Double)value).toString()).append('\n');
                } else if (value instanceof String) {
                    builder.append('"').append((String)value).append("\"\n");
                } else if (value instanceof JSONAware) {
                    builder.append('\n');
                    formatJSON(builder, itemIndent+"  ", (JSONAware)value);
                } else {
                    builder.append("Unknown\n");
                }
            }
            builder.append(indent).append("}\n");
        }
    }
}
