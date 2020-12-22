package com.jobosk.crudifier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface GenericRepository<Entity> extends JpaRepository<Entity, UUID>, JpaSpecificationExecutor<Entity> {
}
