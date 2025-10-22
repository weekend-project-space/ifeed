package org.bitmagic.ifeed.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {
    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return "[]"; // PostgreSQL 空数组
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (float v : attribute) {
            stringBuilder.append(v).append(",");
        }
        return "[%s]".formatted(stringBuilder.toString().substring(0, stringBuilder.length() - 1));
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.equals("[]]")) {
            return new float[0];
        }
        String[] values = dbData.replace("[", "").replace("]", "").split(",");
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Float.parseFloat(values[i]);
        }
        return result;
    }

    public static void main(String[] args) {
        float[] a = new float[]{1f, 0.1f};
        System.out.println(new FloatArrayConverter().convertToDatabaseColumn(a));
    }
}