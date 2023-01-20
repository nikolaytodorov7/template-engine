package app;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TemplateContext {
    private Map<String, Object> properties = new HashMap<>();

    public void put(String key, Object value) {
        properties.put(key, value);
    }

    public Object get(String key) { //todo recursive go inside instead of one step
        String[] templateSplit = key.split("\\.");
        Object templateObj = properties.get(templateSplit[0]);
        if (templateSplit.length == 1)
            return templateObj;

        Field[] declaredFields = templateObj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (!fieldName.equals(templateSplit[1]))
                continue;

            try {
                return field.get(templateObj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException(key + " can't be found!");
    }
}
