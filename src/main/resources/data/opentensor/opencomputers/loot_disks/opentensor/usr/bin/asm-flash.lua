-- asm-flash: assemble .asm source to EEPROM bytecode and flash it.
-- Works like the stock `flash`, but takes assembly source instead of Lua:
--   asm-flash [-q] <file.asm> [label]
-- Without a label the EEPROM is labeled 'EEPROM (ASM BIOS)'.
-- Part of the OpenTensor floppy disk. Copy to /usr/bin to install:
--   cp -r /mnt/<floppy>/usr/bin/* /usr/bin/
--
-- Mini-ISA: registers R0-R7, labels `name:`, `;` comments.
--   MOV Rd, src     Rd = src (register, number or "string")
--   ADD/SUB/MUL/DIV Rd, Rs   Rd = Rd op Rs (Rs: register or number)
--   CMP Ra, Rb      sets Z (equal) and G (greater, numbers)
--   JMP/JZ/JNZ/JGT/JLT L
--   PUSH src / POP Rd / CALL L / RET
--   PRINT src       print register, number, "string" or DB label via GPU
--   BEEP freq, dur  computer.beep
--   SLEEP n         seconds, via computer.uptime (yields, deadline-safe)
--   HALT            stop the program; the machine stays ON with the
--                   screen output kept (yield parking, no OS boot)
--   [label:] DB v, ...   data (numbers or "strings"), read via label
-- NOTE: EEPROM code runs before OpenOS loads: no io/os/keyboard input.
-- A dead BIOS coroutine halts the whole machine (stock behavior), so HALT
-- parks on coroutine.yield() instead of returning. ASM BIOS images are
-- standalone by design: no OpenOS chain-load, no getBootAddress shims.
local component = require("component")
local shell = require("shell")

local DEFAULT_LABEL = "EEPROM (ASM BIOS)"

local args, options = shell.parse(...)

if #args < 1 then
  io.write("Usage: asm-flash [-q] <file.asm> [label]\n")
  io.write(" q: quiet mode, don't ask questions.\n")
  io.write("Assembles the .asm source to EEPROM bytecode and flashes it.\n")
  io.write("Without a label the EEPROM is labeled '" .. DEFAULT_LABEL .. "'.\n")
  return
end

-- ------------------------------------------------------------------ --
-- Parser / compiler.
-- ------------------------------------------------------------------ --

local OPC = {
  MOV = 1, ADD = 2, SUB = 3, MUL = 4, DIV = 5, CMP = 6,
  JMP = 7, JZ = 8, JNZ = 9, JGT = 10, JLT = 11,
  PRINT = 12, SLEEP = 13, HALT = 14,
  PUSH = 15, POP = 16, CALL = 17, RET = 18, BEEP = 19,
}

local ARITY = {
  MOV = 2, ADD = 2, SUB = 2, MUL = 2, DIV = 2, CMP = 2,
  JMP = 1, JZ = 1, JNZ = 1, JGT = 1, JLT = 1,
  PRINT = 1, SLEEP = 1, HALT = 0,
  PUSH = 1, POP = 1, CALL = 1, RET = 0, BEEP = 2,
}

local JUMPS = {JMP = true, JZ = true, JNZ = true, JGT = true, JLT = true, CALL = true}

local function trim(s)
  return s:match("^%s*(.-)%s*$")
end

local function stripComment(line)
  local out, inStr, esc = {}, false, false
  for i = 1, #line do
    local c = line:sub(i, i)
    if inStr then
      out[#out + 1] = c
      if esc then
        esc = false
      elseif c == "\\" then
        esc = true
      elseif c == '"' then
        inStr = false
      end
    else
      if c == '"' then
        inStr = true
        out[#out + 1] = c
      elseif c == ";" then
        break
      else
        out[#out + 1] = c
      end
    end
  end
  return table.concat(out)
end

local function splitList(s)
  local ops, cur, inStr, esc = {}, {}, false, false
  s = s .. ","
  for i = 1, #s do
    local c = s:sub(i, i)
    if inStr then
      cur[#cur + 1] = c
      if esc then
        esc = false
      elseif c == "\\" then
        esc = true
      elseif c == '"' then
        inStr = false
      end
    else
      if c == '"' then
        inStr = true
        cur[#cur + 1] = c
      elseif c == "," then
        ops[#ops + 1] = trim(table.concat(cur))
        cur = {}
      else
        cur[#cur + 1] = c
      end
    end
  end
  while #ops > 0 and ops[#ops] == "" do
    ops[#ops] = nil
  end
  return ops
end

local function parseStringLit(s)
  if #s < 2 or s:sub(1, 1) ~= '"' or s:sub(-1) ~= '"' then
    return nil
  end
  local body = s:sub(2, -2)
  body = body:gsub("\\(.)", function(e)
    if e == "n" then
      return "\n"
    elseif e == "t" then
      return "\t"
    elseif e == '"' then
      return '"'
    elseif e == "\\" then
      return "\\"
    else
      return e
    end
  end)
  return body
end

local function classifyOperand(text, lineNo)
  local reg = text:upper():match("^R([0-7])$")
  if reg then
    return {t = "r", v = tonumber(reg)}
  end
  local num = tonumber(text)
  if num then
    return {t = "n", v = num}
  end
  local str = parseStringLit(text)
  if str then
    return {t = "s", v = str}
  end
  if text:match("^[A-Za-z_][%w_]*$") then
    return {t = "sym", v = text}
  end
  error("line " .. lineNo .. ": bad operand '" .. text .. "'")
end

local function emitOperand(op)
  if op.t == "r" then
    return '{t="r",v=' .. op.v .. "}"
  elseif op.t == "n" then
    return '{t="n",v=' .. tostring(op.v) .. "}"
  elseif op.t == "s" then
    return '{t="s",v=' .. string.format("%q", op.v) .. "}"
  elseif op.t == "c" then
    return '{t="n",v=' .. op.v .. "}"
  else
    return '{t="d",v=' .. op.v .. "}"
  end
end

local function assembleFile(path)
  local file = assert(io.open(path, "rb"))
  local src = file:read("*a")
  file:close()

  -- Pass 1: labels, code and data.
  local codeLabels, dbLabels = {}, {}
  local code, db = {}, {}
  local pending = {}

  local function claimLabel(name, lineNo)
    if codeLabels[name] or dbLabels[name] then
      error("line " .. lineNo .. ": duplicate label '" .. name .. "'")
    end
    for _, pl in ipairs(pending) do
      if pl == name then
        error("line " .. lineNo .. ": duplicate label '" .. name .. "'")
      end
    end
    return name
  end

  local function flushPending(lineNo, isDb, index)
    for _, pl in ipairs(pending) do
      if isDb then
        dbLabels[pl] = index
      else
        codeLabels[pl] = index
      end
    end
    pending = {}
  end

  local lineNo = 0
  for raw in (src .. "\n"):gmatch("([^\n]*)\n") do
    lineNo = lineNo + 1
    local line = trim(stripComment(raw))
    if line ~= "" then
      local label, rest = line:match("^([A-Za-z_][%w_]*)%s*:%s*(.*)$")
      if label then
        claimLabel(label, lineNo)
        pending[#pending + 1] = label
        line = trim(rest)
      end
      if line ~= "" then
        local head, tail = line:match("^(%a+)%s*(.*)$")
        if not head then
          error("line " .. lineNo .. ": cannot parse '" .. raw .. "'")
        end
        if head:upper() == "DB" then
          local vals = splitList(tail)
          if #vals == 0 then
            error("line " .. lineNo .. ": DB needs at least one value")
          end
          for i, vtext in ipairs(vals) do
            local num = tonumber(vtext)
            local value = num
            if value == nil then
              value = parseStringLit(vtext)
            end
            if value == nil then
              error("line " .. lineNo .. ": DB value must be a number or \"string\"")
            end
            db[#db + 1] = value
            if i == 1 and #pending > 0 then
              flushPending(lineNo, true, #db)
            end
          end
        else
          local mnem = head:upper()
          if not OPC[mnem] then
            error("line " .. lineNo .. ": unknown mnemonic '" .. head .. "'")
          end
          local operands = {}
          if tail ~= "" then
            operands = splitList(tail)
          end
          if #operands ~= ARITY[mnem] then
            error("line " .. lineNo .. ": '" .. mnem .. "' wants " ..
              ARITY[mnem] .. " operand(s), got " .. #operands)
          end
          local entry = {op = mnem, ops = {}, line = lineNo}
          for _, otext in ipairs(operands) do
            entry.ops[#entry.ops + 1] = classifyOperand(otext, lineNo)
          end
          if mnem == "MOV" or mnem == "ADD" or mnem == "SUB" or
             mnem == "MUL" or mnem == "DIV" or mnem == "POP" then
            if entry.ops[1].t ~= "r" then
              error("line " .. lineNo .. ": '" .. mnem .. "' destination must be a register")
            end
          end
          if #pending > 0 then
            flushPending(lineNo, false, #code + 1)
          end
          code[#code + 1] = entry
        end
      end
    end
  end
  if #pending > 0 then
    -- Trailing labels: point past the end (falls through to halt).
    flushPending(lineNo, false, #code + 1)
  end
  if #code == 0 then
    error("nothing to assemble")
  end

  -- Pass 2: resolve symbols and emit.
  local out = {}
  out[#out + 1] = "-- EEPROM image assembled by asm-flash from " .. path
  out[#out + 1] = "local R={[0]=0,0,0,0,0,0,0,0}"
  out[#out + 1] = "local D={"
  for i, v in ipairs(db) do
    if type(v) == "number" then
      out[#out + 1] = "[" .. i .. "]=" .. tostring(v) .. ","
    else
      out[#out + 1] = "[" .. i .. "]=" .. string.format("%q", v) .. ","
    end
  end
  out[#out + 1] = "}"
  out[#out + 1] = "local P={"
  local pc = 0
  for _, entry in ipairs(code) do
    pc = pc + 1
    local parts = {"{" .. OPC[entry.op]}
    for _, op in ipairs(entry.ops) do
      local rop = op
      if op.t == "sym" then
        if codeLabels[op.v] then
          rop = {t = "n", v = codeLabels[op.v]}
        elseif dbLabels[op.v] then
          if JUMPS[entry.op] then
            error("line " .. entry.line .. ": '" .. op.v .. "' is data, not code")
          end
          rop = {t = "d", v = dbLabels[op.v]}
        else
          error("line " .. entry.line .. ": unknown label '" .. op.v .. "'")
        end
      end
      parts[#parts + 1] = emitOperand(rop)
    end
    parts[#parts + 1] = "}"
    out[#out + 1] = "[" .. pc .. "]=" .. table.concat(parts, ",") .. ","
  end
  out[#out + 1] = "}"
  out[#out + 1] = "local Z,G=false,false"
  out[#out + 1] = "local S,CS={},{}"
  out[#out + 1] = "local __g,__s"
  out[#out + 1] = 'for __a in component.list("gpu") do __g=__a break end'
  out[#out + 1] = 'for __a in component.list("screen") do __s=__a break end'
  out[#out + 1] = 'if __g and __s then pcall(component.invoke,__g,"bind",__s) end'
  out[#out + 1] = "local __x,__y=1,1"
  out[#out + 1] = "local function __P(v)"
  out[#out + 1] = "v=tostring(v)"
  out[#out + 1] = "if not (__g and __s) then return end"
  out[#out + 1] = 'local ok,w,h=pcall(component.invoke,__g,"getResolution")'
  out[#out + 1] = 'if not ok or type(w)~="number" then w,h=80,25 end'
  out[#out + 1] = "local function __scroll()"
  out[#out + 1] = "if __y>h then"
  out[#out + 1] = 'pcall(component.invoke,__g,"copy",1,2,w,h-1,0,-1)'
  out[#out + 1] = 'pcall(component.invoke,__g,"fill",1,h,w,1," ")'
  out[#out + 1] = "__y=h end end"
  out[#out + 1] = "local i=1"
  out[#out + 1] = "while i<=#v do"
  out[#out + 1] = 'local nl=v:find("\\n",i,true)'
  out[#out + 1] = "local chunk"
  out[#out + 1] = "if nl then chunk=v:sub(i,nl-1) i=nl+1 else chunk=v:sub(i) i=#v+1 end"
  out[#out + 1] = "while #chunk>0 do"
  out[#out + 1] = "local part=chunk:sub(1,w-__x+1)"
  out[#out + 1] = 'pcall(component.invoke,__g,"set",__x,__y,part)'
  out[#out + 1] = "__x=__x+#part chunk=chunk:sub(#part+1)"
  out[#out + 1] = "if __x>w then __x=1 __y=__y+1 __scroll() end"
  out[#out + 1] = "end"
  out[#out + 1] = "if nl then __x=1 __y=__y+1 __scroll() end"
  out[#out + 1] = "end end"
  out[#out + 1] = "local __hasUp=false"
  out[#out + 1] = "do local ok,t=pcall(computer.uptime) if ok and type(t)==\"number\" then __hasUp=true end end"
  out[#out + 1] = "local function __V(o)"
  out[#out + 1] = 'local t=o.t if t=="r" then return R[o.v]'
  out[#out + 1] = 'elseif t=="n" then return o.v'
  out[#out + 1] = 'elseif t=="s" then return o.v'
  out[#out + 1] = "else return D[o.v] end end"
  out[#out + 1] = "local __pc=1"
  out[#out + 1] = "local function __halt()"
  out[#out + 1] = "while true do coroutine.yield() end end"
  out[#out + 1] = "-- standalone ASM BIOS image: no OS chain-load by design"
  out[#out + 1] = "while true do"
  out[#out + 1] = "local __i=P[__pc]"
  out[#out + 1] = "if not __i then __halt() end"
  out[#out + 1] = "local __n=__pc+1"
  out[#out + 1] = "local __op=__i[1]"
  out[#out + 1] = "if __op==14 then __halt()"
  out[#out + 1] = "elseif __op==1 then R[__i[2].v]=__V(__i[3])"
  out[#out + 1] = "elseif __op==2 then R[__i[2].v]=R[__i[2].v]+__V(__i[3])"
  out[#out + 1] = "elseif __op==3 then R[__i[2].v]=R[__i[2].v]-__V(__i[3])"
  out[#out + 1] = "elseif __op==4 then R[__i[2].v]=R[__i[2].v]*__V(__i[3])"
  out[#out + 1] = "elseif __op==5 then R[__i[2].v]=R[__i[2].v]/__V(__i[3])"
  out[#out + 1] = "elseif __op==6 then local a=__V(__i[2]) local b=__V(__i[3]) Z=(a==b) G=(type(a)==\"number\" and type(b)==\"number\" and a>b)"
  out[#out + 1] = "elseif __op==7 then __n=__i[2].v"
  out[#out + 1] = "elseif __op==8 then if Z then __n=__i[2].v end"
  out[#out + 1] = "elseif __op==9 then if not Z then __n=__i[2].v end"
  out[#out + 1] = "elseif __op==10 then if G then __n=__i[2].v end"
  out[#out + 1] = "elseif __op==11 then if (not G) and (not Z) then __n=__i[2].v end"
  out[#out + 1] = "elseif __op==12 then __P(__V(__i[2]))"
  out[#out + 1] = "elseif __op==13 then local d=__V(__i[2]) if type(d)==\"number\" and __hasUp then local t0=computer.uptime() while computer.uptime()-t0<d do coroutine.yield() end end"
  out[#out + 1] = "elseif __op==15 then S[#S+1]=__V(__i[2])"
  out[#out + 1] = "elseif __op==16 then R[__i[2].v]=S[#S] S[#S]=nil"
  out[#out + 1] = "elseif __op==17 then CS[#CS+1]=__n __n=__i[2].v"
  out[#out + 1] = "elseif __op==18 then __n=CS[#CS] or (#P+1) CS[#CS]=nil"
  out[#out + 1] = "elseif __op==19 then pcall(computer.beep,__V(__i[2]),__V(__i[3]))"
  out[#out + 1] = "end"
  out[#out + 1] = "__pc=__n"
  out[#out + 1] = "end"
  return table.concat(out, "\n")
end

-- ------------------------------------------------------------------ --
-- Flash flow (mirrors stock flash).
-- ------------------------------------------------------------------ --

if not component.isAvailable("eeprom") then
  io.stderr:write("asm-flash: no EEPROM installed\n")
  return 1
end

local ok, image = pcall(assembleFile, args[1])
if not ok then
  io.stderr:write("asm-flash: " .. tostring(image) .. "\n")
  return 1
end

if not options.q then
  io.write("Insert the EEPROM you would like to flash.\n")
  io.write("When ready to write, type `y` to confirm.\n")
  repeat
    local response = io.read()
  until response and response:lower():sub(1, 1) == "y"
  io.write("Beginning to assemble and flash EEPROM.\n")
end

local eeprom = component.eeprom

if not options.q then
  io.write("Flashing EEPROM " .. eeprom.address .. ".\n")
  io.write("Please do NOT power down or restart your computer during this operation!\n")
end

local maxSize = eeprom.getSize()
if #image > maxSize then
  io.stderr:write("asm-flash: assembled image too large (" ..
    #image .. " > " .. maxSize .. " bytes)\n")
  return 1
end

local _, reason = eeprom.set(image)
if reason then
  io.stderr:write(tostring(reason) .. "\n")
  return 1
end

local label = args[2] or DEFAULT_LABEL
local _, lreason = eeprom.setLabel(label)
if lreason then
  io.stderr:write(tostring(lreason) .. "\n")
  return 1
end

if not options.q then
  io.write("Set label to '" .. eeprom.getLabel() .. "'.\n")
  io.write("All done! You can remove the EEPROM and re-insert the previous one now.\n")
end
