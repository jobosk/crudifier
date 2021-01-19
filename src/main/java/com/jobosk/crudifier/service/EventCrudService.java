package com.jobosk.crudifier.service;

import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
import org.springframework.transaction.annotation.Transactional;

public abstract class EventCrudService<Entity, Id> extends CrudService<Entity, Id> {

    private final GenericSupplier<Entity> supplierCreate;
    private final GenericSupplier<Entity> supplierUpdate;
    private final GenericSupplier<Id> supplierDelete;

    public EventCrudService(
            final GenericRepository<Entity, Id> repository
            , final GenericSupplier<Entity> supplierCreate
            , final GenericSupplier<Entity> supplierUpdate
            , final GenericSupplier<Id> supplierDelete
    ) {
        super(repository);
        this.supplierCreate = supplierCreate;
        this.supplierUpdate = supplierUpdate;
        this.supplierDelete = supplierDelete;
    }

    public EventCrudService(final GenericRepository<Entity, Id> repository) {
        this(repository, null, null, null);
    }

    @Override
    @Transactional
    public Entity create(final Entity obj) {
        final Entity result = super.create(obj);
        if (supplierCreate != null) {
            supplierCreate.getProcessor().onNext(result);
        }
        return result;
    }

    protected Entity update(final Entity entity) {
        final Entity result = super.update(entity);
        if (supplierUpdate != null) {
            supplierUpdate.getProcessor().onNext(result);
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(final Id id) {
        super.delete(id);
        if (supplierDelete != null) {
            supplierDelete.getProcessor().onNext(id);
        }
    }
}
