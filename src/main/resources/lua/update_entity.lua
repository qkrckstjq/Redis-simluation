local entityKey = KEYS[1]
local geoKey = KEYS[2]
local worldKey = KEYS[3]

local hp = ARGV[1]
local x = ARGV[2]
local y = ARGV[3]
local state = ARGV[4]
local stamina = ARGV[5]

local geoX = ARGV[6]
local geoY = ARGV[7]

local entityId = ARGV[8]

redis.call(
    "HMSET",
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
    geoX,
    geoY,
    entityId
)

return 1