package com.jobosk.crudifier.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.entity.IHasIdentifier;
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

    public static <Entity extends IHasIdentifier<UUID>, Source> void setFromOne(final Source source, final Entity previous, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        setFromOne(
                previous
                , current
                , reverse
                , prev -> setter.accept(prev, null, false)
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasIdentifier<UUID>> void setFromOne(final Entity previous, final Entity current
            , final boolean reverse, final Consumer<Entity> previousClear, final Consumer<Entity> currentReverse) {
        Optional.ofNullable(current)
                .ifPresent(c -> {
                    Optional.ofNullable(previous)
                            .filter(p -> !sameIds(p, c))
                            .ifPresent(previousClear);
                    if (reverse) {
                        currentReverse.accept(c);
                    }
                });
    }

    public static <T extends IHasIdentifier<?>> boolean sameIds(final T o1, final T o2) {
        return Optional.ofNullable(o1)
                .map(IHasIdentifier::getId)
                .flatMap(id1 -> Optional.ofNullable(o2)
                        .map(IHasIdentifier::getId)
                        .map(id2 -> id2.equals(id1))
                )
                .orElse(false);
    }

    public static <Entity extends IHasIdentifier<UUID>, Source> void setFromMany(final Source source, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        setFromMany(
                current
                , reverse
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasIdentifier<UUID>> void setFromMany(final Entity current, final boolean reverse
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
        if (!getAnnotations(field, JsonIgnore.class).isEmpty()) {
            return true;
        }
        final List<JsonProperty> annotations = getAnnotations(field, JsonProperty.class);
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

    public static <T extends IHasIdentifier<UUID>, R> List<Object> setTransientField(final List<T> items, final Object newItems
            , final String transientField, final Function<UUID, Optional<R>> getter, final BiConsumer<T, R> setter
            , final Consumer<T> reflexiveAction, final Class<T> type, final Supplier<T> builder
            , final ObjectMapper mapper) {
        return setTransientField(items, newItems, transientField, getter, setter, reflexiveAction
                , null, null, null, type, builder, mapper);
    }

    public static <T extends IHasIdentifier<UUID>, R> List<Object> setTransientField(final List<T> items, final Object newItems
            , final String transientField, final Function<UUID, Optional<R>> getter, final BiConsumer<T, R> setter
            , final Consumer<T> reflexiveAction, final String recursionField, final Function<T, List<T>> recursiveGetter
            , final BiConsumer<T, T> recursiveSetter, final Class<T> type, final Supplier<T> builder
            , final ObjectMapper mapper) {
        final List<Object> sameProductItems = new ArrayList<>();
        final Map<UUID, T> currentItems = items.stream()
                .collect(Collectors.toMap(
                        T::getId
                        , i -> i
                ));
        Optional.ofNullable(newItems)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(collection -> {
                    for (final Object element : collection) {
                        if (element instanceof Map) {
                            updateTransientFields(
                                    (Map<String, Object>) element
                                    , currentItems
                                    , transientField
                                    , getter
                                    , setter
                                    , reflexiveAction
                                    , recursionField
                                    , recursiveGetter
                                    , recursiveSetter
                                    , () -> sameProductItems.add(element)
                                    , type
                                    , builder
                                    , mapper
                            );
                        } else {
                            FormatUtil.getUUID(element)
                                    .ifPresent(sameProductItems::add);
                        }
                    }
                });
        return sameProductItems;
    }

    private static <T extends IHasIdentifier<UUID>, R> void updateTransientFields(final Map<String, Object> map
            , final Map<UUID, T> currentItems, final String transientField, final Function<UUID, Optional<R>> getter
            , final BiConsumer<T, R> setter, final Consumer<T> reflexiveAction, final String recursionField
            , final Function<T, List<T>> recursiveGetter, final BiConsumer<T, T> recursiveSetter
            , final Runnable fallbackAction, final Class<T> type, final Supplier<T> builder, final ObjectMapper mapper) {
        Optional.ofNullable(map.remove(transientField))
                .flatMap(FormatUtil::getUUID)
                .flatMap(getter)
                .map(product -> {
                    final T item = getOrCreateItem(
                            map.remove("id")
                            , currentItems
                            , type
                            , builder
                            , reflexiveAction
                    );
                    setter.accept(item, product);
                    return item;
                })
                .ifPresentOrElse(
                        i -> {
                            if (recursionField != null && recursiveGetter != null && recursiveSetter != null) {
                                final List<Object> sameProductChildren = setTransientField(
                                        recursiveGetter.apply(i)
                                        , map.remove(recursionField)
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
                            copyProperties(i, map, mapper);
                        }
                        , fallbackAction
                );
    }

    public static <T extends IHasIdentifier<UUID>> T getOrCreateItem(final Object itemId, final Map<UUID, T> currentItems
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
                    .filter(e -> canCopyProperty(itemWrapper, e.getKey()))
                    .forEach(e -> copyProperty(itemWrapper, (String) e.getKey(), e.getValue(), mapper));
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
        Object convertedValue = getValue(value, propertyDescriptor.getPropertyType(), mapper);
        if (convertedValue instanceof Collection) {
            final Collection collectionValues = (Collection) convertedValue;
            Optional.ofNullable(itemWrapper.getPropertyValue(key))
                    .filter(Collection.class::isInstance)
                    .map(Collection.class::cast)
                    .filter(currentValues -> currentValues.size() == collectionValues.size())
                    .ifPresentOrElse(
                            currentValues -> {
                                final Iterator<?> currentIterator = currentValues.iterator();
                                final Iterator<?> collectionIterator = collectionValues.iterator();
                                while (currentIterator.hasNext() && collectionIterator.hasNext()) {
                                    final Object collectionItem = collectionIterator.next();
                                    if (collectionItem instanceof Map) {
                                        copyProperties(currentIterator.next(), (Map) collectionItem, mapper);
                                    }
                                }
                            }
                            , () -> {
                                final Collection<Object> convertedCollection = convertCollection(
                                        collectionValues
                                        , (Class) getItemType(propertyDescriptor)
                                        , mapper
                                );
                                itemWrapper.setPropertyValue(key, convertedCollection);
                            }
                    );
        } else {
            itemWrapper.setPropertyValue(key, convertedValue);
        }
    }

    private static <T> T getValue(final Object value, final Class<T> propertyType, final ObjectMapper mapper) {
        return mapper.convertValue(value, propertyType);
    }

    private static Type getItemType(final PropertyDescriptor propertyDescriptor) {
        return ((ParameterizedType) propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0])
                .getActualTypeArguments()[0];
    }

    private static <T> Collection<T> convertCollection(final Collection<T> convertedValue, final Class<T> itemType
            , final ObjectMapper mapper) {
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
