package com.jobosk.crudifier.service;

import com.jobosk.crudifier.exception.CrudException;
import com.jobosk.crudifier.repository.GenericRepository;
import com.jobosk.crudifier.supplier.GenericSupplier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public abstract class IndexedCrudService<Entity, Id, Index> extends EventCrudService<Entity, Id> {

    private final ElasticsearchRepository<Index, Id> elasticsearchRepository;
    private final Class<Index> elasticIndexType;

    public IndexedCrudService(
            final GenericRepository<Entity, Id> repository
            , final GenericSupplier<Entity> supplierCreate
            , final GenericSupplier<Entity> supplierUpdate
            , final GenericSupplier<Id> supplierDelete
            , final ElasticsearchRepository<Index, Id> elasticsearchRepository
            , final Class<Index> elasticIndexType
    ) {
        super(repository, supplierCreate, supplierUpdate, supplierDelete);
        this.elasticsearchRepository = elasticsearchRepository;
        this.elasticIndexType = elasticIndexType;
    }

    public IndexedCrudService(
            final GenericRepository<Entity, Id> repository
            , final ElasticsearchRepository<Index, Id> elasticsearchRepository
            , final Class<Index> elasticIndexType
    ) {
        this(repository, null, null, null, elasticsearchRepository, elasticIndexType);
    }

    @Override
    protected Entity update(final Entity obj) {
        final Entity result = super.update(obj);
        if (elasticsearchRepository != null && elasticIndexType != null) {
            elasticsearchRepository.save(getIndex(result));
        }
        return result;
    }

    private Index getIndex(final Entity entity) {
        Index result;
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
