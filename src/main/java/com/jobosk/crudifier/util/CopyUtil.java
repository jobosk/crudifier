package com.jobosk.crudifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
          .forEach(e -> copyProperty(
              itemWrapper
              , (String) e.getKey()
              , e.getValue()
              , mapper
          ));
    }
  }

  private static boolean canCopyProperty(final BeanWrapper itemWrapper, final Object key) {
    return key instanceof String && itemWrapper.isWritableProperty((String) key);
  }

  private static void copyProperty(final BeanWrapper itemWrapper, final String key
      , final Object value, final ObjectMapper mapper) {

    final PropertyDescriptor propertyDescriptor = itemWrapper.getPropertyDescriptor(key);
    final Class<?> valueType = propertyDescriptor.getPropertyType();
    Object result = mapper.convertValue(value, valueType);

    final boolean canUpdatePrevious = canUpdatePrevious(value.getClass(), valueType);

    // Special treatment if the attribute its an identifiable entity
    final Object previousValue = itemWrapper.getPropertyValue(key);
    if (previousValue != null && canUpdatePrevious) {
      result = updatePreviousValue(
          result
          , previousValue
          , ((IHasIdentifier<?>) previousValue).getId()
          , mapper
      );
    }

    // Special treatment if the attribute its a collection
    if (result instanceof Collection) {
      result = copyCollection(
          (Collection<?>) result
          , (Collection<?>) previousValue
          , (Class<?>) getItemType(propertyDescriptor)
          , canUpdatePrevious
          , mapper
      );
    }

    itemWrapper.setPropertyValue(key, result);
  }

  private static boolean canUpdatePrevious(final Class<?> valueType, final Class<?> propertyType) {
    return IHasIdentifier.class.isAssignableFrom(propertyType) && valueType
        .isAssignableFrom(propertyType);
  }

  private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
    return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
        .getActualTypeArguments()[0];
  }

  private static <T> T updatePreviousValue(final T currentValue, final T previousValue
      , final Object previousValueId, final ObjectMapper mapper) {
    final Map<?, ?> properties = mapper.convertValue(currentValue, Map.class);
    if (isSameItem(previousValueId, properties.remove(PROPERTY_ID))) {
      copyProperties(previousValue, properties, mapper);
    }
    return previousValue;
  }

  private static boolean isSameItem(final Object v1, final Object v2) {
    if (v1 == null || v2 == null) {
      return false;
    }
    return String.valueOf(v1).equals(String.valueOf(v2));
  }

  private static <T> Collection<T> copyCollection(final Collection<?> convertedValues
      , final Collection<? extends T> previousValues, final Class<? extends T> itemType
      , final boolean canUpdatePrevious, final ObjectMapper mapper) {

    final List<T> result = new ArrayList<>();
    for (final Object v : convertedValues) {
      result.add(mapper.convertValue(v, itemType));
    }

    // Special treatment if the attribute is a collection of identifiable entities
    if (canUpdatePrevious) {
      return updateEntityCollection(result, previousValues, mapper);
    }

    return result;
  }

  private static <T> List<T> updateEntityCollection(final Collection<T> currentValues
      , final Collection<? extends T> previousValues, final ObjectMapper mapper) {
    return previousValues.parallelStream()
        .map(pv -> updateEntity(
            ((IHasIdentifier<?>) pv).getId()
            , pv
            , currentValues
            , mapper
        ))
        .collect(Collectors.toList());
  }

  private static <T> T updateEntity(final Object valueId, final T value, final Collection<T> values
      , final ObjectMapper mapper) {
    return values.stream()
        .filter(v -> valueId != null && valueId.equals(((IHasIdentifier<?>) v).getId()))
        .findFirst()
        .map(v -> updatePreviousValue(v, value, valueId, mapper))
        .orElse(value);
  }
}