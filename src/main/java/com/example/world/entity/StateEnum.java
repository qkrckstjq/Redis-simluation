package com.example.world.entity;

import lombok.Getter;

@Getter
public enum StateEnum {
    IDLE("IDLE"),
    MOVE("MOVE"),
    CHASE("CHASE"),
    ATTACK("ATTACK"),
    RUN("RUN"),
    REST("REST"),
    FLOCK("FLOCK"),
    SPAWN("SPAWN");

    private final String state;

    StateEnum(String state) {
        this.state = state;
    }
}
