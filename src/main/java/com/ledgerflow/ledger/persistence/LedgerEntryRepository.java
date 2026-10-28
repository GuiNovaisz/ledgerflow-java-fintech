package com.ledgerflow.ledger.persistence;

import com.ledgerflow.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    @EntityGraph(attributePaths = "operation")
    Page<LedgerEntry> findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID accountId, Instant from, Instant to, Pageable pageable);
}
