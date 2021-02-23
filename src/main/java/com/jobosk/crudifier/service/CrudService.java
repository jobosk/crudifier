package com.jobosk.crudifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.constant.CrudConstant;
import com.jobosk.crudifier.entity.ICrudEntity;
import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.util.CopyUtil;
import org.hibernate.query.criteria.internal.path.ListAttributeJoin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.BasicType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//@Primary
//@Service
//@Scope("prototype")
public abstract class CrudService<Entity, Id> implements ICrudService<Entity, Id> {

    private static final String ID = "id";
    private static final String SEPARATOR = ".";

    private final GenericRepository<Entity, Id> repository;

    @Autowired
    protected ObjectMapper mapper;

    public CrudService(final GenericRepository<Entity, Id> repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Map<String, String> filters) {
        return repository.findAll(getSpecification(filters));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Map<String, String> filters, final Sort sort) {
        return repository.findAll(getSpecification(filters), sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Entity> find(final Map<String, String> filters, final Pageable pageable) {
        return repository.findAll(getSpecification(filters), pageable);
    }

    private Specification<Entity> getSpecification(final Map<String, String> filters) {
        return (Specification<Entity>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            filters.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .forEach(e -> {
                        final String key = e.getKey();
                        final String[] values = e.getValue().split(",");
                        if (values.length > 0) {
                            if (values.length == 1) {
                                addPredicate(predicates, builder, root, root, root.getModel(), key, values[0]);
                            } else {
                                final List<Predicate> predicatesOr = new ArrayList<>();
                                for (final String value : values) {
                                    addPredicate(predicatesOr, builder, root, root, root.getModel(), key, value);
                                }
                                predicates.add(builder.or(predicatesOr.toArray(new Predicate[0])));
                            }
                        }
                    });
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private <T> void addPredicate(final List<Predicate> predicates, final CriteriaBuilder builder, final From<?, ?> from
            , final Path<T> path, final ManagedType<T> modelType, final String property, final Object filterValue) {
        final Predicate predicate;
        if ("genericSearch".equals(property)) {
            predicate = buildPredicateGenericSearch(
                    builder
                    , from
                    , path
                    , modelType
                    , filterValue
            );
        } else {
            predicate = buildPredicate(
                    builder
                    , from
                    , path
                    , modelType
                    , property
                    , filterValue
            );
        }
        if (predicate != null) {
            predicates.add(predicate);
        }
    }

    private <T> Predicate buildPredicateGenericSearch(final CriteriaBuilder builder, final From<?, ?> from
            , final Path<T> path, final ManagedType<T> modelType, final Object filterValue) {
        final List<Predicate> predicates = new ArrayList<>();
        for (Attribute<? super T, ?> attribute : modelType.getAttributes()) {
            final Predicate predicate = buildPredicate(
                    builder
                    , from
                    , path
                    , attribute
                    , null
                    , filterValue
            );
            if (predicate != null) {
                predicates.add(predicate);
            }
        }
        return builder.or(predicates.toArray(new Predicate[0]));
    }

    private <T> Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from, final Path<T> path
            , final ManagedType<T> modelType, final String property, final Object filterValue) {
        if (filterValue == null) {
            return null;
        }
        final int index = property != null ? property.indexOf(SEPARATOR) : -1;
        if (index != -1) {
            final Attribute<? super T, ?> attribute = modelType.getAttribute(
                    property.substring(0, index)
            );
            return buildPredicate(
                    builder
                    , from
                    , path
                    , attribute
                    , property.substring(index + 1)
                    , filterValue
            );
        }
        if (property != null) {
            return setProperty(builder, path, modelType, property, filterValue);
        } else {
            return setAttributes(builder, path, modelType, filterValue);
        }
    }

    private <T, R> Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from, final Path<T> path
            , final Attribute<? super T, R> attribute, final String property, final Object filterValue) {
        if (attribute instanceof SingularAttribute) {
            final SingularAttribute<T, R> singularAttribute = (SingularAttribute<T, R>) attribute;
            final Type<R> modelType = singularAttribute.getType();
            if (modelType instanceof ManagedType) {
                return buildPredicate(
                        builder
                        , from
                        , path.get(singularAttribute)
                        , (ManagedType<R>) modelType
                        , property
                        , filterValue
                );
            } else if (modelType instanceof BasicType) {
                return setAttribute(
                        builder
                        , path
                        , singularAttribute
                        , filterValue
                );
            }
        }
        if (attribute instanceof ListAttribute) {
            final ListAttributeJoin<T, R> join = (ListAttributeJoin<T, R>) from.join((ListAttribute) attribute);
            final Type<R> modelType = join.getAttribute().getElementType();
            if (modelType instanceof ManagedType) {
                return buildPredicate(
                        builder
                        , join
                        , join
                        , (ManagedType<R>) modelType
                        , property
                        , filterValue
                );
            }
        }
        return null;
    }

    private <T> Predicate setProperty(final CriteriaBuilder builder, final Path<T> path
            , final ManagedType<T> modelType, final String property, final Object filterValue) {
        final SingularAttribute<? super T, ?> attribute = modelType.getSingularAttribute(property);
        return setAttribute(
                builder
                , path
                , attribute
                , filterValue
        );
    }

    private <T> Predicate setAttributes(final CriteriaBuilder builder, final Path<T> path
            , final ManagedType<T> modelType, final Object filterValue) {
        final List<Predicate> predicates = new ArrayList<>();
        for (SingularAttribute<? super T, ?> attribute : modelType.getSingularAttributes()) {
            final Predicate predicate = setAttribute(
                    builder
                    , path
                    , attribute
                    , filterValue
            );
            if (predicate != null) {
                predicates.add(predicate);
            }
        }
        return builder.or(predicates.toArray(new Predicate[0]));
    }

    private <T, R> Predicate setAttribute(final CriteriaBuilder builder, final Path<T> path
            , final SingularAttribute<? super T, R> attribute, final Object value) {
        Path<R> valuePath = path.get(attribute);
        Type<R> attributeType = attribute.getType();
        if (attributeType instanceof IdentifiableType) {
            SingularAttribute<? super R, ?> attributeId = ((IdentifiableType<R>) attributeType).getSingularAttribute(ID);
            return setAttributeValue(
                    builder
                    , valuePath.get(attributeId)
                    , attributeId.getType().getJavaType()
                    , attribute.isId()
                    , value
            );
        }
        return setAttributeValue(
                builder
                , valuePath
                , attributeType.getJavaType()
                , attribute.isId()
                , value
        );
    }

    private Predicate setAttributeValue(final CriteriaBuilder builder, final Path<?> path
            , final Class<?> type, final boolean isId, final Object value) {
        if (value == null) {
            return builder.isNull(path);
        }
        if (isId) {
            final Object id = formatIdentifier(value, type);
            if (id != null) {
                return builder.equal(path, id);
            }
        }
        if (type.isEnum() && value instanceof Enum) {
            return builder.equal(path, ((Enum<?>) value).name());
        }
        return builder.like(getValue(path, builder), "%" + String.valueOf(value).toUpperCase() + "%");
    }

    private Object formatIdentifier(final Object value, final Class<?> attributeType) {
        if (value == null) {
            return null;
        }
        if (attributeType != null && !attributeType.isInstance(value)) {
            final String id = value.toString();
            try {
                return formatValue(id, getEnum(attributeType));
            } catch (final IllegalArgumentException iae) {
                // TODO Add compatible log
                return null;
            }
        }
        return value;
    }

    private Object formatValue(final String id, final CrudConstant.TypeId type) throws IllegalArgumentException {
        switch (type) {
            case UUID:
                return UUID.fromString(id);
            case LONG:
                return Long.valueOf(id);
            case INTEGER:
                return Integer.valueOf(id);
            case STRING:
                return id;
            default:
        }
        return null;
    }

    private CrudConstant.TypeId getEnum(final Class<?> attributeType) {
        if (attributeType == UUID.class) {
            return CrudConstant.TypeId.UUID;
        } else if (attributeType == Long.class) {
            return CrudConstant.TypeId.LONG;
        } else if (attributeType == String.class) {
            return CrudConstant.TypeId.STRING;
        } else if (attributeType == Integer.class) {
            return CrudConstant.TypeId.INTEGER;
        } else {
            return CrudConstant.TypeId.OTHER;
        }
    }

    private Expression<String> getValue(final Path<?> path, final CriteriaBuilder builder) {
        return Date.class.equals(path.getJavaType()) ? formatDate(path, builder) : formatString(path, builder);
    }

    private Expression<String> formatDate(final Path<?> path, final CriteriaBuilder builder) {
        return builder.function("TO_CHAR", String.class, path, builder.literal("DD/MM/YYYY"));
    }

    private Expression<String> formatString(final Path<?> path, final CriteriaBuilder builder) {
        return builder.upper(path.as(String.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Entity find(final Id id) {
        return repository.getOne(id);
    }

    @Override
    @Transactional
    public Entity create(final Entity obj) {
        if (obj instanceof ICrudEntity) {
            ((ICrudEntity<?>) obj).setId(null);
        }
        return update(obj);
    }

    @Override
    @Transactional
    public Entity update(final Id id, final Map<String, Object> fields) {
        final Entity entity = repository.getOne(id);
        CopyUtil.copyProperties(entity, fields, mapper);
        return update(entity);
    }

    protected Entity update(final Entity entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(final Id id) {
        repository.deleteById(id);
    }
}
