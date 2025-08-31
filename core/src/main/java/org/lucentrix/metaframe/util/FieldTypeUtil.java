package org.lucentrix.metaframe.util;

import lombok.Builder;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.StringObjectMap;
import org.lucentrix.metaframe.metadata.StringValueMap;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.value.FieldValue;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Builder
@ToString
public class FieldTypeUtil {


    public static ArrayList<Boolean> parseBooleans(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Boolean> booleans = new ArrayList<>();
            for (Object element : (Collection<?>) obj) {
                booleans.add(parseBoolean(element));
            }

            return booleans;
        } else if (obj instanceof boolean[]) {
            ArrayList<Boolean> booleans = new ArrayList<>();
            for (boolean element : (boolean[]) obj) {
                booleans.add(parseBoolean(element));
            }

            return booleans;
        } else if (obj instanceof Boolean[]) {
            ArrayList<Boolean> booleans = new ArrayList<>();
            Collections.addAll(booleans, (Boolean[]) obj);

            return booleans;
        } else if (obj instanceof Object[]) {
            ArrayList<Boolean> booleans = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                booleans.add(parseBoolean(element));
            }

            return booleans;
        } else if (obj instanceof String) {
            ArrayList<Boolean> booleans = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    booleans.add(parseBoolean(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    booleans.add(parseBoolean(strElement));
                }
            } else {
                booleans.add(parseBoolean(strValue));
            }

            return booleans;
        } else {
            ArrayList<Boolean> booleans = new ArrayList<>();
            booleans.add(parseBoolean(obj));

            return booleans;
        }
    }

    public static String parseString(Object obj) {
        return obj == null ? null : String.valueOf(obj);
    }

    public static List<String> parseStrings(Object obj) {
        List<String> value = new ArrayList<>();

        if (obj instanceof Collection) {
            for (Object item : (Collection) obj) {
                if (item instanceof String) {
                    value.add((String) item);
                } else {
                    value.add(item == null ? null : String.valueOf(item));
                }
            }
        } else if (obj instanceof String[]) {
            Collections.addAll(value, (String[]) obj);
        } else if (obj instanceof Object[]) {
            for (Object item : (Object[]) obj) {
                if (item instanceof String) {
                    value.add((String) item);
                } else {
                    value.add(item == null ? null : String.valueOf(item));
                }
            }
        } else if (obj instanceof String) {
            value.add((String) obj);
        } else {
            value.add(obj == null ? null : String.valueOf(obj));
        }

        return value;
    }

    public static Set<String> parseStringSet(Object obj) {
        List<String> strings = parseStrings(obj);
        return new HashSet<>(strings);
    }

    public static Boolean parseBoolean(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        } else {
            String boolStr = String.valueOf(obj);
            if (boolStr.length() == 1) {
                return !"F".equalsIgnoreCase(boolStr) && !"0".equalsIgnoreCase(boolStr);
            }
            try {
                return Boolean.parseBoolean(boolStr);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing Boolean: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static ArrayList<byte[]> parseByteArrays(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<byte[]> arrays = new ArrayList<>();
            for (Object element : (Collection) obj) {
                arrays.add(parseByteArray(element));
            }

            return arrays;
        } else if (obj instanceof byte[][]) {
            ArrayList<byte[]> arrays = new ArrayList<>();
            for (Object element : (byte[][]) obj) {
                arrays.add(parseByteArray(element));
            }

            return arrays;
        } else if (obj instanceof Object[][]) {
            ArrayList<byte[]> arrays = new ArrayList<>();
            for (Object element : (Object[][]) obj) {
                arrays.add(parseByteArray(element));
            }

            return arrays;
        } else if (obj instanceof String) {
            ArrayList<byte[]> arrays = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    arrays.add(parseByteArray(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    arrays.add(parseByteArray(strElement));
                }
            } else {
                arrays.add(parseByteArray(strValue));
            }

            return arrays;
        } else {
            ArrayList<byte[]> arrays = new ArrayList<>();
            arrays.add(parseByteArray(obj));

            return arrays;
        }
    }

    public static byte[] parseByteArray(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof byte[]) {
            return (byte[]) obj;
        } else if (obj instanceof Number[]) {
            Number[] objects = (Number[]) obj;
            byte[] bytes = new byte[objects.length];
            for (int i = 0; i < objects.length; i++) {
                bytes[i] = objects[i].byteValue();
            }
            return bytes;
        } else if (obj instanceof Object[]) {
            Object[] objects = (Object[]) obj;
            byte[] bytes = new byte[objects.length];
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];

                if (object instanceof Number) {
                    bytes[i] = ((Number) object).byteValue();
                }
            }
            return bytes;
        } else {
            try {
                return Base64.getDecoder().decode(String.valueOf(obj));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Value is not instance of Base64 String or byte[]: " + obj);
            }
        }
    }

    public static ArrayList<Instant> parseDates(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Instant> dates = new ArrayList<>();
            for (Object element : (Collection) obj) {
                dates.add(parseInstant(element));
            }

            return dates;
        } else if (obj instanceof Date[]) {
            ArrayList<Instant> dates = new ArrayList<>();
            for (Date element : (Date[]) obj) {
                dates.add(element.toInstant());
            }

            return dates;
        } else if (obj instanceof Instant[]) {
            ArrayList<Instant> dates = new ArrayList<>();
            Collections.addAll(dates, (Instant[]) obj);

            return dates;
        } else if (obj instanceof long[]) {
            ArrayList<Instant> dates = new ArrayList<>();
            for (long element : (long[]) obj) {
                dates.add(Instant.ofEpochMilli(element));
            }

            return dates;
        } else if (obj instanceof Long[]) {
            ArrayList<Instant> dates = new ArrayList<>();
            for (Long element : (Long[]) obj) {
                dates.add(Instant.ofEpochMilli(element));
            }

            return dates;
        } else if (obj instanceof Object[]) {
            ArrayList<Instant> dates = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                dates.add(parseInstant(element));
            }

            return dates;
        } else if (obj instanceof String) {
            ArrayList<Instant> dates = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    dates.add(parseInstant(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    dates.add(parseInstant(strElement));
                }
            } else {
                dates.add(parseInstant(strValue));
            }

            return dates;
        } else {
            ArrayList<Instant> dates = new ArrayList<>();
            dates.add(parseInstant(obj));
            return dates;
        }
    }

    public static List<FieldObjectMap> parseFieldObjectMaps(Object obj) {
        if (obj == null) {
            return null;
        }

        List<FieldObjectMap> fieldsList = new ArrayList<>();

        if (obj instanceof Collection) {
            Collection objCollection = (Collection) obj;
            for (Object item : objCollection) {
                FieldObjectMap objectValue = null;
                if (item != null) {
                    objectValue = parseFieldObjectMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else if (obj instanceof Object[]) {
            Object[] objArray = (Object[]) obj;
            for (Object item : objArray) {
                FieldObjectMap objectValue = null;
                if (item != null) {
                    objectValue = parseFieldObjectMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else {
            FieldObjectMap objectValue = parseFieldObjectMap(obj);
            fieldsList.add(objectValue);
        }

        return fieldsList;
    }

    public static FieldObjectMap parseFieldObjectMap(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof FieldObjectMap) {
            return (FieldObjectMap) obj;
        }

        Map<Field<?>, Object> fields = new LinkedHashMap<>();

        if (obj instanceof Map) {
            Map map = (Map) obj;
            for (Object key : map.keySet()) {
                Object mapValue = map.get(key);
                Field<?> field;
                if (key instanceof Field) {
                    field = (Field<?>) key;
                    Object fieldValue = field.getType().parse(mapValue);
                    fields.put(field, fieldValue);
                } else {
                    throw new IllegalArgumentException("Unsupported key type in field map, must be instance of "
                            + Field.class.getName() + " class: " + key.getClass());
                }
            }
        } else {
            throw new RuntimeException("Incompatible object field value : " + obj);
        }

        return new FieldObjectMap(fields);
    }

    public static List<StringObjectMap> parseStringObjectMaps(Object obj) {
        if (obj == null) {
            return null;
        }

        List<StringObjectMap> fieldsList = new ArrayList<>();

        if (obj instanceof Collection) {
            Collection objCollection = (Collection) obj;
            for (Object item : objCollection) {
                StringObjectMap objectValue = null;
                if (item != null) {
                    objectValue = parseStringObjectMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else if (obj instanceof Object[]) {
            Object[] objArray = (Object[]) obj;
            for (Object item : objArray) {
                StringObjectMap objectValue = null;
                if (item != null) {
                    objectValue = parseStringObjectMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else {
            StringObjectMap objectValue = parseStringObjectMap(obj);
            fieldsList.add(objectValue);
        }

        return fieldsList;
    }

    public static StringObjectMap parseStringObjectMap(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof StringObjectMap) {
            return (StringObjectMap) obj;
        }

        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        if (obj instanceof Map) {
            Map map = (Map) obj;
            for (Object key : map.keySet()) {
                Object mapValue = map.get(key);
                String field;
                if (key instanceof String) {
                    field = (String) key;
                    fields.put(field, mapValue);
                } else {
                    throw new IllegalArgumentException("Unsupported key type in field map, must be instance of "
                            + Field.class.getName() + " class: " + key.getClass());
                }
            }
        } else {
            throw new RuntimeException("Incompatible object field value : " + obj);
        }

        return new StringObjectMap(fields);
    }

    public static List<StringValueMap> parseStringValueMaps(Object obj) {
        if (obj == null) {
            return null;
        }

        List<StringValueMap> fieldsList = new ArrayList<>();

        if (obj instanceof Collection) {
            Collection objCollection = (Collection) obj;
            for (Object item : objCollection) {
                StringValueMap objectValue = null;
                if (item != null) {
                    objectValue = parseStringValueMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else if (obj instanceof Object[]) {
            Object[] objArray = (Object[]) obj;
            for (Object item : objArray) {
                StringValueMap objectValue = null;
                if (item != null) {
                    objectValue = parseStringValueMap(item);
                }
                fieldsList.add(objectValue);
            }
        } else {
            StringValueMap objectValue = parseStringValueMap(obj);
            fieldsList.add(objectValue);
        }

        return fieldsList;
    }

    public static StringValueMap parseStringValueMap(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof StringValueMap) {
            return (StringValueMap) obj;
        }

        Map<String, FieldValue<?>> fields = new LinkedHashMap<>();

        if (obj instanceof Map) {
            Map map = (Map) obj;
            for (Object key : map.keySet()) {
                Object mapValue = map.get(key);
                FieldValue<?> fieldValue;
                if (mapValue instanceof FieldValue) {
                    fieldValue = (FieldValue<?>) mapValue;
                } else {
                    throw new IllegalArgumentException("Unsupported value type in field map, must be instance of "
                            + FieldValue.class.getName() + " value: " + mapValue);
                }
                String field;
                if (key instanceof String) {
                    field = (String) key;
                    fields.put(field, fieldValue);
                } else {
                    throw new IllegalArgumentException("Unsupported key type in field map, must be instance of "
                            + Field.class.getName() + " class: " + key.getClass());
                }
            }
        } else {
            throw new RuntimeException("Incompatible object field value : " + obj);
        }

        return new StringValueMap(fields);
    }

    public static Instant parseInstant(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Instant) {
            return (Instant) obj;
        } else if (obj instanceof Date) {
            return ((Date) obj).toInstant();
        } else if (obj instanceof TemporalAccessor) {
            return Instant.from((TemporalAccessor) obj);
        } else if (obj instanceof XMLGregorianCalendar) {
            return ((XMLGregorianCalendar) obj).toGregorianCalendar().toInstant().atOffset(ZoneOffset.UTC).toInstant();
        } else if (obj instanceof Long) {
            return Instant.ofEpochMilli((long) obj);
        } else {
            //Zulu format
            String dateAsString = obj.toString();
            try {
                return Instant.parse(dateAsString);
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing instant from string: " + dateAsString, ex);
            }

        }
    }

    public static ArrayList<Float> parseFloats(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Float> floats = new ArrayList<>();
            for (Object element : (Collection) obj) {
                floats.add(parseFloat(element));
            }

            return floats;
        } else if (obj instanceof float[]) {
            ArrayList<Float> floats = new ArrayList<>();
            for (float element : (float[]) obj) {
                floats.add(element);
            }

            return floats;
        } else if (obj instanceof Float[]) {
            ArrayList<Float> floats = new ArrayList<>();
            Collections.addAll(floats, (Float[]) obj);

            return floats;
        } else if (obj instanceof Number[]) {
            ArrayList<Float> floats = new ArrayList<>();
            for (Number element : (Number[]) obj) {
                floats.add(parseFloat(element));
            }

            return floats;
        } else if (obj instanceof Object[]) {
            ArrayList<Float> floats = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                floats.add(parseFloat(element));
            }

            return floats;
        } else if (obj instanceof String) {
            ArrayList<Float> floats = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    floats.add(parseFloat(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    floats.add(parseFloat(strElement));
                }
            } else {
                floats.add(parseFloat(strValue));
            }

            return floats;
        } else {
            ArrayList<Float> floats = new ArrayList<>();
            floats.add(parseFloat(obj));
            return floats;
        }
    }

    public static ArrayList<Double> parseDoubles(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Double> doubles = new ArrayList<>();
            for (Object element : (Collection) obj) {
                doubles.add(parseDouble(element));
            }

            return doubles;
        } else if (obj instanceof double[]) {
            ArrayList<Double> doubles = new ArrayList<>();
            for (double element : (double[]) obj) {
                doubles.add(element);
            }

            return doubles;
        } else if (obj instanceof Double[]) {
            ArrayList<Double> doubles = new ArrayList<>();
            Collections.addAll(doubles, (Double[]) obj);

            return doubles;
        } else if (obj instanceof Number[]) {
            ArrayList<Double> doubles = new ArrayList<>();
            for (Number element : (Number[]) obj) {
                doubles.add(parseDouble(element));
            }

            return doubles;
        } else if (obj instanceof Object[]) {
            ArrayList<Double> doubles = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                doubles.add(parseDouble(element));
            }

            return doubles;
        } else if (obj instanceof String) {
            ArrayList<Double> doubles = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    doubles.add(parseDouble(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    doubles.add(parseDouble(strElement));
                }
            } else {
                doubles.add(parseDouble(strValue));
            }

            return doubles;
        } else {
            ArrayList<Double> doubles = new ArrayList<>();
            doubles.add(parseDouble(obj));
            return doubles;
        }
    }

    public static Float parseFloat(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Float) {
            return (Float) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        } else {
            try {
                return Float.parseFloat(String.valueOf(obj).replace(',', '.'));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing Float value: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static Double parseDouble(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else {
            try {
                return Double.parseDouble(String.valueOf(obj).replace(',', '.'));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing Double value: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static ArrayList<Integer> parseIntegers(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Integer> ints = new ArrayList<>();
            for (Object element : (Collection) obj) {
                ints.add(parseInteger(element));
            }

            return ints;
        } else if (obj instanceof int[]) {
            ArrayList<Integer> ints = new ArrayList<>();
            for (int element : (int[]) obj) {
                ints.add(element);
            }

            return ints;
        } else if (obj instanceof Integer[]) {
            ArrayList<Integer> ints = new ArrayList<>();
            Collections.addAll(ints, (Integer[]) obj);

            return ints;
        } else if (obj instanceof Number[]) {
            ArrayList<Integer> ints = new ArrayList<>();
            for (Number element : (Number[]) obj) {
                ints.add(parseInteger(element));
            }

            return ints;
        } else if (obj instanceof Object[]) {
            ArrayList<Integer> ints = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                ints.add(parseInteger(element));
            }

            return ints;
        } else if (obj instanceof String) {
            ArrayList<Integer> ints = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    ints.add(parseInteger(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    ints.add(parseInteger(strElement));
                }
            } else {
                ints.add(parseInteger(strValue));
            }

            return ints;
        } else {
            ArrayList<Integer> ints = new ArrayList<>();
            ints.add(parseInteger(obj));

            return ints;
        }
    }

    public static Integer parseInteger(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else {
            try {
                return Integer.parseInt(String.valueOf(obj));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing Integer value: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static ArrayList<Long> parseLongs(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<Long> longs = new ArrayList<>();
            for (Object element : (Collection) obj) {
                longs.add(parseLong(element));
            }

            return longs;
        } else if (obj instanceof long[]) {
            ArrayList<Long> longs = new ArrayList<>();
            for (long element : (long[]) obj) {
                longs.add(element);
            }

            return longs;
        } else if (obj instanceof Long[]) {
            ArrayList<Long> longs = new ArrayList<>();
            Collections.addAll(longs, (Long[]) obj);

            return longs;
        } else if (obj instanceof Number[]) {
            ArrayList<Long> longs = new ArrayList<>();
            for (Number element : (Number[]) obj) {
                longs.add(element.longValue());
            }

            return longs;
        } else if (obj instanceof Object[]) {
            ArrayList<Long> longs = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                longs.add(parseLong(element));
            }

            return longs;
        } else if (obj instanceof String) {
            ArrayList<Long> longs = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    longs.add(parseLong(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    longs.add(parseLong(strElement));
                }
            } else {
                longs.add(parseLong(strValue));
            }

            return longs;
        } else {
            ArrayList<Long> longs = new ArrayList<>();
            longs.add(parseLong(obj));

            return longs;
        }
    }

    public static Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            try {
                return Long.parseLong(String.valueOf(obj));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing Long value: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static ArrayList<UUID> parseUUIDs(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Collection) {
            ArrayList<UUID> uuids = new ArrayList<>();
            for (Object element : (Collection) obj) {
                uuids.add(parseUUID(element));
            }

            return uuids;
        } else if (obj instanceof UUID[]) {
            ArrayList<UUID> uuids = new ArrayList<>();
            Collections.addAll(uuids, (UUID[]) obj);

            return uuids;
        } else if (obj instanceof Object[]) {
            ArrayList<UUID> uuids = new ArrayList<>();
            for (Object element : (Object[]) obj) {
                uuids.add(parseUUID(element));
            }

            return uuids;
        } else if (obj instanceof String) {
            ArrayList<UUID> uuids = new ArrayList<>();

            String strValue = String.valueOf(obj);
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                String[] strValues = parseArrayString(strValue);
                for (String strElement : strValues) {
                    uuids.add(parseUUID(strElement));
                }
            } else if (strValue.contains(",")) {
                String[] strValues = strValue.split(",");
                for (String strElement : strValues) {
                    uuids.add(parseUUID(strElement));
                }
            } else {
                uuids.add(parseUUID(strValue));
            }

            return uuids;
        } else {
            ArrayList<UUID> uuids = new ArrayList<>();
            uuids.add(parseUUID(obj));

            return uuids;
        }
    }

    public static UUID parseUUID(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof UUID) {
            return (UUID) obj;
        } else {
            try {
                return java.util.UUID.fromString(String.valueOf(obj));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Error parsing UUID value: " + obj + "\n Details: " + ex);
            }
        }
    }

    public static String[] parseArrayString(String arr) {
        if (StringUtils.isBlank(arr)) {
            return new String[]{arr};
        }

        return arr.replaceAll("\\[", "").replaceAll("\\]", "")
                .replaceAll("\\s", "").split(",");
    }
}
