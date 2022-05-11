package com.jobosk.crudifier.service;

import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public abstract class EventCrudService<Entity, Id> extends CrudService<Entity, Id> {

  private final GenericSupplier<Entity> createSupplier;
  private final GenericSupplier<Entity> updateSupplier;
  private final GenericSupplier<Id> deleteSupplier;

  public EventCrudService(
      final GenericRepository<Entity, Id> repository
      , final GenericSupplier<Entity> createSupplier
      , final GenericSupplier<Entity> updateSupplier
      , final GenericSupplier<Id> deleteSupplier
  ) {
    super(repository);
    this.createSupplier = createSupplier;
    this.updateSupplier = updateSupplier;
    this.deleteSupplier = deleteSupplier;
  }

  public EventCrudService(final GenericRepository<Entity, Id> repository) {
    this(repository, null, null, null);
  }

  @Override
  @Transactional
  public Entity create(final Entity entity) {
    final Entity result = super.create(entity);
    if (createSupplier != null) {
      createSupplier.getProcessor().onNext(result);
    }
    return result;
  }

  @Override
  @Transactional
  public Entity update(final Entity entity, final Map<String, Object> fields) {
    final Entity result = super.update(entity, fields);
    if (updateSupplier != null) {
      updateSupplier.getProcessor().onNext(result);
    }
    return result;
  }

  @Override
  @Transactional
  public boolean delete(final Id id) {
    super.delete(id);
    if (deleteSupplier != null) {
      deleteSupplier.getProcessor().onNext(id);
    }
    return true;
  }
}
