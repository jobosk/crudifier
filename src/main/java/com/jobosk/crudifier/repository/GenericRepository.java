package com.jobosk.crudifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GenericRepository<Entity, Id> extends JpaRepository<Entity, Id>, JpaSpecificationExecutor<Entity> {
}
