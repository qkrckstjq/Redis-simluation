package com.example.world.entity;

import lombok.Getter;

@Getter
public enum TypeEnum {
    WOLF("WOLF"),
    SHEEP("SHEEP");

    private final String type;

    TypeEnum(String type) {
        this.type = type;
    }
}
