package com.taxi.notification.repository;

import com.taxi.notification.model.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
    List<NotificationTask> findByTripId(Long tripId);

    List<NotificationTask> findByStatusOrderByCreatedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM NotificationTask t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC LIMIT 1")
    Optional<NotificationTask> findNextPendingTask();

    @Modifying
    @Query("UPDATE NotificationTask t SET t.status = :status, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);
}
