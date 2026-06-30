package com.example.world.service;

import com.example.world.entity.RedisGeo;

import java.util.List;

public interface EntityService {
    public void createEntity(String type, String name, int hp, int x, int y);
    public void createEntityAmount(String type, int count);
    public void processTickListSync();
    public void processTickListAsync();
    public List<RedisGeo> getNearEntities(Long base, double range);
    public Double getDistBetEntities(Long entity1, Long entity2);
}
