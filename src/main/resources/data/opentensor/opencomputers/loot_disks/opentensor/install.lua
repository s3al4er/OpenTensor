-- OpenTensor floppy installer: copies tools to /usr/bin and examples to /usr/share.
-- Run this from the floppy with its FULL path, e.g.: /mnt/<address>/install.lua
-- (bare `install` is the OpenOS installer, NOT this one.)
-- Or copy manually: cp -r /mnt/<address>/usr/bin/* /usr/bin/
--                   cp -r /mnt/<address>/usr/share/* /usr/share/
local INSTALLER_VERSION = "1.0.9-b6"

local filesystem = require("filesystem")

-- Marker present only in up-to-date scripts (old disks/jars lack it).
local function isFresh(root)
  local f = io.open(filesystem.concat(root, "usr/bin/asm-flash.lua"), "r")
  if not f then
    return false
  end
  local content = f:read("*a")
  f:close()
  return content and content:find("getBootAddress", 1, true) ~= nil
end

local function hasTools(root)
  return filesystem.exists(filesystem.concat(root, "usr/bin/opentensor-smi.lua"))
end

local function findSource()
  local stale = {}
  if filesystem.exists("/mnt") then
    for entry in filesystem.list("/mnt") do
      local mount = filesystem.concat("/mnt", entry)
      if filesystem.isDirectory(mount) and hasTools(mount) then
        if isFresh(mount) then
          return mount, nil
        end
        stale[#stale + 1] = mount
      end
    end
  end
  return nil, stale
end

local function copyTree(src, dst)
  if not filesystem.exists(src) then
    return false, "missing " .. src
  end
  if filesystem.isDirectory(src) then
    filesystem.makeDirectory(dst)
    for entry in filesystem.list(src) do
      local ok, err = copyTree(filesystem.concat(src, entry), filesystem.concat(dst, entry))
      if not ok then
        return false, err
      end
    end
    return true
  end
  if filesystem.exists(dst) then
    filesystem.remove(dst)
  end
  return filesystem.copy(src, dst)
end

print("OpenTensor installer " .. INSTALLER_VERSION)
local srcDir, stale = findSource()
if not srcDir then
  io.stderr:write("opentensor install FAILED: no up-to-date OpenTensor floppy found.\n")
  if stale and #stale > 0 then
    io.stderr:write("Disk(s) with OUTDATED scripts (refusing to install):\n")
    for _, m in ipairs(stale) do
      io.stderr:write("  " .. m .. "\n")
    end
  end
  io.stderr:write("Make sure: (1) the opentensor jar is updated, (2) the world was\n")
  io.stderr:write("restarted after the update, (3) this installer runs as\n")
  io.stderr:write("/mnt/<address>/install.lua from a fresh disk (bare `install`\n")
  io.stderr:write("is the OpenOS installer, not this one).\n")
  return 1
end
print("Source: " .. srcDir)

local ok, err = copyTree(filesystem.concat(srcDir, "usr/bin"), "/usr/bin")
if not ok then
  io.stderr:write("opentensor install failed: " .. tostring(err) .. "\n")
  return 1
end
if filesystem.exists(filesystem.concat(srcDir, "usr/share")) then
  local okS, errS = copyTree(filesystem.concat(srcDir, "usr/share"), "/usr/share")
  if not okS then
    io.stderr:write("opentensor install failed: " .. tostring(errS) .. "\n")
    return 1
  end
end
print("OpenTensor tools installed: opentensor-smi, neofetch, asm-flash (+ examples in /usr/share/asm)")
print("Verify: grep -c getBootAddress /usr/bin/asm-flash.lua  (expect a number above 0)")
