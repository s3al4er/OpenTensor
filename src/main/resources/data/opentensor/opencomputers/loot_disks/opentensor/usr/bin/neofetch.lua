-- neofetch: show system info (like neofetch).
-- Part of the OpenTensor floppy disk. Copy to /usr/bin to install:
--   cp -r /mnt/<floppy>/usr/bin/* /usr/bin/
local component = require("component")
local computer = require("computer")
local shell = require("shell")

local logo = {
  "░█████╗░██████╗░███████╗███╗░░██╗████████╗███████╗███╗░░██╗░██████╗░█████╗░██████╗░",
  "██╔══██╗██╔══██╗██╔════╝████╗░██║╚══██╔══╝██╔════╝████╗░██║██╔════╝██╔══██╗██╔══██╗",
  "██║░░██║██████╔╝█████╗░░██╔██╗██║░░░██║░░░█████╗░░██╔██╗██║╚█████╗░██║░░██║██████╔╝",
  "██║░░██║██╔═══╝░██╔══╝░░██║╚████║░░░██║░░░██╔══╝░░██║╚████║░╚═══██╗██║░░██║██╔══██╗",
  "╚█████╔╝██║░░░░░███████╗██║░╚███║░░░██║░░░███████╗██║░╚███║██████╔╝╚█████╔╝██║░░██║",
  "░╚════╝░╚═╝░░░░░╚══════╝╚═╝░░╚══╝░░░╚═╝░░░╚══════╝╚═╝░░╚══╝╚═════╝░░╚════╝░╚═╝░░╚═╝",
}

-- glyph width: unicode.wlen counts cells, #s counts bytes (UTF-8 breaks it)
local wlen = function(s) return #s end
do
  local ok, unicode = pcall(require, "unicode")
  if ok and unicode and unicode.wlen then
    wlen = unicode.wlen
  end
end

-- Tensor products already include the vendor ("NVIDIA RTX PRO 6000 Ada",
-- "AMD EPYC 9354P", "Groq LPU"), stock OC ones don't: prepend only if missing.
local function fullName(info, fallback)
  local product = info.product or fallback or "?"
  local vendor = info.vendor or ""
  if vendor ~= "" and not product:find(vendor, 1, true) then
    return vendor .. " " .. product
  end
  return product
end

local devices = computer.getDeviceInfo()

local cpu = "?"
local ramTotal = 0
local gpus = {}
local fans = 0

for addr, info in pairs(devices) do
  local class = (info.class or ""):lower()
  if class == "processor" or class == "cpu" then
    cpu = fullName(info, "?")
  elseif class == "display" or class == "gpu" then
    local temp = info.temperature
    if temp == nil then
      local okProxy, proxy = pcall(component.proxy, addr)
      if okProxy and proxy.getTemperature then
        local ok, t = pcall(proxy.getTemperature)
        if ok and type(t) == "number" then
          temp = string.format("%.1f", t)
        end
      end
    end
    table.insert(gpus, {
      name = fullName(info, "GPU"),
      vram = info.memory or "?",
      temp = temp and (temp .. "C") or "n/a",
    })
  elseif class == "memory" or class == "ram" then
    local cap = tonumber(info.capacity or "0") or 0
    ramTotal = ramTotal + cap
  elseif class == "generic" and (info.product or ""):lower():find("noctua") then
    fans = fans + 1
  end
end
table.sort(gpus, function(a, b) return a.name < b.name end)

local memTotal = computer.totalMemory()
local memFree = computer.freeMemory()
local uptime = computer.uptime()

local osName = "OpenOS"
do
  local f = io.open("/etc/os-release", "r")
  if f then
    local content = f:read("*a")
    f:close()
    local pretty = content:match('PRETTY_NAME="([^"]+)"')
    if pretty then osName = pretty end
  end
end

local res = "n/a"
for addr in component.list("gpu", true) do
  local okProxy, proxy = pcall(component.proxy, addr)
  if okProxy and proxy.maxResolution then
    local ok, w, h = pcall(proxy.maxResolution)
    if ok and w and h then
      res = string.format("%dx%d", math.floor(w), math.floor(h))
      break
    end
  end
end

local lines = {}
table.insert(lines, "OS: " .. osName)
table.insert(lines, "Host: " .. computer.address():sub(1, 13))
table.insert(lines, string.format("Uptime: %ds", math.floor(uptime)))
table.insert(lines, "CPU: " .. cpu)
table.insert(lines, string.format("Memory: %d / %d", memTotal - memFree, memTotal))
if ramTotal > 0 then
  table.insert(lines, string.format("Devices RAM: %d cells", ramTotal))
end
table.insert(lines, "Display: " .. res)
for _, g in ipairs(gpus) do
  table.insert(lines, string.format("GPU: %s (%s, %s)", g.name, g.vram, g.temp))
end
if fans > 0 then
  table.insert(lines, string.format("Fans: %d x Noctua NF-A14", fans))
end

local logoWidth = 0
for _, l in ipairs(logo) do
  logoWidth = math.max(logoWidth, wlen(l))
end

for i = 1, math.max(#logo, #lines) do
  local l = logo[i] or ""
  local r = lines[i] or ""
  print(l .. string.rep(" ", logoWidth - wlen(l) + 2) .. r)
end
