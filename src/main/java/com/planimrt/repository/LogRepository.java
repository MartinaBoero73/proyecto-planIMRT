package com.planimrt.repository;

import com.planimrt.model.EventType;
import com.planimrt.model.Log;
import com.planimrt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    List<Log> findByUser(User user);

    List<Log> findByEventType(EventType eventType);

    List<Log> findBySeverity(String severity);

    @Query("SELECT l FROM Log l WHERE l.timestamp >= :startDate AND l.timestamp <= :endDate ORDER BY l.timestamp DESC")
    List<Log> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT l FROM Log l WHERE l.user.id = :userId ORDER BY l.timestamp DESC")
    List<Log> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
}