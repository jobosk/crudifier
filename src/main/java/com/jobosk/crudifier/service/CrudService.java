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
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ListAttribute;
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

    private void addPredicate(final List<Predicate> predicates, final CriteriaBuilder builder, final From<?, ?> from
            , final Path<?> path, final EntityType<?> entityType, final String property, final Object filterValue) {
        final Predicate predicate = buildPredicate(
                builder
                , from
                , path
                , entityType
                , property
                , filterValue
        );
        if (predicate != null) {
            predicates.add(predicate);
        }
    }

    private Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from, final Path<?> path
            , final EntityType<?> entityType, final String property, final Object filterValue) {
        if (filterValue == null) {
            return null;
        }
        final int index = property != null ? property.indexOf(SEPARATOR) : -1;
        if (index == -1) {
            return getAttributeValue(
                    builder
                    , path
                    , entityType
                    , property
                    , filterValue
            );
        }
        Attribute<?, ?> attribute = entityType.getAttribute(
                property.substring(0, index)
        );
        if (attribute instanceof SingularAttribute) {
            final SingularAttribute<?, ?> singularAttribute = (SingularAttribute<?, ?>) attribute;
            return buildPredicate(
                    builder
                    , from
                    , getSinglePath(path, singularAttribute)
                    , (EntityType<?>) singularAttribute.getType()
                    , property.substring(index + 1)
                    , filterValue
            );
        }
        if (attribute instanceof ListAttribute) {
            final ListAttribute<?, ?> listAttribute = (ListAttribute<?, ?>) attribute;
            final ListAttributeJoin<?, ?> join = (ListAttributeJoin<?, ?>) getJoin(from, listAttribute);
            return buildPredicate(
                    builder
                    , join
                    , join
                    , (EntityType<?>) join.getAttribute().getElementType()
                    , property.substring(index + 1)
                    , filterValue
            );
        }
        return null;
    }

    private Predicate getAttributeValue(final CriteriaBuilder builder, final Path<?> path
            , final EntityType<?> entityType, final String property, final Object value) {
        SingularAttribute<?, ?> attribute = entityType.getSingularAttribute(property);
        Path<?> valuePath = getSinglePath(path, attribute);
        Type<?> attributeType = attribute.getType();
        if (attributeType instanceof EntityType) {
            attribute = ((EntityType<?>) attributeType).getDeclaredSingularAttribute(ID);
            valuePath = getSinglePath(valuePath, attribute);
            attributeType = attribute.getType();
        }
        if (value == null) {
            return builder.isNull(valuePath);
        }
        final Class<?> type = attributeType.getJavaType();
        if (attribute.isId()) {
            return builder.equal(valuePath, formatIdentifier(value, type));
        }
        if (type.isEnum() && value instanceof Enum) {
            return builder.equal(path.as(String.class), ((Enum<?>) value).name());
        }
        return builder.like(getValue(valuePath, builder), "%" + String.valueOf(value).toUpperCase() + "%");
    }

    private Object formatIdentifier(final Object value, final Class<?> attributeType) {
        if (value == null) {
            return null;
        }
        if (attributeType != null && !attributeType.isInstance(value)) {
            final String id = value.toString();
            switch (getEnum(attributeType)) {
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
        }
        return value;
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
        return Date.class.equals(path.getJavaType()) ? formatDate(path, builder) : builder.upper(path.as(String.class));
    }

    private Expression<String> formatDate(final Path<?> path, final CriteriaBuilder builder) {
        return builder.function("TO_CHAR", String.class, path, builder.literal("DD/MM/YYYY"));
    }

    @SuppressWarnings("unchecked")
    private <X, Y> Path<Y> getSinglePath(final Path<X> path, final SingularAttribute<?, ?> attribute) {
        return path.get((SingularAttribute<? super X, Y>) attribute);
    }

    @SuppressWarnings("unchecked")
    private <X, Y, Z> ListJoin<X, Y> getJoin(final From<Z, X> from, final ListAttribute<?, ?> listAttribute) {
        return from.join((ListAttribute<? super X, Y>) listAttribute);
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
