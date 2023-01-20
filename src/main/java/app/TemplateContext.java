package app;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TemplateContext {
    private Map<String, Object> templates = new HashMap<>();

    void put(String key, Object value) {
        templates.put(key, value);
    }

    public Object getTemplate(String template) {
        String[] templateSplit = template.split("\\.");
        Object templateObj = templates.get(templateSplit[0]);
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

        throw new IllegalStateException(template + " can't be found!");
    }
}
