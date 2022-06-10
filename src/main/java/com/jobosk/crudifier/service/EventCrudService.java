package com.jobosk.crudifier.service;

import com.jobosk.crudifier.messaging.MessageBridge;
import com.jobosk.crudifier.repository.GenericRepository;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public abstract class EventCrudService<Entity, Id> extends CrudService<Entity, Id> {

  private final MessageBridge<Entity> createSupplier;
  private final MessageBridge<Entity> updateSupplier;
  private final MessageBridge<Id> deleteSupplier;

  public EventCrudService(
      final GenericRepository<Entity, Id> repository
      , final MessageBridge<Entity> createSupplier
      , final MessageBridge<Entity> updateSupplier
      , final MessageBridge<Id> deleteSupplier
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
      createSupplier.pushMessage(result);
    }
    return result;
  }

  @Override
  @Transactional
  public Entity update(final Entity entity, final Map<String, Object> fields) {
    final Entity result = super.update(entity, fields);
    if (updateSupplier != null) {
      updateSupplier.pushMessage(result);
    }
    return result;
  }

  @Override
  @Transactional
  public boolean delete(final Id id) {
    super.delete(id);
    if (deleteSupplier != null) {
      deleteSupplier.pushMessage(id);
    }
    return true;
  }
}
