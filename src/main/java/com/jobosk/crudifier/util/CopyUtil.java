package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class CopyUtil {

  private static final String PROPERTY_ID = "id";

  private CopyUtil() {

  }

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
        , itemWrapper.getPropertyValue(key)
        , mapper
    ));
  }

  private static <T> Object convertValue(final Object value
      , final PropertyDescriptor propertyDescriptor, final T previousValue
      , final ObjectMapper mapper) {
    if (value == null) {
      return null;
    }
    Object convertedValue = getValue(
        value
        , propertyDescriptor.getPropertyType()
        , previousValue
        , mapper
    );
    if (!(convertedValue instanceof Collection)) {
      return convertedValue;
    }
    return convertCollection(
        (Collection<T>) convertedValue
        , (Class<T>) getItemType(propertyDescriptor)
        , previousValue
        , mapper
    );
  }

  private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
    return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
        .getActualTypeArguments()[0];
  }

  private static <T> Collection<T> convertCollection(final Collection<T> convertedValue
      , final Class<T> itemType, final Object previousValue, final ObjectMapper mapper) {
    Collection<T> convertedCollection;
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

  private static <T> T getValue(final Object item, final Class<T> valueType
      , final Object previousValue, final ObjectMapper mapper) {
    if (IHasIdentifier.class.isAssignableFrom(valueType)) {
      final Map<?, ?> properties = mapper.convertValue(item, Map.class);
      if (isSameItem(
          ((IHasIdentifier<?>) valueType.cast(previousValue)).getId()
          , properties.remove(PROPERTY_ID)
      )) {
        return updateValue(valueType.cast(previousValue), properties, mapper);
      }
    }
    return mapper.convertValue(item, valueType);
  }

  private static boolean isSameItem(final Object v1, final Object v2) {
    if (v1 == null || v2 == null) {
      return false;
    }
    return String.valueOf(v1).equals(String.valueOf(v2));
  }

  private static <T> T updateValue(final T value, final Map<?, ?> properties
      , final ObjectMapper mapper) {
    copyProperties(value, properties, mapper);
    return value;
  }
}