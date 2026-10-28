package com.ledgerflow.ledger.persistence;

import com.ledgerflow.ledger.domain.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<Operation, UUID> {
    Optional<Operation> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
