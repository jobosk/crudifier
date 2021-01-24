package com.jobosk.crudifier.repository;

public interface IndexedRepository<Entity, Id, Indexed> {

    <S extends Entity> S save(S entity);

    void deleteById(final Id id);

    Indexed getIndexedEntity(Entity entity);
}
