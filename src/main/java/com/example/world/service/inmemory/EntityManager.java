package com.example.world.service.inmemory;

import com.example.world.cluster.CellManager;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.world.service.batch.BatchProcessor.BATCH_SIZE;

@Service
public class EntityManager {
    private List<RedisEntity> entityList = new ArrayList<>();
    private Map<Long, RedisEntity> entityMap = new HashMap<>();
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final EntityMapper entityMapper;
    private final CellManager cellManager;

    public EntityManager(
            RedisRepository redisRepository,
            RedisService entityClusterService,
            EntityMapper entityClusterMapper,
            CellManager cellManager
    ) {
        this.redisRepository = redisRepository;
        this.redisService = entityClusterService;
        this.entityMapper = entityClusterMapper;
        this.cellManager = cellManager;
    }

    @PostConstruct
    public void init() {
        List<String> ids = redisRepository.getAllEntityIds(BATCH_SIZE);
        List<Object> results = redisRepository.responsePipeLine(
                redisService.getEntityIds(ids)
        );
        entityList = EntityMapper.objectsToRedisEntities(results);

        for(RedisEntity entity : entityList) {
            Long id = entity.getId();
            entityMap.put(id, entity);
        }
    }

    public void addEntity(RedisEntity entity) {
        entityList.add(entity);
        entityMap.put(entity.getId(), entity);
    }
}
