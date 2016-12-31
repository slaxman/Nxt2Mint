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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response is used for the JSON-encoded responses returned by the Nxt server
 */
@SuppressWarnings("unchecked")
public class Response {

    /** Empty string list */
    private static final List<String> emptyStringList = Collections.emptyList();

    /** Empty long list */
    private static final List<Long> emptyLongList = Collections.emptyList();

    /** Empty object list */
    private static final List<Map<String, Object>> emptyObjectList = Collections.emptyList();

    /** Empty object */
    private static final Map<String, Object> emptyObject = Collections.emptyMap();

    /** JSON object map */
    private final Map<String, Object> objectMap;

    /**
     * Create the peer response with an empty map
     */
    public Response() {
        objectMap = new HashMap<>();
    }

    /**
     * Create the peer response for the supplied JSON object map
     *
     * @param       map                     JSON object map
     */
    public Response(Map<String, Object> map) {
        objectMap = map;
    }

    /**
     * Return the JSON object map
     *
     * @return                              Object map
     */
    public Map<String, Object> getObjectMap() {
        return objectMap;
    }

    /**
     * Return an object map entry
     *
     * @param       key                     JSON key
     * @return                              Map value or null if the key is not found
     */
    public Object get(String key) {
        return objectMap.get(key);
    }

    /**
     * Return a boolean value
     *
     * @param       key                     JSON key
     * @return                              Boolean value (FALSE if key not found)
     */
    public boolean getBoolean(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof Boolean) ? (Boolean)value : false);
    }

    /**
     * Return a byte value
     *
     * @param       key                     JSON key
     * @return                              Byte value (0 if key not found)
     */
    public byte getByte(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof Long) ? (byte)((Long)value).intValue() : 0);
    }

    /**
     * Return a short value
     *
     * @param       key                     JSON key
     * @return                              Short value (0 if key not found)
     */
    public short getShort(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof Long) ? (short)((Long)value).intValue() : 0);
    }

    /**
     * Return an integer value
     *
     * @param       key                     JSON key
     * @return                              Integer value (0 if key not found)
     */
    public int getInt(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof Long) ? ((Long)value).intValue() : 0);
    }

    /**
     * Return a long value
     *
     * @param       key                     JSON key
     * @return                              Long value (0 if key not found)
     */
    public long getLong(String key) {
        long result;
        Object value = get(key);
        if (value instanceof Long) {
            result = (Long)value;
        } else if (value instanceof String) {
            try {
                result = Long.parseUnsignedLong((String)value);
            } catch (NumberFormatException exc) {
                result = 0;
            }
        } else {
            result = 0;
        }
        return result;
    }

    /**
     * Return a long value encoded as a string
     *
     * @param       key                     JSON key
     * @return                              Long value (0 if key not found)
     * @throws      NumberFormatException   Invalid decimal string
     */
    public long getLongString(String key) throws NumberFormatException {
        Object value = get(key);
        return (value!=null && (value instanceof String) && ((String)value).length()>0 ?
                Long.parseLong((String)value) : 0);
    }

    /**
     * Return a list of long values
     *
     * @param       key                     JSON key
     * @return                              List of long values (empty list if key not found)
     */
    public List<Long> getLongList(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof List) && !((List)value).isEmpty() &&
                (((List)value).get(0) instanceof Long) ? (List<Long>)value : emptyLongList);
    }

    /**
     * Return an object identifier
     *
     * @param       key                     JSON key
     * @return                              Object identifier (0 if key not found)
     * @throws      IdentifierException     Invalid object identifier
     */
    public long getId(String key) throws IdentifierException {
        Object value = get(key);
        return (value!=null && (value instanceof String) ? Utils.stringToId((String)value) : 0);
    }

    /**
     * Return a list of object identifiers
     *
     * @param       key                     JSON key
     * @return                              Identifier list (empty list if key not found)
     * @throws      IdentifierException     Invalid object identifier
     */
    public List<Long> getIdList(String key) throws IdentifierException {
        Object value = get(key);
        if (value == null || !(value instanceof List) || ((List)value).isEmpty() || !(((List)value).get(0) instanceof String))
            return emptyLongList;
        List<String> stringList = (List<String>)value;
        List<Long> longList = new ArrayList<>(stringList.size());
        for (String longString : stringList)
            longList.add(Utils.stringToId(longString));
        return longList;
    }

    /**
     * Return a string value
     *
     * @param       key                     JSON key
     * @return                              String value (empty string if key not found)
     */
    public String getString(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof String) ? (String)value : "");
    }

    /**
     * Return a hexadecimal byte value
     *
     * @param       key                     JSON key
     * @return                              Hexadecimal byte array (null if key not found)
     * @throws      NumberFormatException   Invalid hexadecimal string
     */
    public byte[] getHexString(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof String) ? Utils.parseHexString(((String)value).trim()) : null);
    }

    /**
     * Return a string list value
     *
     * @param       key                     JSON key
     * @return                              String list (empty list if key not found)
     */
    public List<String> getStringList(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof List) && !((List)value).isEmpty() &&
                (((List)value).get(0) instanceof String) ? (List<String>)value : emptyStringList);
    }

    /**
     * Return a JSON object
     *
     * @param       key                     JSON key
     * @return                              JSON object (empty map if key not found)
     */
    public Map<String, Object> getObject(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof Map) ? (Map<String, Object>)value : emptyObject);
    }

    /**
     * Return a list of JSON objects
     *
     * @param       key                     JSON key
     * @return                              Object list (empty list if key not found)
     */
    public List<Map<String, Object>> getObjectList(String key) {
        Object value = get(key);
        return (value!=null && (value instanceof List) && !((List)value).isEmpty() &&
                (((List)value).get(0) instanceof Map) ? (List<Map<String, Object>>)value : emptyObjectList);
    }
}
