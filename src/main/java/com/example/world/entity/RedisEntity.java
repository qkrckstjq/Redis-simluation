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
    private boolean skipSearch;
    private boolean skipGeoUpdate;

    public RedisEntity(
            Long id,
            int age,
            TypeEnum type,
            StateEnum state,
            int stamina,
            int hp,
            int x,
            int y,
            boolean breedReady,
            int breedReadyTick,
            String cellKey,
            Long targetId,
            boolean skipSearch
    ) {
        this.id = id;
        this.age = age;
        this.type = type;
        this.state = state;
        this.stamina = stamina;
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.breedReady = breedReady;
        this.breedReadyTick = breedReadyTick;
        this.cellKey = cellKey;
        this.targetId = targetId;
        this.skipSearch = skipSearch;
        this.skipGeoUpdate = false;
    }

    public void decreaseStamina() {
        this.stamina = Math.max(this.stamina - 1, 0);
    }

    public void increaseStamina() {
        this.stamina = Math.min(this.stamina + 1, 100);
    }

    public void decreaseBreedTick() {
        this.breedReadyTick = Math.max(this.breedReadyTick - 1, 0);
    }

    public void increaseHp() { hp = Math.min(100, hp + 1);}

    public void attackedByWolf() {
        hp -= 20;
    }

    public void increaseAge() {
        this.age++;
    }

    public void decreaseAge(int age) { this.age = Math.max(0, this.age - age); }

    public boolean checkCellKey(String cellKey) {
        return this.cellKey.equals(cellKey);
    }


    public boolean isDead() {
        return this.hp < 0 || this.age >= 1000;
    }

    public boolean isBreedReady() {
        if(this.type.equals(TypeEnum.SHEEP)) {
            if(this.age < 400) {
                this.setBreedReady(false);
                return false;
            }

            if(this.hp >= 80 && this.stamina >= 50) {
                this.setBreedReady(true);
                return true;
            }
            this.setBreedReady(false);
            return false;
        }

        if(this.breedReadyTick > 0) {
            this.setBreedReady(true);
            return true;
        }
        this.setBreedReady(false);
        return false;
    }

    public void successHunt() {
        this.setBreedReady(true);
        this.setBreedReadyTick(100);
        this.decreaseAge(1000);
        this.setTargetId(null);
    }

    public void healHp() {
        this.increaseHp();
    }

    public void afterBreed() {
        this.setHp(this.hp - 20);
        this.setStamina(this.stamina - 40);
        this.setBreedReady(false);
        this.setBreedReadyTick(0);
    }

//    public boolean canRest() {
//        if(this.state.equals(StateEnum.REST)) {
//            if(this.type.equals(TypeEnum.WOLF)) {
//                return this.age < 500;
//            }
//
//            if(this.type.equals(TypeEnum.SHEEP)) {
//                return this.stamina < 50;
//            }
//        }
//        return false;
//    }
}
