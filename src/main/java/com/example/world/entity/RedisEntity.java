package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RedisEntity {
    Long id;
    TypeEnum type;
    StateEnum state;
    int stamina;
    int hp;
    int x;
    int y;
    Long targetId;
    private String cellKey;

    public RedisEntity(Long id, TypeEnum type, StateEnum state, int stamina, int hp, int x, int y, String cellKey) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.cellKey = cellKey;
    }

    public void decreaseStamina() {
        stamina = Math.max(stamina - 1, 0);
    }

    public void increaseStamina() {
        stamina = Math.min(stamina + 1, 100);
    }

    public void increaseHp() { hp = Math.min(100, hp + 1);}

    public void attackedByWolf() {
        hp -= 20;
    }

    public boolean checkCellKey(String cellKey) {
        return this.cellKey.equals(cellKey);
    }
}
