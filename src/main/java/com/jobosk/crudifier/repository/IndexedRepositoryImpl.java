package com.jobosk.crudifier.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.exception.CrudException;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;

public abstract class IndexedRepositoryImpl<Entity, Id, Indexed> extends SimpleJpaRepository<Entity, Id>
        implements IndexedRepository<Entity, Id, Indexed> {

    private ElasticsearchRepository<Indexed, Id> indexRepository;
    private Class<Indexed> indexedType;
    private ObjectMapper mapper;

    public IndexedRepositoryImpl(
            final Class<Entity> entityType
            , final EntityManager entityManager
            , final ElasticsearchRepository<Indexed, Id> indexRepository
            , final Class<Indexed> indexedType
            , final ObjectMapper mapper
    ) {
        super(entityType, entityManager);
        this.indexRepository = indexRepository;
        this.indexedType = indexedType;
        this.mapper = mapper;
    }

    @Override
    public <S extends Entity> S save(S entity) {
        final S result = super.save(entity);
        if (indexRepository != null && indexedType != null) {
            indexRepository.save(getIndexedEntity(result));
        }
        return result;
    }

    @Override
    public Indexed getIndexedEntity(final Entity entity) {
        Indexed result;
        try {
            result = mapper.convertValue(entity, indexedType);
        } catch (final Exception e) {
            throw new CrudException(e, "CRUD entity cannot be mapped to indexed type");
        }
        return result;
    }

    @Override
    public void deleteById(final Id id) {
        super.deleteById(id);
        if (indexRepository != null) {
            indexRepository.deleteById(id);
        }
    }
}
