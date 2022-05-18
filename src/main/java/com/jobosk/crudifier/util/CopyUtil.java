package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class CopyUtil {

  private static final String PROPERTY_ID = "id";

  private CopyUtil() {

  }

  public static void copyProperties(final Object item, final Map<?, ?> props
      , final ObjectMapper mapper) {
    copyProperties(item, props, new HashMap<>(), mapper);
  }

  public static void copyProperties(final Object item, final Map<?, ?> props
      , final Map<Class<?>, Set<Object>> updatedIds, final ObjectMapper mapper) {
    final BeanWrapper itemWrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
    if (props != null) {
      props.entrySet().stream()
          .filter(e -> e.getKey() instanceof String && itemWrapper
              .isWritableProperty((String) e.getKey()))
          .forEach(e -> setItemValue(
              itemWrapper
              , (String) e.getKey()
              , e.getValue()
              , updatedIds
              , mapper
          ));
    }
  }

  private static void setItemValue(final BeanWrapper itemWrapper, final String key
      , final Object value, final Map<Class<?>, Set<Object>> updatedIds
      , final ObjectMapper mapper) {
    final PropertyDescriptor propertyDescriptor = itemWrapper.getPropertyDescriptor(key);
    final Object previousValue = itemWrapper.getPropertyValue(key);
    final Object convertedValue = convertValue(
        value
        , propertyDescriptor
        , previousValue
        , updatedIds
        , mapper
    );
    if (convertedValue instanceof Collection) {
      itemWrapper.setPropertyValue(key, convertCollection(
          (Class<?>) getItemType(propertyDescriptor)
          , (Collection<?>) convertedValue
          , (Collection<?>) previousValue
          , updatedIds
          , mapper
      ));
    } else {
      itemWrapper.setPropertyValue(key, convertedValue);
    }
  }

  private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
    return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
        .getActualTypeArguments()[0];
  }

  private static <T> Object convertValue(final Object value
      , final PropertyDescriptor propertyDescriptor, final T previousValue
      , final Map<Class<?>, Set<Object>> updatedIds, final ObjectMapper mapper) {
    final Class<?> valueType = propertyDescriptor.getPropertyType();
    final Object result = mapper.convertValue(value, valueType);
    if (previousValue != null && IHasIdentifier.class.isAssignableFrom(valueType)) {
      return updatePreviousValue(
          previousValue
          , ((IHasIdentifier<?>) previousValue).getId()
          , result
          , updatedIds
          , mapper
      );
    }
    return result;
  }

  private static <T> T updatePreviousValue(final T previousValue, final Object previousValueId
      , final T currentValue, final Map<Class<?>, Set<Object>> updatedIds
      , final ObjectMapper mapper) {
    final Map<?, ?> properties = mapper.convertValue(currentValue, Map.class);
    if (canUpdatePreviousItem(
        previousValueId
        , properties.remove(PROPERTY_ID)
        , updatedIds
        , previousValue.getClass()
    )) {
      updatedIds
          .computeIfAbsent(previousValue.getClass(), k -> new HashSet<>())
          .add(previousValueId);
      copyProperties(previousValue, properties, updatedIds, mapper);
    }
    return previousValue;
  }

  private static boolean canUpdatePreviousItem(final Object previousId, final Object id
      , final Map<Class<?>, Set<Object>> updatedIds, final Class<?> previousType) {
    final boolean previouslyUpdated = updatedIds
        .getOrDefault(previousType, new HashSet<>())
        .contains(previousId);
    return isSameItem(previousId, id) && !previouslyUpdated;
  }

  private static boolean isSameItem(final Object v1, final Object v2) {
    if (v1 == null || v2 == null) {
      return false;
    }
    return String.valueOf(v1).equals(String.valueOf(v2));
  }

  private static <T> Collection<T> convertCollection(final Class<? extends T> itemType
      , final Collection<?> convertedValues, final Collection<? extends T> previousValues
      , final Map<Class<?>, Set<Object>> updatedIds, final ObjectMapper mapper) {
    Collection<T> convertedCollection = new HashSet<>();
    for (final Object v : convertedValues) {
      convertedCollection.add(mapper.convertValue(v, itemType));
    }
    if (IHasIdentifier.class.isAssignableFrom(itemType)) {
      return updatedPreviousValues(convertedCollection, previousValues, updatedIds, mapper);
    }
    return convertedCollection;
  }

  private static <T> Collection<T> updatedPreviousValues(final Collection<T> values
      , final Collection<? extends T> previousValues, final Map<Class<?>, Set<Object>> updatedIds
      , final ObjectMapper mapper) {
    return previousValues.stream()
        .map(pv -> {
          final IHasIdentifier<?> previousValue = (IHasIdentifier<?>) pv;
          return values.stream()
              .filter(v -> previousValue.getId().equals(((IHasIdentifier<?>) v).getId()))
              .findFirst()
              .map(v -> updatePreviousValue(pv, previousValue.getId(), v, updatedIds, mapper))
              .orElse(pv);
        })
        .collect(Collectors.toSet());
  }
}