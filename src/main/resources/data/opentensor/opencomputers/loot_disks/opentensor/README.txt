OpenTensor floppy disk
======================

Tools:
  opentensor-smi  - list all connected GPUs (any vendor) with declared VRAM,
                    temperature, VRAM usage and cooling fans. Like nvidia-smi.
                    Usage: opentensor-smi [-v]
  neofetch        - show system info: OS, CPU, memory, GPUs, fans. Like neofetch.
  asm-flash       - assemble .asm source to EEPROM bytecode and flash it.
                    Like flash, but takes assembly, not Lua.
                    Usage: asm-flash [-q] <file.asm> [label]
                    Without a label the EEPROM gets 'EEPROM (ASM BIOS)'.
                    Example: asm-flash -q hello_world.asm "ASM BIOS"
                    Sources live in /usr/share/asm (hello_world.asm).

Mini-ISA (registers R0-R7, labels `name:`, `;` comments):
  MOV Rd, src     Rd = src (register, number or "string")
  ADD/SUB/MUL/DIV Rd, Rs
  CMP Ra, Rb      Z = equal, G = greater (numbers)
  JMP/JZ/JNZ/JGT/JLT L
  PUSH src / POP Rd / CALL L / RET
  PRINT src       via GPU (EEPROM has no print/io)
  BEEP freq, dur  via computer.beep
  SLEEP n         seconds, via computer.uptime
  HALT
  [label:] DB v, ...   data, read back through its label
NOTE: EEPROM code runs before OpenOS loads, so there is no keyboard
input, no io/os and no filesystem access in flashed programs.
HALT stops the program and parks the machine ON (yield loop, screen
output kept). ASM BIOS images are standalone: no OpenOS boot involved.

Install (pick one):
  1. Run the installer from this floppy:
       /mnt/<floppy-address>/install.lua
  2. Or copy by hand:
       cp -r /mnt/<floppy-address>/usr/bin/* /usr/bin/
       cp -r /mnt/<floppy-address>/usr/share/* /usr/share/

Notes:
  - opentensor-smi shows the DECLARED memory of each card (e.g. A100 80GB
    shows 80GB) and its live temperature via gpu.getTemperature().
  - Tensor GPUs heat up under load. Above 100C the machine shuts down.
    Put Noctua NF-A14 fans next to them to cool them down:
    bus version for servers/racks, card version for plain computers.
