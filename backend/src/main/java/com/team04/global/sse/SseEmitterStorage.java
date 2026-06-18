package com.team04.global.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterStorage {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void add(Long userId, SseEmitter emitter){
        emitters.put(userId, emitter);
    }

    public void remove(Long userId){
        emitters.remove(userId);
    }

    public SseEmitter get(Long id){
        return emitters.get(id);
    }
}
