local entityKey = KEYS[1]
local geoKey = KEYS[2]
local worldKey = KEYS[3]

local hp = tonumber(ARGV[1])
local x = ARGV[2]
local y = ARGV[3]
local state = ARGV[4]
local stamina = ARGV[5]
local longitude = ARGV[6]
local latitude = ARGV[7]
local entityId = ARGV[8]

if hp <= 0 then
    redis.call("DEL", entityKey)
    redis.call("ZREM", geoKey, entityKey)
    redis.call("SREM", worldKey, entityId)
    return 0
end

redis.call(
    "HSET",
    entityKey,
    "hp", hp,
    "x", x,
    "y", y,
    "state", state,
    "stamina", stamina
)

redis.call(
    "GEOADD",
    geoKey,
    longitude,
    latitude,
    entityKey
)

return 1