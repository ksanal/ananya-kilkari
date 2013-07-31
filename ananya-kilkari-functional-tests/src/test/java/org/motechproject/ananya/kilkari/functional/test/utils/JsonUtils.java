package org.motechproject.ananya.kilkari.functional.test.utils;


import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;

import java.io.IOException;
import java.io.StringWriter;

@Ignore
public class JsonUtils {
    public static String toJson(Object objectToSerialize) {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        try {
            mapper.writeValue(stringWriter, objectToSerialize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    public static <T> T fromJson(String jsonString, Class<T> subscriberResponseClass) {
        ObjectMapper mapper = new ObjectMapper();
        T serializedObject = null;
        try {
            serializedObject = mapper.readValue(jsonString, subscriberResponseClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serializedObject;
    }

    public static <T> T fromJsonWithResponse(String jsonString, Class<T> subscriberResponseClass) {
        return fromJson(jsonString.replace("var response = ", ""), subscriberResponseClass);
    }
}
