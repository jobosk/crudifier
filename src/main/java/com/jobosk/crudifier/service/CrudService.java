package com.jobosk.crudifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.annotation.FindExcluded;
import com.jobosk.crudifier.constant.CrudConstant;
import com.jobosk.crudifier.entity.ICrudEntity;
import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.util.CopyUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.BasicType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.query.criteria.internal.path.ListAttributeJoin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

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

  @Override
  @Transactional(readOnly = true)
  public Collection<Entity> findAll(final Map<String, String> parameters
      , final HttpServletResponse response) {
    Collection<Entity> result;
    final Sort sort = getSort(getParameter(parameters, "order"));
    final Optional<Integer> page = getInteger(getParameter(parameters, "page"));
    final Optional<Integer> size = getInteger(getParameter(parameters, "size"));
    if (page.isPresent() && size.isPresent()) {
      final Pageable pageRequest = getPageRequest(page.get(), size.get(), sort);
      Page<Entity> pagedResult = find(parameters, pageRequest);
      response.addHeader(CrudConstant.Http.Header.TOTAL_COUNT,
          String.valueOf(pagedResult.getTotalElements()));
      response
          .addHeader(CrudConstant.Http.Header.EXPOSE_HEADER, CrudConstant.Http.Header.TOTAL_COUNT);
      result = pagedResult.getContent();
    } else {
      result = sort != null ? find(parameters, sort) : find(parameters);
    }
    return result;
  }

  protected Sort getSort(final Object sort) {
    if (sort == null) {
      return null;
    }
    final String[] list = sort.toString().split(",");
    if (list.length == 0) {
      return null;
    }
    Sort result = getDirectionSort(list[0]);
    for (int i = 1; i < list.length; i++) {
      result = result.and(getDirectionSort(list[i]));
    }
    return result;
  }

  protected Optional<Integer> getInteger(final String value) {
    return Optional.ofNullable(value).map(v -> {
      try {
        return Integer.valueOf(v);
      } catch (final Exception e) {
        throw new RuntimeException(
            "Invalid value for integer parameter: " + value); // TODO Codificar error
      }
    });
  }

  protected String getParameter(final Map<String, String> parameters, final String key) {
    if (key == null || parameters == null) {
      return null;
    }
    final String result = parameters.get(key);
    parameters.entrySet().removeIf(e -> key.equals(e.getKey()));
    return result;
  }

  private Sort getDirectionSort(final String value) {
    final boolean isDesc;
    final String sort;
    if (value != null && value.charAt(0) == '-') {
      isDesc = true;
      sort = value.substring(1);
    } else {
      isDesc = false;
      sort = value;
    }
    return Sort.by((isDesc ? Sort.Direction.DESC : Sort.Direction.ASC), sort);
  }

  protected Pageable getPageRequest(final int page, final int size, final Sort sort) {
    return sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
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

  private <T> void addPredicate(final List<Predicate> predicates, final CriteriaBuilder builder
      , final From<?, ?> from, final Path<T> path, final ManagedType<T> modelType
      , final String property, final Object filterValue) {
    final Predicate predicate;
    if ("genericSearch".equals(property)) {
      predicate = buildGenericPredicates(
          builder
          , from
          , modelType
          , filterValue
          , new HashSet<>()
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

  private <T> Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from
      , final Path<T> path, final ManagedType<T> modelType, final String property
      , final Object filterValue) {
    if (property == null || filterValue == null) {
      return null;
    }
    return buildPredicateProperty(
        builder
        , from
        , path
        , modelType
        , property
        , filterValue
    );
  }

  private <T> Predicate buildPredicateProperty(final CriteriaBuilder builder
      , final From<?, ?> from, final Path<T> path, final ManagedType<T> modelType
      , final String property, final Object filterValue) {
    final int index = property.indexOf(SEPARATOR);
    if (index != -1) {
      final Attribute<? super T, ?> attribute = modelType.getAttribute(
          property.substring(0, index)
      );
      if (excludedFromFind(attribute)) {
        return null;
      }
      return buildPredicate(
          builder
          , from
          , path
          , attribute
          , property.substring(index + 1)
          , filterValue
      );
    }
    return setProperty(builder, path, modelType, property, filterValue);
  }

  private <T> boolean excludedFromFind(final Attribute<? super T, ?> attribute) {
    return Optional.ofNullable(attribute)
        .map(Attribute::getJavaMember)
        .map(Member::getDeclaringClass)
        .map(dc -> getField(dc, attribute.getName()))
        .map(f -> f.getAnnotation(FindExcluded.class))
        .isPresent();
  }

  private Field getField(final Class<?> fieldDeclaringClass, final String fieldName) {
    try {
      return fieldDeclaringClass.getDeclaredField(fieldName);
    } catch (final NoSuchFieldException nsfe) {
      return null;
    }
  }

  private <T, R> Predicate buildPredicate(final CriteriaBuilder builder, final From<?, ?> from
      , final Path<T> path, final Attribute<? super T, R> attribute, final String property
      , final Object filterValue) {
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
            , path.get(singularAttribute)
            , singularAttribute
            , filterValue
        );
      }
    }
    if (attribute instanceof ListAttribute) {
      final ListAttributeJoin<T, R> join = (ListAttributeJoin<T, R>) from
          .join((ListAttribute) attribute);
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

  private <T, R> Predicate setProperty(final CriteriaBuilder builder, final Path<T> path
      , final ManagedType<T> modelType, final String property, final Object filterValue) {
    final SingularAttribute<T, R> attribute = (SingularAttribute<T, R>) modelType
        .getSingularAttribute(property);
    return setAttribute(
        builder
        , path.get(attribute)
        , attribute
        , filterValue
    );
  }

  private <T> Predicate buildGenericPredicates(final CriteriaBuilder builder
      , final From<?, ?> from, final ManagedType<T> modelType, final Object filterValue
      , final Set<ManagedType<?>> visited) {
    if (!visited.contains(modelType)) {
      visited.add(modelType);
    } else {
      return null;
    }
    final List<Predicate> predicates = new ArrayList<>();
    for (SingularAttribute<? super T, ?> attribute : modelType.getSingularAttributes()) {
      if (excludedFromFind(attribute)) {
        continue;
      }
      final Predicate predicate = buildGenericPredicate(
          builder
          , from
          , attribute
          , filterValue
          , visited
      );
      if (predicate != null) {
        predicates.add(predicate);
      }
    }
    return builder.or(predicates.toArray(new Predicate[0]));
  }

  private <T, R> Predicate buildGenericPredicate(final CriteriaBuilder builder
      , final From<?, ?> from, final SingularAttribute<? super T, R> attribute
      , final Object filterValue, final Set<ManagedType<?>> visited) {
    final Type<R> modelType = attribute.getType();
    if (modelType instanceof ManagedType) {
      return buildGenericPredicates(
          builder
          , from.join(attribute.getName(), JoinType.LEFT)
          , (ManagedType) modelType
          , filterValue
          , visited
      );
    }
    if (modelType instanceof BasicType) {
      return setAttribute(
          builder
          , from.get(attribute.getName())
          , attribute
          , filterValue
      );
    }
    return null;
  }

  private <T, R> Predicate setAttribute(final CriteriaBuilder builder, final Path<R> path
      , final SingularAttribute<? super T, R> attribute, final Object value) {
    Type<R> attributeType = attribute.getType();
    if (attributeType instanceof IdentifiableType) {
      SingularAttribute<? super R, ?> attributeId = ((IdentifiableType<R>) attributeType)
          .getSingularAttribute(ID);
      return setAttributeValue(
          builder
          , path.get(attributeId)
          , attributeId.getType().getJavaType()
          , attribute.isId()
          , value
      );
    }
    return setAttributeValue(
        builder
        , path
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

  private Object formatValue(final String id, final CrudConstant.TypeId type)
      throws IllegalArgumentException {
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
    return Date.class.equals(path.getJavaType()) ? formatDate(path, builder)
        : formatString(path, builder);
  }

  private Expression<String> formatDate(final Path<?> path, final CriteriaBuilder builder) {
    return builder.function("TO_CHAR", String.class, path, builder.literal("DD/MM/YYYY"));
  }

  private Expression<String> formatString(final Path<?> path, final CriteriaBuilder builder) {
    return builder.upper(path.as(String.class));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Entity> find(final Id id) {
    return repository.findById(id);
  }

  @Override
  @Transactional
  public Entity create(final Entity entity) {
    if (entity instanceof ICrudEntity) {
      ((ICrudEntity<?>) entity).setId(null);
    }
    return update(entity);
  }

  @Override
  @Transactional
  public Entity update(final Entity entity, final Map<String, Object> fields) {
    CopyUtil.copyProperties(entity, fields, mapper);
    return update(entity);
  }

  protected Entity update(final Entity entity) {
    return repository.save(entity);
  }

  @Override
  @Transactional
  public boolean delete(final Id id) {
    repository.deleteById(id);
    return true;
  }
}
