package com.planimrt.repository;

import com.planimrt.model.History;
import com.planimrt.model.OperationType;
import com.planimrt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findByUser(User user);

    List<History> findByOperationType(OperationType operationType);

    List<History> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT h FROM History h WHERE h.timestamp >= :startDate AND h.timestamp <= :endDate ORDER BY h.timestamp DESC")
    List<History> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT h FROM History h WHERE h.user.id = :userId AND h.operationType = :operationType")
    List<History> findByUserIdAndOperationType(@Param("userId") Long userId,
                                               @Param("operationType") OperationType operationType);
}