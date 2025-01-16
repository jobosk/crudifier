package com.jobosk.crudifier.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.dto.ITransientFieldAction;
import com.jobosk.crudifier.dto.RecursiveActionDTO;
import com.jobosk.crudifier.entity.IHasCrudId;
import com.jobosk.crudifier.exception.CrudException;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

public abstract class ModelUtil {

    public static <Entity extends IHasCrudId<?>, Source> void setFromOne(final Source source, final Entity previous, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        ModelUtil.setFromOne(
                previous
                , current
                , reverse
                , prev -> setter.accept(prev, null, false)
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasCrudId<?>> void setFromOne(final Entity previous, final Entity current
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

    public static <Entity extends IHasCrudId<?>, Source> void setFromMany(final Source source, final Entity current
            , final boolean reverse, final TriConsumer<Entity, Source, Boolean> setter) {
        ModelUtil.setFromMany(
                current
                , reverse
                , curr -> setter.accept(curr, source, false)
        );
    }

    private static <Entity extends IHasCrudId<?>> void setFromMany(final Entity current, final boolean reverse
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

    public static <T extends IHasCrudId<UUID>> void setTransientFieldsInObject(
            final T currentItem
            , final Map<String, Object> attributes
            , final Collection<ITransientFieldAction> actions
    ) {
        actions.forEach(action -> applyAction(
                attributes
                , action.getTransientField()
                , action.getGetter()
                , action.getSetter()
                , () -> currentItem
        ));
    }

    public static <T extends IHasCrudId<UUID>> void setTransientFieldsInArray(
            final Map<String, Object> attributes
            , final String arrayField
            , final Function<UUID, T> currentItemGetter
            , final Collection<ITransientFieldAction> actions
            , final Consumer<T> reflexiveAction
            , final Class<T> type
            , final Supplier<T> builder
            , final RecursiveActionDTO recursiveAction
            , final ObjectMapper mapper
    ) {
        if (attributes.containsKey(arrayField)) {
            attributes.put(
                    arrayField
                    , ModelUtil.setTransientFieldsInArray(
                            attributes.remove(arrayField)
                            , currentItemGetter
                            , actions
                            , reflexiveAction
                            , type
                            , builder
                            , recursiveAction
                            , mapper
                    )
            );
        }
    }

    public static <T extends IHasCrudId<UUID>, R extends IHasCrudId<UUID>> List<Object> setTransientFieldsInArray(
            final Object newItems
            , final Function<UUID, T> currentItemGetter
            , final Collection<ITransientFieldAction> actions
            , final Consumer<T> reflexiveAction
            , final Class<T> type
            , final Supplier<T> builder
            , final RecursiveActionDTO<T, R> recursiveAction
            , final ObjectMapper mapper
    ) {
        final List<Object> result = new ArrayList<>();
        Optional.ofNullable(newItems)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(collection -> {
                    for (final Object newItem : collection) {
                        if (newItem instanceof Map) {
                            final Map<String, Object> map = (Map<String, Object>) newItem;
                            ModelUtil.updateTransientFieldsRecursive(
                                    map
                                    , (r, m) -> getItem(r, m, currentItemGetter, type, builder, reflexiveAction)
                                    , actions
                                    , recursiveAction
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
                });
        return result;
    }

    private static <T extends IHasCrudId<UUID>> Optional<T> getItem(final Object itemId
            , final Function<UUID, T> currentItemGetter, final Class<T> type) {
        return Optional.ofNullable(itemId)
                .flatMap(FormatUtil::getUUID)
                .map(currentItemGetter)
                .map(type::cast);
    }

    private static <T extends IHasCrudId<UUID>> T getItem(final AtomicReference<T> result, final Map<String, Object> map
            , final Function<UUID, T> currentItemGetter, final Class<T> type, final Supplier<T> builder
            , final Consumer<T> reflexiveAction) {
        return Optional.ofNullable(result.get())
                .orElseGet(() -> ModelUtil.getOrCreateItem(
                        map.remove("id")
                        , currentItemGetter
                        , type
                        , builder
                        , reflexiveAction
                ));
    }

    public static <T extends IHasCrudId<UUID>> T getOrCreateItem(
            final Object itemId
            , final Function<UUID, T> currentItemGetter
            , final Class<T> type
            , final Supplier<T> builder
            , final Consumer<T> reflexiveAction
    ) {
        return getItem(itemId, currentItemGetter, type)
                .orElseGet(() -> {
                    final T item = builder.get();
                    reflexiveAction.accept(item);
                    return item;
                });
    }

    public static <T extends IHasCrudId<UUID>, R extends IHasCrudId<UUID>> Optional<T> updateTransientFieldsRecursive(
            final Map<String, Object> map
            , final BiFunction<AtomicReference<T>, Map<String, Object>, T> itemGetter
            , final Collection<ITransientFieldAction> actions
            , final RecursiveActionDTO<T, R> recursiveAction
            , final ObjectMapper mapper
    ) {
        final AtomicReference<T> result = new AtomicReference<>();
        for (final ITransientFieldAction<T, R> action : actions) {
            applyAction(map, action.getTransientField(), action.getGetter(), action.getSetter(), () -> itemGetter.apply(result, map))
                    .ifPresent(result::set);
        }
        if (recursiveAction != null) {
            final T item = itemGetter.apply(result, map);
            applyRecursiveAction(item, map, recursiveAction, mapper);
            result.set(item);
        }
        return Optional.ofNullable(result.get())
                .map(r -> {
                    if (!map.isEmpty()) {
                        ModelUtil.copyProperties(r, map, mapper);
                    }
                    return r;
                });
    }

    private static <T extends IHasCrudId<UUID>, R> Optional<T> applyAction(final Map<String, Object> map
            , final String transientField, final Function<Object, Optional<R>> getter, final BiConsumer<T, R> setter
            , final Supplier<T> itemGetter) {
        return Optional.ofNullable(map.remove(transientField))
                .flatMap(getter)
                .map(v -> {
                    final T item = itemGetter.get();
                    setter.accept(item, v);
                    return item;
                });
    }

    private static <T extends IHasCrudId<UUID>, R> void applyRecursiveAction(final R item, final Map<String, Object> map
            , final RecursiveActionDTO<R, T> recursiveAction, final ObjectMapper mapper) {
        ModelUtil.setTransientFieldsInArray(
                map
                , recursiveAction.field
                , groupById(recursiveAction.getter.apply(item))::get
                , recursiveAction.actions
                , c -> recursiveAction.setter.accept(item, c)
                , recursiveAction.type
                , recursiveAction.builder
                , recursiveAction.next
                , mapper
        );
    }

    public static <T extends IHasCrudId<UUID>> Map<UUID, T> groupById(final List<T> items) {
        return items.stream()
                .collect(Collectors.toMap(T::getId, i -> i));
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
            itemWrapper.setPropertyValue(key, null);
        } else {
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
    }

    private static <T> Collection<T> copyCollectionValues(final Collection<?> newValues
            , final Object currentValues, final Class<T> type, final ObjectMapper mapper) {
        final List<T> result = new ArrayList<>();
        Optional.ofNullable(currentValues)
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(values -> {
                    final Map<UUID, T> currentValuesById = new HashMap<>();
                    final List<T> currentValuesWithoutId = new ArrayList<>();
                    ModelUtil.groupById(values, type, currentValuesById, currentValuesWithoutId);
                    for (final Object value : newValues) {
                        ModelUtil.getItemAttributes(value, mapper)
                                .flatMap(attributes -> FormatUtil.getUUID(attributes.get("id"))
                                        .map(currentValuesById::get)
                                        .map(currentValue -> {
                                            ModelUtil.copyProperties(currentValue, attributes, mapper);
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
                    ModelUtil.convertCollection(currentValuesWithoutId, type, mapper)
                            .ifPresent(result::addAll);
                });
        return result;
    }

    private static <T> void groupById(final Collection<?> currentValues, final Class<T> type
            , final Map<UUID, T> previousValuesById, final List<T> previousValuesWithoutId) {
        if (IHasCrudId.class.isAssignableFrom(type)) {
            for (final Object value : currentValues) {
                ModelUtil.groupByIds(type.cast(value), previousValuesById, previousValuesWithoutId);
            }
        }
    }

    private static <T> void groupByIds(final T value, final Map<UUID, T> previousValuesById
            , final List<T> previousValuesWithoutId) {
        Optional.ofNullable(((IHasCrudId<?>) value).getId())
                .flatMap(FormatUtil::getUUID)
                .ifPresentOrElse(
                        id -> previousValuesById.put(id, value)
                        , () -> previousValuesWithoutId.add(value)
                );
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

    private static <T> Optional<T> getValue(final Object value, final Class<T> propertyType, final ObjectMapper mapper) {
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

    public static <T extends IHasCrudId<UUID>> void checkRecursion(final T entity, Function<T, Collection<T>> getter
            , final Set<UUID> visited, final String exceptionCode) throws CrudException {
        if (visited.contains(entity.getId())) {
            throw new CrudException(exceptionCode, List.of(entity.getId()));
        }
        visited.add(entity.getId());
        for (final T e : getter.apply(entity)) {
            checkRecursion(e, getter, visited, exceptionCode);
        }
    }
}
