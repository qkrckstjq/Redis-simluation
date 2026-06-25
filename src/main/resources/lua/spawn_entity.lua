local entityKey = KEYS[1]
local geoKey = KEYS[2]
local worldKey = KEYS[3]

redis.call(
    "HSET",
    entityKey,

    "id",ARGV[1],
    "age",ARGV[2],
    "type",ARGV[3],
    "state",ARGV[4],
    "stamina",ARGV[5],
    "hp",ARGV[6],
    "x",ARGV[7],
    "y",ARGV[8],
    "breedReady",ARGV[9],
    "breedReadyTick",ARGV[10],
    "cellKey",ARGV[11],
    "targetId",ARGV[12]
)

redis.call(
    "SADD",
    worldKey,
    entityKey
)

redis.call(
    "GEOADD",
    geoKey,
    ARGV[13],
    ARGV[14],
    entityKey
)

return 1