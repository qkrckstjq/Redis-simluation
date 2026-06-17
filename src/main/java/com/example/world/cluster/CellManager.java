package com.example.world.cluster;

import com.example.world.cluster.entity.Cell;
import com.example.world.constants.RedisKeys;
import com.example.world.entity.RedisGeo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class CellManager {
    public static final int CELL_SIZE = 64;

    public Cell getCell(int x, int y) {
        return new Cell(x / CELL_SIZE, y / CELL_SIZE);
    }

    private boolean useCell = true;

    public String getGeoKey(int x, int y) {

        if(!useCell) return RedisKeys.GEO_STR;

        Cell cell = getCell(x, y);

        return "geo:%d:%d".formatted(cell.x(), cell.y());
    }

    public String getGeoKeyByCell(int cellX, int cellY) {
        return "geo:%d:%d".formatted(cellX, cellY);
    }
}
