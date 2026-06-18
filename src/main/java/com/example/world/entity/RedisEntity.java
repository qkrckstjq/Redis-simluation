package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RedisEntity {
    private Long id;
    private int age;
    private TypeEnum type;
    private StateEnum state;
    private int stamina;
    private int hp;
    private int x;
    private int y;
    private Long targetId;
    private boolean breedReady;
    private int breedReadyTick;
    private String cellKey;

    public RedisEntity(Long id, int age, TypeEnum type, StateEnum state, int stamina, int hp, int x, int y, String cellKey) {
        this.id = id;
        this.age = age;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.breedReady = false;
        this.breedReadyTick = 0;
        this.cellKey = cellKey;
    }

    public void decreaseStamina() {
        this.stamina = Math.max(this.stamina - 1, 0);
    }

    public void increaseStamina() {
        this.stamina = Math.min(this.stamina + 1, 100);
    }

    public void decreaseBreedTick() {
        this.breedReadyTick--;
    }

    public void increaseHp() { hp = Math.min(100, hp + 1);}

    public void attackedByWolf() {
        hp -= 20;
    }

    public void increaseAge() {
        this.age++;
    }

    public boolean checkCellKey(String cellKey) {
        return this.cellKey.equals(cellKey);
    }
}
