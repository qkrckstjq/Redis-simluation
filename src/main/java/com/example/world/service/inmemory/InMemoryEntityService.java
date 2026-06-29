package com.example.world.service.inmemory;

import com.example.world.entity.RedisGeo;
import com.example.world.repository.RedisRepository;
import com.example.world.service.EntityService;
import com.example.world.service.RedisService;
import com.example.world.service.batch.BatchProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InMemoryEntityService implements EntityService {
    private final RedisRepository redisRepository;
    private final RedisService redisService;
    private final BatchProcessor batchProcessor;
    private final EntityManager entityManager;

    public InMemoryEntityService(
            RedisRepository redisRepository,
            RedisService inMemoryRedisService,
            BatchProcessor batchProcessor,
            EntityManager entityManager
    ) {
        this.redisRepository = redisRepository;
        this.redisService = inMemoryRedisService;
        this.batchProcessor = batchProcessor;
        this.entityManager = entityManager;
    }

    public void createEntity(String type, String name, int hp, int x, int y) {}

    @Override
    public void createEntityAmount(String type, int count) {
        Long curId = redisRepository.getEntityId();
        redisRepository.requestPipeLine(redisService.saveNewEntities(type, curId + 1, count));
    }

    @Override
    public void processTickListSync() {

    }

    @Override
    public void processTickListAsync() {

    }

    public List<RedisGeo> getNearEntities(Long base, double range) {
        String entityKey = "entity:" + base;
        GeoResults<RedisGeoCommands.GeoLocation<String>> list = redisRepository.getNearEntities(entityKey, range);
        return list.getContent().stream()
                .map(entity -> {
                    RedisGeoCommands.GeoLocation<String> content = entity.getContent();
                    String member = content.getName();
                    Point point = content.getPoint();

                    double x = point.getX();
                    double y = point.getY();
                    return new RedisGeo(x, y, member);
                }).toList();
    }

    public Double getDistBetEntities(Long entity1, Long entity2) {
        String key1 = "entity:" + entity1;
        String key2 = "entity:" + entity2;
        return redisRepository.getDistBetEntities(key1, key2);
    }
}
