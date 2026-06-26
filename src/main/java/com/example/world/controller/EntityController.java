package com.example.world.controller;

import com.example.world.entity.EntityHistoryDto;
import com.example.world.entity.RedisGeo;
import com.example.world.event.EntityEventScheduler;
import com.example.world.service.EntityService;
import com.example.world.stream.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entity")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EntityController {
    private final EntityService entityService;
    private final HistoryService historyService;

    @PostMapping("/")
    public ResponseEntity<String> createEntity(
            @RequestParam String type,
            @RequestParam String name,
            @RequestParam int hp,
            @RequestParam int x,
            @RequestParam int y
    ) {
        entityService.createEntity(type, name, hp, x, y);
        return new ResponseEntity<>("앤티티 생성 성공",HttpStatus.CREATED);
    }

    @PostMapping("/spawn")
    public ResponseEntity<String> createEntityAmount(
            @RequestParam String type,
            @RequestParam int count
    ) {
        entityService.createEntityAmount(type, count);
        return new ResponseEntity<>(type + "타입" + " 앤티티 " + count + " 생성 성공",HttpStatus.CREATED);
    }

    @GetMapping("/geo/near")
    public ResponseEntity<List<RedisGeo>> getNearEntities(
            @RequestParam Long entity,
            @RequestParam double radius
    ) {
        List<RedisGeo> result = entityService.getNearEntities(entity, radius);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/geo/dist")
    public ResponseEntity<Double> getDistBetEntities(
            @RequestParam Long entity1,
            @RequestParam Long entity2
    ) {
        Double result = entityService.getDistBetEntities(entity1, entity2);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/scheduler")
    public void schedulerSwitch() {
        EntityEventScheduler.RUN = !EntityEventScheduler.RUN;
    }

    @GetMapping("/history")
    public ResponseEntity<List<EntityHistoryDto>> getEntityHistory(
            @RequestParam Long entityId
    ) {
        List<EntityHistoryDto> entityHistoryList = historyService.getEntityHistory(entityId);
        return new ResponseEntity<>(entityHistoryList, HttpStatus.OK);
    }

    @PostMapping("/process")
    public void processBatch() {
        if(EntityEventScheduler.ASYNC) entityService.processTickListAsync();
        else entityService.processTickListSync();
    }

    @PostMapping("/async")
    public void processBatchAsync() {
        EntityEventScheduler.ASYNC = !EntityEventScheduler.ASYNC;
    }
}
