package com.jobosk.crudifier.service;

import com.jobosk.crudifier.exception.CrudException;
import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public abstract class IndexedCrudService<Entity, Id, Indexed> extends EventCrudService<Entity, Id> {

    private final ElasticsearchRepository<Indexed, Id> elasticsearchRepository;
    private final Class<Indexed> elasticIndexType;

    public IndexedCrudService(
            final GenericRepository<Entity, Id> repository
            , final GenericSupplier<Entity> supplierCreate
            , final GenericSupplier<Entity> supplierUpdate
            , final GenericSupplier<Id> supplierDelete
            , final ElasticsearchRepository<Indexed, Id> elasticsearchRepository
            , final Class<Indexed> elasticIndexType
    ) {
        super(repository, supplierCreate, supplierUpdate, supplierDelete);
        this.elasticsearchRepository = elasticsearchRepository;
        this.elasticIndexType = elasticIndexType;
    }

    public IndexedCrudService(
            final GenericRepository<Entity, Id> repository
            , final ElasticsearchRepository<Indexed, Id> elasticsearchRepository
            , final Class<Indexed> elasticIndexType
    ) {
        this(repository, null, null, null, elasticsearchRepository, elasticIndexType);
    }

    @Override
    protected Entity update(final Entity obj) {
        final Entity result = super.update(obj);
        if (elasticsearchRepository != null && elasticIndexType != null) {
            elasticsearchRepository.save(getIndexedEntity(result));
        }
        return result;
    }

    protected Indexed getIndexedEntity(final Entity entity) {
        Indexed result;
        try {
            result = mapper.convertValue(entity, elasticIndexType);
        } catch (final Exception e) {
            throw new CrudException(e, "CRUD entity cannot be mapped to indexed type");
        }
        return result;
    }

    @Override
    public void delete(final Id id) {
        super.delete(id);
        if (elasticsearchRepository != null) {
            elasticsearchRepository.deleteById(id);
        }
    }

}
