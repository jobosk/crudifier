package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class CopyUtil {

    private static final String PROPERTY_ID = "id";

    public static void copyProperties(final Object item, final Map<?, ?> props, final ObjectMapper mapper) {
        final BeanWrapper itemWrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
        if (props != null) {
            props.entrySet().stream()
                    .filter(e -> e.getKey() instanceof String && itemWrapper.isWritableProperty((String) e.getKey()))
                    .forEach(e -> setItemValue(itemWrapper, (String) e.getKey(), e.getValue(), mapper));
        }
    }

    private static void setItemValue(final BeanWrapper itemWrapper, final String key, final Object value
            , final ObjectMapper mapper) {
        itemWrapper.setPropertyValue(key, convertValue(
                value
                , itemWrapper.getPropertyDescriptor(key)
                , itemWrapper.getPropertyValue(key)
                , mapper
        ));
    }

    private static <ValueType> Object convertValue(final Object value, final PropertyDescriptor propertyDescriptor
            , final ValueType previousValue, final ObjectMapper mapper) {
        if (value == null) {
            return null;
        }
        Object convertedValue = getValue(value, propertyDescriptor.getPropertyType(), previousValue, mapper);
        if (!(convertedValue instanceof Collection)) {
            return convertedValue;
        }
        return convertCollection(
                (Collection<ValueType>) convertedValue
                , (Class<ValueType>) getItemType(propertyDescriptor)
                , previousValue
                , mapper
        );
    }

    private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
        return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0]).getActualTypeArguments()[0];
    }

    private static <ValueType> Collection<ValueType> convertCollection(final Collection<ValueType> convertedValue
            , final Class<ValueType> itemType, final Object previousValue, final ObjectMapper mapper) {
        Collection<ValueType> convertedCollection;
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

    private static <ValueType> ValueType getValue(final Object item, final Class<ValueType> valueType
            , final Object previousValue, final ObjectMapper mapper) {
        //if (isValidJsonObject(previousValue)) {
        if (IHasIdentifier.class.isAssignableFrom(valueType)) {
            final Map<?, ?> properties = mapper.convertValue(item, Map.class);
            //final Map<?, ?> previousProperties = mapper.convertValue(previousValue, Map.class);
            if (isSameItem(
                    //previousProperties.get(PROPERTY_ID)
                    ((IHasIdentifier<?>) valueType.cast(previousValue)).getId()
                    , properties.remove(PROPERTY_ID)
            )) {
                return updateValue(valueType.cast(previousValue), properties, mapper);
            }
        }
        return mapper.convertValue(item, valueType);
    }

    /*
    private static boolean isValidJsonObject(final Object value) {
        try {
            final JsonParser parser = new JsonFactory().createParser(value.toString());
            do {
                parser.nextToken();
            } while (!parser.isClosed());
        } catch (final Exception e) {
            return false;
        }
        return true;
    }
    */

    private static boolean isSameItem(final Object v1, final Object v2) {
        if (v1 == null || v2 == null) {
            return false;
        }
        return String.valueOf(v1).equals(String.valueOf(v2));
    }

    private static <ValueType> ValueType updateValue(final ValueType value, final Map<?, ?> properties
            , final ObjectMapper mapper) {
        copyProperties(value, properties, mapper);
        return value;
    }

    /*
    public static void copyProperties(final Object src, final Object trg, final Iterable<String> props) {
        final BeanWrapper srcWrap = PropertyAccessorFactory.forBeanPropertyAccess(src);
        final BeanWrapper trgWrap = PropertyAccessorFactory.forBeanPropertyAccess(trg);
        props.forEach(p -> trgWrap.setPropertyValue(p, srcWrap.getPropertyValue(p)));
    }
    */
}