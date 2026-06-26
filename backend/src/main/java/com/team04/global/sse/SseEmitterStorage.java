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

    // 콜백 등록 시점과 실제 실행 시점 사이에 새 emitter가 추가된 경우 새 emitter를 지우지 않도록 보호
    public void removeIfSame(Long userId, SseEmitter emitter){
        emitters.remove(userId, emitter);
    }

    public SseEmitter get(Long userId){
        return emitters.get(userId);
    }
}
