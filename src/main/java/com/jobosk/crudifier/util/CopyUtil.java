package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class CopyUtil {

    public static void copyProperties(final Object obj, final Map<String, Object> props, final ObjectMapper mapper) {
        final BeanWrapper objWrap = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        if (props != null) {
            props.entrySet().stream()
                    .filter(e -> objWrap.isWritableProperty(e.getKey()))
                    .forEach(e -> objWrap.setPropertyValue(
                            e.getKey()
                            , convertValue(mapper, e.getValue(), objWrap.getPropertyDescriptor(e.getKey()))
                    ));
        }
    }

    private static Object convertValue(final ObjectMapper mapper, final Object value
            , final PropertyDescriptor propertyDescriptor) {
        if (value == null) {
            return null;
        }
        Object convertedValue = mapper.convertValue(value, propertyDescriptor.getPropertyType());
        if (convertedValue instanceof Collection) {
            convertedValue = convertCollection(
                    mapper
                    , (Collection) convertedValue
                    , (Class) getItemType(propertyDescriptor)
            );
        }
        return convertedValue;
    }

    private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
        return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0]).getActualTypeArguments()[0];
    }

    private static <Entity> Collection<Entity> convertCollection(final ObjectMapper mapper
            , final Collection<Entity> convertedValue, final Class<Entity> itemType) {
        Collection<Entity> convertedCollection;
        try {
            convertedCollection = convertedValue.getClass().getDeclaredConstructor().newInstance();
            for (final Object item : convertedValue) {
                convertedCollection.add(mapper.convertValue(item, itemType));
            }
        } catch (final Exception e) {
            convertedCollection = null;
        }
        return convertedCollection;
    }

    /*
    public static void copyProperties(final Object src, final Object trg, final Iterable<String> props) {
        final BeanWrapper srcWrap = PropertyAccessorFactory.forBeanPropertyAccess(src);
        final BeanWrapper trgWrap = PropertyAccessorFactory.forBeanPropertyAccess(trg);
        props.forEach(p -> trgWrap.setPropertyValue(p, srcWrap.getPropertyValue(p)));
    }
    */
}
