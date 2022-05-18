package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
import com.jobosk.crudifier.exception.CrudException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
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
          .filter(e -> e.getKey() instanceof String && itemWrapper
              .isWritableProperty((String) e.getKey()))
          .forEach(e -> setItemValue(itemWrapper, (String) e.getKey(), e.getValue(), mapper));
    }
  }

  private static void setItemValue(final BeanWrapper itemWrapper, final String key,
      final Object value
      , final ObjectMapper mapper) {
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
        propertyDescriptor.getPropertyType()
        , value
        , previousValue
        , mapper
    );
    if (!(convertedValue instanceof Collection)) {
      return convertedValue;
    }
    return convertCollection(
        (Collection<?>) convertedValue
        , (Class<?>) getItemType(propertyDescriptor)
        , previousValue
        , mapper
    );
  }

  private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
    return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
        .getActualTypeArguments()[0];
  }

  private static <T> Collection<T> convertCollection(final Collection<?> convertedValue
      , final Class<? extends T> itemType, final T previousValue, final ObjectMapper mapper) {
    Collection<T> convertedCollection = new HashSet<>();
    try {
      for (final Object v : convertedValue) {
        convertedCollection.add(getValue(itemType, v, previousValue, mapper));
      }
    } catch (final Exception e) {
      throw new CrudException(e, "cannot_convert_collection");
    }
    return convertedCollection;
  }

  private static <T> T getValue(final Class<? extends T> valueType, final Object item
      , final T previousValue, final ObjectMapper mapper) {
    if (previousValue != null && IHasIdentifier.class.isAssignableFrom(valueType)) {
      final IHasIdentifier<?> identifiedType = (IHasIdentifier<?>) previousValue;
      final Map<?, ?> properties = mapper.convertValue(item, Map.class);
      if (isSameItem(identifiedType.getId(), properties.remove(PROPERTY_ID))) {
        return updateValue(previousValue, properties, mapper);
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