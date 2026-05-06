package com.taxi.notification.repository;

import com.taxi.notification.model.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
    List<NotificationTask> findByTripId(Long tripId);
    List<NotificationTask> findByStatus(String status);
}
