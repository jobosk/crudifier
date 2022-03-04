package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class CopyUtil {

    public static void copyProperties(final Object item, final Map<?, ?> props, final ObjectMapper mapper) {
        final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
        if (props != null) {
            props.entrySet().stream()
                    .filter(e -> e.getKey() instanceof String && wrapper.isWritableProperty((String) e.getKey()))
                    .forEach(e -> setValue(wrapper, (String) e.getKey(), e.getValue(), mapper));
        }
    }

    private static void setValue(final BeanWrapper wrapper, final String key, final Object value, final ObjectMapper mapper) {
        wrapper.setPropertyValue(key, convertValue(
                mapper
                , value
                , wrapper.getPropertyDescriptor(key)
                , wrapper.getPropertyValue(key)
        ));
    }

    private static <Entity> Object convertValue(final ObjectMapper mapper, final Object value
            , final PropertyDescriptor propertyDescriptor, final Entity previousValue) {
        if (value == null) {
            return null;
        }
        Object convertedValue = getValue(value, propertyDescriptor.getPropertyType(), previousValue, mapper);
        if (!(convertedValue instanceof Collection)) {
            return convertedValue;
        }
        return convertCollection(
                (Collection<Entity>) convertedValue
                , (Class<Entity>) getItemType(propertyDescriptor)
                , previousValue
                , mapper
        );
    }

    private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
        return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0]).getActualTypeArguments()[0];
    }

    private static <Entity> Collection<Entity> convertCollection(final Collection<Entity> convertedValue
            , final Class<Entity> itemType, final Object previousValue, final ObjectMapper mapper) {
        Collection<Entity> convertedCollection;
        try {
            convertedCollection = convertedValue.getClass().getDeclaredConstructor().newInstance();
            for (final Object v : convertedValue) {
                convertedCollection.add(getValue(v, itemType, previousValue, mapper));
            }
        } catch (final Exception e) {
            convertedCollection = null;
        }
        return convertedCollection;
    }

    private static <Entity> Entity getValue(final Object item, final Class<Entity> itemType
            , final Object previousValue, final ObjectMapper mapper) {
        if (overridePreviousValue(itemType)) {
            return mapper.convertValue(item, itemType);
        }
        return updatePreviousValue(previousValue, itemType, item, mapper);
    }

    private static <Entity> boolean overridePreviousValue(final Class<Entity> itemType) {
        return Serializable.class.isAssignableFrom(itemType) || Collection.class.isAssignableFrom(itemType);
    }

    private static <Entity> Entity updatePreviousValue(final Object previousValue, final Class<Entity> itemType
            , final Object item, final ObjectMapper mapper) {
        final Entity result = mapper.convertValue(previousValue, itemType);
        copyProperties(result, mapper.convertValue(item, Map.class), mapper);
        return result;
    }

    /*
    public static void copyProperties(final Object src, final Object trg, final Iterable<String> props) {
        final BeanWrapper srcWrap = PropertyAccessorFactory.forBeanPropertyAccess(src);
        final BeanWrapper trgWrap = PropertyAccessorFactory.forBeanPropertyAccess(trg);
        props.forEach(p -> trgWrap.setPropertyValue(p, srcWrap.getPropertyValue(p)));
    }
    */
}