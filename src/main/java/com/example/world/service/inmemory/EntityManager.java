package com.example.world.service.inmemory;

import com.example.world.cluster.CellManager;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.repository.RedisRepository;
import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.world.service.batch.BatchProcessor.BATCH_SIZE;

@Service
@Getter
@Setter
public class EntityManager {
    private List<RedisEntity> entityList = new ArrayList<>();
    private Map<Long, RedisEntity> entityMap = new HashMap<>();
    private List<RedisEntity> spawnEntities = new ArrayList<>();
    private List<RedisEntity> noneTargetEntities = new ArrayList<>();
    private List<RedisEntity> historyEntities = new ArrayList<>();
    private List<Object> geoResults = new ArrayList<>();
    private Map<Long, List<RedisEntity>> nearEntities = new HashMap<>();
    private List<NextMove> nextMoves = new ArrayList<>();
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

    public void initEntityList() {
        entityList = new ArrayList<>();
        entityList.addAll(entityMap.values());
    }

    public void initSpawnEntities() {
        spawnEntities = new ArrayList<>();
    }

    public void addEntity(RedisEntity entity) {
        entityMap.put(entity.getId(), entity);
    }

    public void addAllEntities(List<RedisEntity> entities, Long nextEntityId) {
        for(RedisEntity entity : entities) {
            entity.setId(nextEntityId++);
            entityMap.put(entity.getId(), entity);
        }
    }

    public void removeEntity(RedisEntity entity) {
        entityMap.remove(entity.getId());
    }
}
