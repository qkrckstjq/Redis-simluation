package com.example.world.websocket;

import com.example.world.entity.Tick;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimulationWebSocketHandler handler;

    public void sendSnapShots(Tick snapshotDtoList) {
        handler.broadcast(snapshotDtoList);
    }
}
