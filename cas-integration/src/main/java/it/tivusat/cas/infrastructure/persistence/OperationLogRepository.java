package it.tivusat.cas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationLogRepository extends JpaRepository<OperationLogEntity, UUID> {

    List<OperationLogEntity> findBySmartcardSnOrderByCreatedAtDesc(String smartcardSn);
}