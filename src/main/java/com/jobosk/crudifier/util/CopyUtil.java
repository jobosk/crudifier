package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class CopyUtil {

  public static void copyProperties(final Object item, final Map<?, ?> props
      , final ObjectMapper mapper) {
    final BeanWrapper itemWrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
    if (props != null) {
      props.entrySet().stream()
          .filter(e -> canCopyProperty(itemWrapper, e.getKey()))
          .forEach(e -> copyProperty(itemWrapper, (String) e.getKey(), e.getValue(), mapper));
    }
  }

  private static boolean canCopyProperty(final BeanWrapper itemWrapper, final Object property) {
    return property instanceof String && itemWrapper.isWritableProperty((String) property);
  }

  private static void copyProperty(final BeanWrapper itemWrapper, final String key
      , final Object value, final ObjectMapper mapper) {
    itemWrapper.setPropertyValue(key, convertValue(
        value
        , itemWrapper.getPropertyDescriptor(key)
        , mapper
    ));
  }

  private static Object convertValue(final Object value, final PropertyDescriptor propertyDescriptor
      , final ObjectMapper mapper) {
    if (value == null) {
      return null;
    }
    Object convertedValue = getValue(
        value
        , propertyDescriptor.getPropertyType()
        , mapper
    );
    if (!(convertedValue instanceof Collection)) {
      return convertedValue;
    }
    return convertCollection(
        (Collection) convertedValue
        , (Class) getItemType(propertyDescriptor)
        , mapper
    );
  }

  private static <T> T getValue(final Object value, final Class<T> propertyType
      , final ObjectMapper mapper) {
    return mapper.convertValue(value, propertyType);
  }

  private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
    return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
        .getActualTypeArguments()[0];
  }

  private static <T> Collection<T> convertCollection(final Collection<T> convertedValue
      , final Class<T> itemType, final ObjectMapper mapper) {
    Collection<T> convertedCollection;
    try {
      convertedCollection = convertedValue.getClass().getDeclaredConstructor().newInstance();
      for (final Object v : convertedValue) {
        convertedCollection.add(getValue(v, itemType, mapper));
      }
    } catch (final Exception e) {
      convertedCollection = null;
    }
    return convertedCollection;
  }
}