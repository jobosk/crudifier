package com.jobosk.crudifier.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasCrudId;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class ModelUtil {

    public static <Entity extends IHasCrudId<UUID>, Source> void setFromOne(final Source source, final Entity previous, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        ModelUtil.setFromOne(
                previous
                , current
                , reverse
                , prev -> setter.accept(prev, null, false)
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasCrudId<UUID>> void setFromOne(final Entity previous, final Entity current
            , final boolean reverse, final Consumer<Entity> previousClear, final Consumer<Entity> currentReverse) {
        Optional.ofNullable(current)
                .ifPresent(c -> {
                    Optional.ofNullable(previous)
                            .filter(p -> !ModelUtil.sameIds(p, c))
                            .ifPresent(previousClear);
                    if (reverse) {
                        currentReverse.accept(c);
                    }
                });
    }

    public static <T extends IHasCrudId<?>> boolean sameIds(final T o1, final T o2) {
        return Optional.ofNullable(o1)
                .map(IHasCrudId::getId)
                .flatMap(id1 -> Optional.ofNullable(o2)
                        .map(IHasCrudId::getId)
                        .map(id2 -> id2.equals(id1))
                )
                .orElse(false);
    }

    public static <Entity extends IHasCrudId<UUID>, Source> void setFromMany(final Source source, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        ModelUtil.setFromMany(
                current
                , reverse
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasCrudId<UUID>> void setFromMany(final Entity current, final boolean reverse
            , final Consumer<Entity> currentReverse) {
        Optional.ofNullable(current)
                .ifPresent(c -> {
                    if (reverse) {
                        currentReverse.accept(c);
                    }
                });
    }

    public static List<Field> getAllFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));
        Optional.ofNullable(type.getSuperclass())
                .map(ModelUtil::getAllFields)
                .ifPresent(fields::addAll);
        return fields;
    }

    public static boolean isReadOnly(final Field field) {
        if (!ModelUtil.getAnnotations(field, JsonIgnore.class).isEmpty()) {
            return true;
        }
        final List<JsonProperty> annotations = ModelUtil.getAnnotations(field, JsonProperty.class);
        if (annotations.isEmpty()) {
            return false;
        }
        return annotations.stream()
                .allMatch(p -> JsonProperty.Access.READ_ONLY.equals(p.access()));
    }

    public static <T> List<T> getAnnotations(final Field field, final Class<T> type) {
        return Arrays.stream(field.getDeclaredAnnotations())
                .filter(a -> a.annotationType().isAssignableFrom(type))
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public static <T extends IHasCrudId<UUID>, R> void setTransientFieldsInArray(final List<T> currentItems
            , final Map<String, Object> attributes, final String arrayField, final String transientField
            , final Function<UUID, Optional<R>> getter, final BiConsumer<T, R> setter
            , final Consumer<T> reflexiveAction, final String recursionField
            , final Function<T, List<T>> recursiveGetter, final BiConsumer<T, T> recursiveSetter
            , final Class<T> type, final Supplier<T> builder, final ObjectMapper mapper) {
        final Collection<?> items = Optional.ofNullable(attributes.remove(arrayField))
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .orElse(new ArrayList<>());
        if (items.isEmpty() && !currentItems.isEmpty()) {
            attributes.put(arrayField, items);
        } else {
            final List<Object> result = ModelUtil.setTransientFields(currentItems, items, transientField
                    , getter, setter, reflexiveAction, recursionField, recursiveGetter, recursiveSetter
                    , type, builder, mapper);
            if (!result.isEmpty()) {
                attributes.put(arrayField, result);
            }
        }
    }

    public static <T extends IHasCrudId<UUID>, R> List<Object> setTransientFields(final List<T> currentItems
            , final Collection<?> newItems, final String transientField, final Function<UUID, Optional<R>> getter
            , final BiConsumer<T, R> setter, final Consumer<T> reflexiveAction, final Class<T> type
            , final Supplier<T> builder, final ObjectMapper mapper) {
        return ModelUtil.setTransientFields(currentItems, newItems, transientField, getter, setter, reflexiveAction
                , null, null, null, type, builder, mapper);
    }

    public static <T extends IHasCrudId<UUID>, R> List<Object> setTransientFields(final List<T> currentItems
            , final Collection<?> newItems, final String transientField, final Function<UUID, Optional<R>> getter
            , final BiConsumer<T, R> setter, final Consumer<T> reflexiveAction, final String recursionField
            , final Function<T, List<T>> recursiveGetter, final BiConsumer<T, T> recursiveSetter, final Class<T> type
            , final Supplier<T> builder, final ObjectMapper mapper) {
        final Map<UUID, T> currentItemsById = currentItems.stream()
                .collect(Collectors.toMap(T::getId, i -> i));
        final List<Object> result = new ArrayList<>();
        for (final Object newItem : newItems) {
            if (newItem instanceof Map) {
                ModelUtil.updateTransientFields(
                        (Map<String, Object>) newItem
                        , currentItemsById
                        , transientField
                        , getter
                        , setter
                        , reflexiveAction
                        , recursionField
                        , recursiveGetter
                        , recursiveSetter
                        , type
                        , builder
                        , mapper
                ).ifPresentOrElse(
                        item -> Optional.ofNullable(item.getId())
                                .ifPresent(result::add)
                        , () -> result.add(newItem)
                );
            } else {
                FormatUtil.getUUID(newItem)
                        .ifPresent(result::add);
            }
        }
        return result;
    }

    private static <T extends IHasCrudId<UUID>, R> Optional<T> updateTransientFields(final Map<String, Object> map
            , final Map<UUID, T> currentItems, final String transientField, final Function<UUID, Optional<R>> getter
            , final BiConsumer<T, R> setter, final Consumer<T> reflexiveAction, final String recursionField
            , final Function<T, List<T>> recursiveGetter, final BiConsumer<T, T> recursiveSetter
            , final Class<T> type, final Supplier<T> builder, final ObjectMapper mapper) {
        return Optional.ofNullable(map.remove(transientField))
                .flatMap(FormatUtil::getUUID)
                .flatMap(getter)
                .map(product -> {
                    final T item = ModelUtil.getOrCreateItem(map.remove("id"), currentItems, type, builder, reflexiveAction);
                    setter.accept(item, product);
                    return item;
                })
                .map(i -> {
                    if (recursionField != null && recursiveGetter != null && recursiveSetter != null) {
                        ModelUtil.setTransientFieldsInArray(
                                recursiveGetter.apply(i)
                                , map
                                , recursionField
                                , transientField
                                , getter
                                , setter
                                , c -> recursiveSetter.accept(i, c)
                                , recursionField
                                , recursiveGetter
                                , recursiveSetter
                                , type
                                , builder
                                , mapper
                        );
                        /*
                        final Collection<?> children = Optional.ofNullable(map.remove(recursionField))
                                .filter(Collection.class::isInstance)
                                .map(Collection.class::cast)
                                .orElse(new ArrayList<>());
                        final List<T> currentChildren = recursiveGetter.apply(i);
                        if (children.isEmpty() && !currentChildren.isEmpty()) {
                            map.put(recursionField, children);
                        } else {
                            final List<Object> sameProductChildren = setTransientFields(
                                    currentChildren
                                    , children
                                    , transientField
                                    , getter
                                    , setter
                                    , c -> recursiveSetter.accept(i, c)
                                    , recursionField
                                    , recursiveGetter
                                    , recursiveSetter
                                    , type
                                    , builder
                                    , mapper
                            );
                            if (!sameProductChildren.isEmpty()) {
                                map.put(recursionField, sameProductChildren);
                            }
                        }
                        */
                    }
                    ModelUtil.copyProperties(i, map, mapper);
                    return i;
                });
    }

    public static <T extends IHasCrudId<UUID>> T getOrCreateItem(final Object itemId, final Map<UUID, T> currentItems
            , final Class<T> type, final Supplier<T> builder, final Consumer<T> reflexiveAction) {
        return Optional.ofNullable(itemId)
                .flatMap(FormatUtil::getUUID)
                .map(currentItems::get)
                .map(type::cast)
                .orElseGet(() -> {
                    final T item = builder.get();
                    reflexiveAction.accept(item);
                    return item;
                });
    }

    public static void copyProperties(final Object item, final Map<?, ?> props, final ObjectMapper mapper) {
        final BeanWrapper itemWrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
        if (props != null) {
            props.entrySet().stream()
                    .filter(e -> ModelUtil.canCopyProperty(itemWrapper, e.getKey()))
                    .forEach(e -> ModelUtil.copyProperty(itemWrapper, (String) e.getKey(), e.getValue(), mapper));
        }
    }

    private static boolean canCopyProperty(final BeanWrapper itemWrapper, final Object property) {
        return property instanceof String && itemWrapper.isWritableProperty((String) property);
    }

    private static void copyProperty(final BeanWrapper itemWrapper, final String key, final Object value
            , final ObjectMapper mapper) {
        if (value == null) {
            return;
        }
        final PropertyDescriptor propertyDescriptor = itemWrapper.getPropertyDescriptor(key);
        ModelUtil.getValue(value, propertyDescriptor.getPropertyType(), mapper)
                .ifPresent(convertedValue -> {
                    if (convertedValue instanceof Collection) {
                        itemWrapper.setPropertyValue(key, ModelUtil.copyCollectionValues(
                                (Collection<?>) convertedValue
                                , itemWrapper.getPropertyValue(key)
                                , (Class<?>) ModelUtil.getItemType(propertyDescriptor)
                                , mapper
                        ));
                    } else {
                        itemWrapper.setPropertyValue(key, convertedValue);
                    }
                });
    }

    private static <T> Collection<T> copyCollectionValues(final Collection<?> values
            , final Object propertyValue, final Class<T> type, final ObjectMapper mapper) {
        final List<T> result = new ArrayList<>();
        final Map<UUID, T> previousValuesById = new HashMap<>();
        final List<T> previousValuesWithoutId = new ArrayList<>();
        ModelUtil.groupById(propertyValue, type, previousValuesById, previousValuesWithoutId);
        for (final Object value : values) {
            ModelUtil.getItemAttributes(value, mapper)
                    .flatMap(collectionItem -> FormatUtil.getUUID(collectionItem.get("id"))
                            .map(previousValuesById::get)
                            .map(currentValue -> {
                                ModelUtil.copyProperties(currentValue, collectionItem, mapper);
                                return currentValue;
                            })
                    )
                    .map(type::cast)
                    .ifPresentOrElse(
                            result::add
                            , () -> ModelUtil.getValue(value, type, mapper)
                                    .ifPresent(result::add)
                    );
        }
        ModelUtil.convertCollection(previousValuesWithoutId, type, mapper)
                .ifPresent(result::addAll);
        return result;
    }

    private static <T> void groupById(final Object propertyValue, final Class<T> type
            , final Map<UUID, T> previousValuesById, final List<T> previousValuesWithoutId) {
        Optional.ofNullable(propertyValue)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(values -> {
                    for (final Object value : values) {
                        ModelUtil.groupByIds(type.cast(value), previousValuesById, previousValuesWithoutId);
                    }
                });
    }

    private static <T> void groupByIds(final T value, final Map<UUID, T> previousValuesById
            , final List<T> previousValuesWithoutId) {
        if (IHasCrudId.class.isAssignableFrom(value.getClass())) {
            Optional.ofNullable(((IHasCrudId<?>) value).getId())
                    .flatMap(FormatUtil::getUUID)
                    .ifPresentOrElse(
                            id -> previousValuesById.put(id, value)
                            , () -> previousValuesWithoutId.add(value)
                    );
        } else {
            previousValuesWithoutId.add(value);
        }
    }

    private static Optional<Map> getItemAttributes(final Object item, final ObjectMapper mapper) {
        return Optional.ofNullable(item)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .or(() -> ModelUtil.getValue(item, Map.class, mapper));
    }

    private static Optional<UUID> getId(final Object currentValue, final ObjectMapper mapper) {
        if (currentValue instanceof IHasCrudId) {
            return FormatUtil.getUUID(((IHasCrudId<?>) currentValue).getId());
        }
        try {
            final Map<?, ?> map = mapper.convertValue(currentValue, Map.class);
            return Optional.ofNullable(map.get("id"))
                    .flatMap(FormatUtil::getUUID);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static <T> Optional<T> getValue(final Object value, final Class<T> propertyType,
                                            final ObjectMapper mapper) {
        try {
            return Optional.of(mapper.convertValue(value, propertyType));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
        return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
                .getActualTypeArguments()[0];
    }

    private static <T> Optional<Collection<T>> convertCollection(final Collection<T> convertedValue,
                                                                 final Class<T> itemType
            , final ObjectMapper mapper) {
        try {
            final Collection<T> convertedCollection = convertedValue.getClass().getDeclaredConstructor().newInstance();
            for (final Object v : convertedValue) {
                ModelUtil.getValue(v, itemType, mapper)
                        .ifPresent(convertedCollection::add);
            }
            return Optional.of(convertedValue);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}
