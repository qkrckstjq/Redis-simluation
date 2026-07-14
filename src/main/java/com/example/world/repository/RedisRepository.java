package com.example.world.repository;

import com.example.world.constants.RedisKeys;
import com.example.world.entity.RedisEntity;
import com.example.world.entity.RedisGeo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.StreamInfo.*;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
public class RedisRepository {
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_HISTORY = -100;

    public Long nextEntityId() {
        return redisTemplate.opsForValue().increment(RedisKeys.ENTITY_STR);
    }

    public Long allocateIds(int count) {
        Long lastId = redisTemplate.opsForValue()
                .increment(RedisKeys.ENTITY_STR, count);

        return lastId - count + 1;
    }

    public Long getEntityId() {
        String value = redisTemplate.opsForValue().get(RedisKeys.ENTITY_STR);
        return value == null ? 0L : Long.parseLong(value);
    }

    public Set<String> getWorldEntities() {
        long start = System.nanoTime();
        Set<String> result = redisTemplate.opsForSet().members(RedisKeys.WORLD_STR);
        long afterIds = System.nanoTime();
        System.out.printf("ids : %d\n", (afterIds - start) / 1_000_000);
        return result;
    }

    public void saveEntity(RedisEntity entity) {
        String key = "entity:" + entity.getId();

        Map<String, String> values = Map.of(
                "id", String.valueOf(entity.getId()),
                "type", entity.getType().name(),
                "state", entity.getState().getState(),
                "x", String.valueOf(entity.getX()),
                "y", String.valueOf(entity.getY()),
                "hp", String.valueOf(entity.getHp())
        );

        redisTemplate.opsForHash().putAll(key, values);
    }

    public void saveGeoEntity(RedisEntity entity) {
        String id = "entity:" + String.valueOf(entity.getId());
        int x = entity.getX();
        int y = entity.getY();
        Point point = new Point(x, y);
        redisTemplate.opsForGeo().add(RedisKeys.GEO_STR, point, id);
    }

    public RedisGeo getGeoEntity(String key) {
        List<Point> points = redisTemplate.opsForGeo().position(RedisKeys.GEO_STR, key);
        Point point = points.getFirst();
        return new RedisGeo(point.getX(), point.getY(), key);
    }


//    public RedisEntity getEntity(Long entityId) {
//        String key = "entity:" + entityId;
//
//        Map<Object, Object> values =
//                redisTemplate.opsForHash().entries(key);
//
//        if (values.isEmpty()) {
//            return null;
//        }
//
//        return new RedisEntity(
//                Long.parseLong((String) values.get("id")),
//                String.
//                StateEnum.MOVE,
//                Integer.parseInt((String) values.get("hp")),
//                Integer.parseInt((String) values.get("x")),
//                Integer.parseInt((String) values.get("y"))
//        );
//    }

    public void updateEntity(RedisEntity entity) {
        String key = "entity:" + entity.getId();

        Map<String, String> values = new HashMap<>();
        values.put("x", String.valueOf(entity.getX()));
        values.put("y", String.valueOf(entity.getY()));

        redisTemplate.opsForHash().putAll(key, values);
    }

    public void addWorldEntity(Long entityId) {
        redisTemplate.opsForSet().add(RedisKeys.WORLD_STR, String.valueOf(entityId));
    }

    public Set<String> keys(String key) {
        return redisTemplate.keys(key);
    }

    public List<Object> responsePipeLine(Consumer<RedisConnection> consumer) {
        List<Object> results = redisTemplate.executePipelined(((RedisCallback<Object>) connection -> {
            consumer.accept(connection);
            return null;
        }));
        return results;
    }

    public void requestPipeLine(Consumer<RedisConnection> consumer) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            consumer.accept(connection);
            return null;
        });
    }

    public Cursor<byte[]> scanWorldEntities(int count) {

        ScanOptions options = ScanOptions.scanOptions()
                .count(count)
                .build();

        return redisTemplate.execute(
                (RedisCallback<Cursor<byte[]>>) connection ->
                        connection.setCommands().sScan(
                                RedisKeys.WORLD_BYTE,
                                options
                        )
        );
    }

    public <T> T scanWithCursor(
            Supplier<Cursor<byte[]>> cursorSupplier,
            Function<Cursor<byte[]>, T> function
    ) {
        try (Cursor<byte[]> cursor = cursorSupplier.get()) {
            return function.apply(cursor);
        }
    }

    public void scanWithCursor(
            Supplier<Cursor<byte[]>> cursorSupplier,
            Consumer<Cursor<byte[]>> consumer
    ) {
        try (Cursor<byte[]> cursor = cursorSupplier.get()) {
            consumer.accept(cursor);
        }
    }

    public List<String> getAllEntityIds(int count) {

        try (Cursor<byte[]> cursor = scanWorldEntities(count)) {

            List<String> ids = new ArrayList<>();

            while (cursor.hasNext()) {
                ids.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }

            return ids;
        }
    }

    public GeoResults<GeoLocation<String>> getNearEntities(String entityKey, double range) {
        return redisTemplate.opsForGeo().search(
                RedisKeys.GEO_STR,
                GeoReference.fromMember(entityKey),
                new Distance(range, Metrics.METERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeCoordinates()
        );
    }

    public Double getDistBetEntities(String key1, String key2) {
        Distance distance = redisTemplate.opsForGeo().distance(RedisKeys.GEO_STR, key1, key2, Metrics.METERS);
        return distance.getValue();
    }

    public void lPush(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public void listTrim(String key) {
        redisTemplate.opsForList().trim(key, MAX_HISTORY, -1);
    }

    public List<String> lRange(String key, int start, int end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public String getTick() {
        return redisTemplate.opsForValue().get(RedisKeys.TICK);
    }

    public void setTick(String tick) {
        redisTemplate.opsForValue().set(RedisKeys.TICK, tick);
    }

    public Long getStreamSize(String key) {
        return redisTemplate.opsForStream().size(key);
    }

    public PendingMessagesSummary getPending(String key, String consumerGroup) {
        return redisTemplate.opsForStream().pending(key, consumerGroup);
    }

    public XInfoGroups getXInfo(String key) {
        return redisTemplate.opsForStream().groups(key);
    }
}
