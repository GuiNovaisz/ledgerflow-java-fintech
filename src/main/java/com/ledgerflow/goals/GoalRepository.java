package com.ledgerflow.goals;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByOwnerIdOrderByCreatedAtDescIdDesc(UUID ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id=:id and g.ownerId=:owner")
    Optional<Goal> lockOwned(UUID id, UUID owner);
}
