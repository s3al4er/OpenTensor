-- opentensor-smi: show all GPUs connected to this machine (like nvidia-smi).
-- Part of the OpenTensor floppy disk. Copy to /usr/bin to install:
--   cp -r /mnt/<floppy>/usr/bin/* /usr/bin/
local component = require("component")
local computer = require("computer")
local shell = require("shell")

local args, options = shell.parse(...)

-- Bump on every script change: lets users verify jar and /usr/bin are in sync.
local SMI_VERSION = "1.0.9-b5"

-- Tensor products already include the vendor ("NVIDIA RTX PRO 6000 Ada"),
-- stock OC ones don't ("Graphics card"): prepend only when missing.
local function gpuName(info)
  local product = info.product or "Unknown GPU"
  local vendor = info.vendor or ""
  if vendor ~= "" and not product:find(vendor, 1, true) then
    return vendor .. " " .. product
  end
  return product
end

local gpus = {}
for addr in component.list("gpu", true) do
  table.insert(gpus, addr)
end
table.sort(gpus)

if #gpus == 0 then
  io.stderr:write("opentensor-smi: no GPUs found\n")
  return 1
end

local devices = computer.getDeviceInfo()
local fans = 0
for _ in component.list("fan", true) do
  fans = fans + 1
end

print(string.format("OpenTensor SMI %s  (%d GPU(s), %d fan(s))", SMI_VERSION, #gpus, fans))
print(string.format("%-3s %-32s %-7s %-9s %-8s %-4s %s",
  "ID", "Name", "VRAM", "Temp", "VRAM use", "Fans", "Address"))

for i, addr in ipairs(gpus) do
  local info = devices[addr] or {}
  local okProxy, proxy = pcall(component.proxy, addr)
  local name = gpuName(info)
  local declared = info.memory or "?"
  local temp = "n/a"
  if okProxy and proxy.getTemperature then
    local ok, t = pcall(proxy.getTemperature)
    if ok and type(t) == "number" then
      temp = string.format("%.1fC", t)
    end
  elseif info.temperature then
    temp = info.temperature .. "C"
  end
  local use = "n/a"
  if okProxy and proxy.totalMemory and proxy.freeMemory then
    local okT, total = pcall(proxy.totalMemory)
    local okF, free = pcall(proxy.freeMemory)
    if okT and okF and type(total) == "number" and total > 0 then
      use = string.format("%d%%", math.floor((total - free) / total * 100 + 0.5))
    end
  end
  -- fans actually seen by THIS gpu (0 here + fans installed = check placement)
  local seen = "n/a"
  if okProxy and proxy.getFans then
    local ok, f = pcall(proxy.getFans)
    if ok and type(f) == "number" then
      seen = tostring(math.floor(f))
    end
  elseif info.fans then
    seen = info.fans
  end
  if options.v then
    print(string.format("%-3d %s", i - 1, addr))
    print(string.format("    Name: %s  VRAM: %s  Temp: %s  VRAM use: %s  Fans: %s",
      name, declared, temp, use, seen))
  else
    print(string.format("%-3d %-32s %-7s %-9s %-8s %-4s %s",
      i - 1, name:sub(1, 32), declared, temp, use, seen, addr:sub(1, 8)))
  end
end
