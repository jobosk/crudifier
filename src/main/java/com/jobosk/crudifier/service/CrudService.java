package com.jobosk.crudifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
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
public abstract class CrudService<Entity> implements ICrudService<Entity> {

    private static final String ID = "id";
    private static final String SEPARATOR = ".";

    private final GenericRepository<Entity> repository;
    private final GenericSupplier<Entity> supplierCreate;
    private final GenericSupplier<Entity> supplierUpdate;
    private final GenericSupplier<UUID> supplierDelete;

    @Autowired
    private ObjectMapper mapper;
    //protected DozerBeanMapper mapper;

    public CrudService(
            final GenericRepository<Entity> repository
            , final GenericSupplier<Entity> supplierCreate
            , final GenericSupplier<Entity> supplierUpdate
            , final GenericSupplier<UUID> supplierDelete
    ) {
        this.repository = repository;
        this.supplierCreate = supplierCreate;
        this.supplierUpdate = supplierUpdate;
        this.supplierDelete = supplierDelete;
    }

    public CrudService(final GenericRepository<Entity> repository) {
        this(repository, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Map<String, Object> filters) {
        return repository.findAll(getSpecification(filters));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Map<String, Object> filters, final Sort sort) {
        return repository.findAll(getSpecification(filters), sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Entity> find(final Map<String, Object> filters, final Pageable pageable) {
        return repository.findAll(getSpecification(filters), pageable);
    }

    private Specification<Entity> getSpecification(final Map<String, Object> filters) {
        return (Specification<Entity>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            filters.forEach((key, value) -> {
                final Predicate predicate = buildPredicate(
                        builder
                        , root
                        , root
                        , root.getModel()
                        , key
                        , value
                );
                if (predicate != null) {
                    predicates.add(predicate);
                }
            });
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from, final Path<?> path
            , final EntityType<?> entityType, final String property, final Object value) {
        if (value == null) {
            return null;
        }
        final int index = property != null ? property.indexOf(SEPARATOR) : -1;
        if (index == -1) {
            final SingularAttribute<?, ?> attribute = getSingularAttribute(entityType, property);
            return getAttributeValue(
                    builder
                    , getSinglePath(path, attribute)
                    , value
                    , attribute.isId()
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
                    , value
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
                    , value
            );
        }
        return null;
    }

    private SingularAttribute<?, ?> getSingularAttribute(final EntityType<?> entityType, final String property) {
        SingularAttribute<?, ?> attribute = entityType.getSingularAttribute(property);
        final Type<?> attributeType = attribute.getType();
        if (attributeType instanceof EntityType) {
            attribute = ((EntityType<?>) attributeType).getDeclaredSingularAttribute(ID);
        }
        return attribute;
    }

    private Predicate getAttributeValue(final CriteriaBuilder builder, final Path<?> path
            , final Object filterValue, final boolean isId) {
        if ("_NULL_".equals(filterValue)) {
            return builder.isNull(path);
        } else if (filterValue instanceof Enum) {
            return buildEnumPredicate(path, builder, filterValue);
        } else if (isId) {
            return builder.equal(path, filterValue);
        } else {
            return builder.like(getValue(path, builder), "%" + String.valueOf(filterValue).toUpperCase() + "%");
        }
    }

    private Expression<String> getValue(final Path<?> path, final CriteriaBuilder builder) {
        return Date.class.equals(path.getJavaType()) ? formatDate(path, builder) : builder.upper(path.as(String.class));
    }

    private Expression<String> formatDate(final Path<?> path, final CriteriaBuilder builder) {
        return builder.function("TO_CHAR", String.class, path, builder.literal("DD/MM/YYYY"));
    }

    private Predicate buildEnumPredicate(final Path<?> path, final CriteriaBuilder builder, final Object filterValue) {
        return builder.like(path.as(String.class), "%" + ((Enum<?>) filterValue).name() + "%");
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
    public Entity find(final UUID id) {
        return repository.getOne(id);
    }

    @Override
    @Transactional
    public Entity create(final Entity obj) {
        final Entity result = repository.save(obj);
        if (supplierCreate != null) {
            supplierCreate.getProcessor().onNext(result);
        }
        return result;
    }

    @Override
    @Transactional
    public Entity update(final UUID id, final Map<String, Object> fields) {
        final Entity entity = repository.getOne(id);
        CopyUtil.copyProperties(entity, fields, mapper);
        final Entity result = repository.save(entity);
        if (supplierUpdate != null) {
            supplierUpdate.getProcessor().onNext(result);
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(final UUID id) {
        repository.deleteById(id);
        if (supplierDelete != null) {
            supplierDelete.getProcessor().onNext(id);
        }
    }
}
