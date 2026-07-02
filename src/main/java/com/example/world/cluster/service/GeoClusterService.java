package com.example.world.cluster.service;

import com.example.world.cluster.CellManager;
import com.example.world.entity.NextMove;
import com.example.world.entity.RedisEntity;
import com.example.world.util.ByteTypeConverter;
import com.example.world.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

import static com.example.world.cluster.CellManager.CELL_SIZE;

@Service
@RequiredArgsConstructor
public class GeoClusterService {
    private final CellManager cellManager;
//    private final GeoUtil geoUtil;

    public Consumer<RedisConnection> getNearByIds(List<RedisEntity> entities, int range) {
        return connection -> {

            for (RedisEntity entity : entities) {

                int minCellX = (entity.getX() - range) / CELL_SIZE;
                int maxCellX = (entity.getX() + range) / CELL_SIZE;

                int minCellY = (entity.getY() - range) / CELL_SIZE;
                int maxCellY = (entity.getY() + range) / CELL_SIZE;

                int cellCountX = maxCellX - minCellX + 1;
                int cellCountY = maxCellY - minCellY + 1;
                int limit = 6 / (cellCountX * cellCountY);

                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {

                    for (int cellY = minCellY; cellY <= maxCellY; cellY++) {

                        String geoKey = cellManager.getGeoKeyByCell(cellX, cellY);
                        connection.geoCommands().geoSearch(
                                ByteTypeConverter.stringToByte(geoKey),
                                GeoReference.fromCoordinate(
                                        GeoUtil.scaleIn(entity.getX()),
                                        GeoUtil.scaleIn(entity.getY())
                                ),
                                GeoShape.byRadius(new Distance(range)),
                                RedisGeoCommands.GeoSearchCommandArgs
                                        .newGeoSearchArgs()
                                        .sortAscending()
                                        .limit(limit)
                        );
                    }
                }
            }
        };
    }

    public Consumer<RedisConnection> getCollisionIds(
            List<NextMove> nextMoveList,
            double range
    ) {
        return connection -> {

            for (NextMove nextMove : nextMoveList) {

                int minCellX = (int) ((nextMove.getNextX() - range) / CELL_SIZE);
                int maxCellX = (int) ((nextMove.getNextX() + range) / CELL_SIZE);

                int minCellY = (int) ((nextMove.getNextY() - range) / CELL_SIZE);
                int maxCellY = (int) ((nextMove.getNextY() + range) / CELL_SIZE);

                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {

                    for (int cellY = minCellY; cellY <= maxCellY; cellY++) {

                        String geoKey = cellManager.getGeoKeyByCell(cellX, cellY);
                        connection.geoCommands().geoSearch(
                                ByteTypeConverter.stringToByte(geoKey),
                                GeoReference.fromCoordinate(
                                        GeoUtil.scaleIn(nextMove.getNextX()),
                                        GeoUtil.scaleIn(nextMove.getNextY())
                                ),
                                GeoShape.byRadius(new Distance(range)),
                                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        );
                    }
                }
            }
        };
    }
}
