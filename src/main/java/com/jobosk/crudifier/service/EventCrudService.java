package com.jobosk.crudifier.service;

import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    public Entity create(final Entity obj) {
        final Entity result = super.create(obj);
        if (createSupplier != null) {
            createSupplier.getProcessor().onNext(result);
        }
        return result;
    }

    @Override
    @Transactional
    public Entity update(final Id id, final Map<String, Object> fields) {
        final Entity result = super.update(id, fields);
        if (updateSupplier != null) {
            updateSupplier.getProcessor().onNext(result);
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(final Id id) {
        super.delete(id);
        if (deleteSupplier != null) {
            deleteSupplier.getProcessor().onNext(id);
        }
    }
}
