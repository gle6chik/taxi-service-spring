package com.taxi.notification;

import com.taxi.notification.model.NotificationTask;
import com.taxi.notification.repository.NotificationTaskRepository;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationTaskRepository repository;

    public NotificationController(NotificationTaskRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public NotificationTask create(@RequestBody NotificationRequest request) {
        NotificationTask task = new NotificationTask();
        task.setTripId(request.getTripId());
        task.setRecipientId(request.getRecipientId());
        task.setRecipientType(request.getRecipientType());
        task.setMessage(request.getMessage());
        task.setStatus("PENDING");
        return repository.save(task);
    }

    @GetMapping
    public List<NotificationTask> getByTripId(@RequestParam("trip_id") Long tripId) {
        return repository.findByTripId(tripId);
    }

    @Data
    static class NotificationRequest {
        private Long tripId;
        private Long recipientId;
        private String recipientType;
        private String message;
    }
}
