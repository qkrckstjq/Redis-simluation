local entityKey = KEYS[1]
local geoKey = KEYS[2]
local worldKey = KEYS[3]
local entityCounter = KEYS[4]

local id = ARGV[1]
local hp = ARGV[2]
local type = ARGV[3]
local state = ARGV[4]
local stamina = ARGV[5]
local x = ARGV[6]
local y = ARGV[7]
local cellKey = ARGV[8]

local geoX = ARGV[9]
local geoY = ARGV[10]

redis.call(
    "INCR",
    entityCounter
)

redis.call(
    "HMSET",
    entityKey,

    "id", id,
    "hp", hp,
    "type", type,
    "state", state,
    "stamina", stamina,
    "x", x,
    "y", y,
    "cellKey", cellKey
)

redis.call(
    "SADD",
    worldKey,
    entityKey
)

redis.call(
    "GEOADD",
    geoKey,
    geoX,
    geoY,
    entityKey
)

return 1