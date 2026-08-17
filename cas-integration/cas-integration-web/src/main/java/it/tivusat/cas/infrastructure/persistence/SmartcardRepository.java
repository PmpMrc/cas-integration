package it.tivusat.cas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartcardRepository extends JpaRepository<SmartcardEntity, String> {
}