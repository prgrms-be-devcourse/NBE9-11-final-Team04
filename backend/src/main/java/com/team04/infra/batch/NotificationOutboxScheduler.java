package com.team04.infra.batch;

import com.team04.domain.notification.service.NotificationOutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxScheduler {

    private final NotificationOutboxProcessor processor;

    @Scheduled(fixedDelay = 10_000)
    public void process() {
        processor.findPendingIds()
                .forEach(id -> {
                    try {
                        processor.processOne(id);
                    } catch (Exception e) {
                        log.error("[OutboxScheduler] 예기치 않은 오류 id={}", id, e);
                    }
                });
    }
}
