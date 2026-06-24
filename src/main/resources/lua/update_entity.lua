local entityKey      = KEYS[1]
local prevGeoKey     = KEYS[2]
local nextGeoKey     = KEYS[3]
local worldKey       = KEYS[4]

local isDead         = ARGV[1]
local cellChanged    = ARGV[2]

if isDead == "1" then

    redis.call("DEL", entityKey)

    redis.call(
        "ZREM",
        prevGeoKey,
        entityKey
    )

    redis.call(
        "SREM",
        worldKey,
        entityKey
    )

    return
end

if cellChanged == "1" then

    redis.call(
        "DEL",
        entityKey
    )

    redis.call(
        "ZREM",
        prevGeoKey,
        entityKey
    )

    redis.call(
        "SREM",
        worldKey,
        entityKey
    )
end

local nextEntityKey = ARGV[3]

redis.call(
    "HSET",
    nextEntityKey,

    "id",ARGV[4],
    "age",ARGV[5],
    "type",ARGV[6],
    "state",ARGV[7],
    "stamina",ARGV[8],
    "hp",ARGV[9],
    "x",ARGV[10],
    "y",ARGV[11],
    "breedReady",ARGV[12],
    "breedReadyTick",ARGV[13],
    "cellKey",ARGV[14],
    "targetId",ARGV[15]
)

redis.call(
    "GEOADD",
    nextGeoKey,
    ARGV[16],
    ARGV[17],
    nextEntityKey
)

redis.call(
    "SADD",
    worldKey,
    nextEntityKey
)

return 1