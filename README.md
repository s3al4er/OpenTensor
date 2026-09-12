# OpenTensor

Datacenter hardware for [OpenComputers: Rebooted](https://github.com/CaitlynMainer/OpenComputers) (Minecraft 1.21.1 / NeoForge).

Adds 27 GPU-tier cards plus a Tier 5 GPU, a Tier 5 screen (380×120), a Groq LPU (CPU + graphics APU), DDR5 ECC REG memory sticks, AMD EPYC server CPUs, a Samsung 840 Pro SSD, Noctua cooling fans and an OpenTensor tools floppy — all installable into stock Tier 3 (and better) computers, servers and racks. No OpenComputers sources are modified; integration happens exclusively through the public `li.cil.oc.api.Driver` API.

## Contents

### Graphics cards (Card slot, Tier 3, 190×60, 8-bit)

| Item | Real card | VRAM reporting scale | Speed/energy scale |
|---|---|---|---|
| NVIDIA A100 40GB / 80GB | Ampere | 8.0 / 16.0 screens | 1.0 / 0.85 |
| NVIDIA H100 80GB / 96GB | Hopper | 16.0 / 20.0 screens | 0.7 / 0.6 |
| NVIDIA H200 141GB | Hopper | 28.0 screens | 0.5 |
| NVIDIA RTX PRO 6000 Blackwell 96GB | Blackwell | 20.0 screens | 0.55 |
| NVIDIA RTX PRO 6000 Ada 48GB | Ada | 9.6 screens | 0.6 |
| NVIDIA A10 24GB | Ampere | 4.8 screens | 1.0 |
| NVIDIA A30 24GB | Ampere | 4.8 screens | 0.9 |
| NVIDIA A40 48GB | Ampere | 9.6 screens | 0.75 |
| NVIDIA Tesla P40 24GB | Pascal | 4.8 screens | 1.2 |
| NVIDIA Tesla T4 16GB | Turing | 3.2 screens | 1.1 |
| NVIDIA Tesla K80 24GB | Kepler | 4.8 screens | 1.4 |
| NVIDIA Tesla P100 16GB | Pascal | 3.2 screens | 1.05 |
| NVIDIA Tesla V100 16GB / 32GB | Volta | 3.2 / 6.4 screens | 0.95 |
| NVIDIA GeForce RTX 3090 24GB | Ampere | 4.8 screens | 0.9 |
| NVIDIA GeForce GT 730 1GB / 2GB / 4GB | Kepler | 0.2 / 0.4 / 0.8 screens | 1.5 |
| AMD Radeon RX 580 4GB / 8GB | Polaris | 0.8 / 1.6 screens | 1.0 |
| AMD Radeon Instinct MI50 16GB | GCN | 3.2 screens | 1.0 |
| AMD Instinct MI100 32GB | CDNA | 6.4 screens | 0.8 |
| AMD Instinct MI200 64GB | CDNA2 | 12.8 screens | 0.6 |
| AMD Instinct MI250X 128GB | CDNA2 | 25.6 screens | 0.55 |
| Google TPU v6e 32GB | Trillium | 6.4 screens | 0.65 |

Scale semantics: `vramScreens` multiplies one full screen of cells (stock Tier 3 = 4.0) for `totalMemory`; `costScale` multiplies call-budget and energy costs (< 1.0 = faster/cheaper than stock). Low-end cards (GT 730, RX 580) honestly report less VRAM than stock. Energy costs follow stock OC normalization (config values divided by 800 basic-screen pixels), so full-screen operations work on any power setup.

> **Slot-compatibility note:** all Tensor GPUs report driver tier 3, so they install into Tier-4-class Card slots (creative case, high-end servers/racks) — same as the strongest stock cards. The LPU, DDR5, EPYC and SSD report tier 2 and fit stock Tier 3 machines.

### Tier 5 screen + RTX 5090 (380×120, 16-bit)

- **Tier 5 Screen** (block): 380×120 at 16-bit color — twice the stock Tier 4 per axis. Implemented as a subclass of the stock screen, so the stock renderer, baked model, right-click GUI, touch/keyboard input, redstone and analyzer support all work unchanged. Factory frame color is OC blue (dye-click recolors like stock; multiblock-merge needs matching colors, so blue walls stay apart from stock gray ones; pre-existing gray screens migrate to blue on load, dyed ones keep their color). Quirk: its mechanics tier is 3 under the hood (stock tables end at Tier 4).
- **NVIDIA GeForce RTX 5090 32GB** (Card slot): Blackwell gaming GPU. 6.4 VRAM screens, 0.45 cost scale — the only card able to drive the Tier 5 screen at full resolution (`gpu.maxResolution()` → `380 120`). Installed components show their address (UUID) in the tooltip, like stock.

### Groq LPU (CPU slot)

An "APU Tier 2.5": like the stock APU Tier 2, but 1.5× stronger on every axis.

- Processor side: 24 supported components, 2.25 call budget, standard Lua architectures (reconfigurable).
- Graphics side (`gpu` component): 80×25, 4-bit, 6000 VRAM cells, Tier-2-class costs divided by 1.5.

### DDR5 ECC REG memory (Memory slot, Tier 2)

| Label | Actual RAM granted |
|---|---|
| DDR5 16GB | 32 MB |
| DDR5 32GB | 64 MB |
| DDR5 64GB | 128 MB |
| DDR5 128GB | 256 MB |

Call budget 1.5 per stick, like high-end stock RAM. advertised sizes are labels only.

> **Note:** OpenComputers clamps total Lua RAM to `computer.lua.maxTotalRam` (default 64 MB), so with default config `free` will show ~64 MB no matter how many sticks are installed. To use the full capacity, set e.g. `computer.lua.maxTotalRam: 1073741824` (1 GB) in `config/opencomputers/application.conf` and restart. Do not exceed ~1 GB total per machine (32-bit and `ramScaleFor64Bit` overflow ceilings in OC itself).

### AMD EPYC CPUs (CPU slot, Tier 2)

| Item | Real chip | Components | Call budget |
|---|---|---|---|
| AMD EPYC 9354P | 32-core Genoa | 32 | 3.0 |
| AMD EPYC 7303P | 16-core Milan | 24 | 2.5 |

Standard Lua architectures (reconfigurable), like stock CPUs.

### Samsung 840 Pro SSD (HDD slot, Tier 2)

Labelled 512 GB, actual capacity 512 MB. Behaves like a stock SSD (save-backed storage, persistent address and label, SSD energy tariffs, silent access, speed class of SSD Tier 2).

### GPU temperature and overheat protection (1.0.9+)

Every Tensor GPU (including the RTX 5090) simulates temperature:

- Starts at 30°C. Drawing on a bound screen (`set`, `fill`, `copy`, `bitblt`) heats the card up, plus idle heat while the machine runs (a stopped machine only cools down). Passive cooling follows Newton's law toward the 30°C ambient; each Noctua fan in the same machine cools much faster. Rule of thumb: ~70°C idle with no fans, ~30°C with fans, ~75–80°C under light load without cooling, overheat under sustained heavy load without cooling.
- Live values via Lua: `gpu.getTemperature()` (°C) and `gpu.getFans()` (fans cooling this card), also visible in `computer.getDeviceInfo()` as `temperature`/`fans`, and in `opentensor-smi` (check the `Fans` column: `0` with fans installed means they are in the wrong machine).

### Noctua NF-A14 industrialPPC-3000 PWM (two mount variants)

Cools all Tensor GPUs in the same machine (stacks: more fans = faster cooling, down to the 30°C ambient floor; only fans in the same computer/server/rack mount count, not fans on other networked machines). Exposes a `fan` component with `fan.getSpeed()` (3000 RPM) and device info, so `opentensor-smi`/`neofetch` can see it. Suggested: one fan per 1–2 loaded cards.

- `noctua_nf_a14` — component-bus slot, Tier 2. For servers and racks (plain computers have no bus slots).
- `noctua_nf_a14_card` — card slot, Tier 1. For plain computers (and servers); occupies one card slot.

### OpenTensor floppy (loot disk)

A `OpenTensor` floppy (OpenComputers creative tab, Misc section; weight 0 so it never spawns in dungeon loot) ships three Lua tools:

- `opentensor-smi` — lists **all** connected `gpu` components of any vendor with declared VRAM (e.g. an A100 80GB shows 80GB), live temperature and VRAM usage. Like `nvidia-smi`. `opentensor-smi -v` for verbose addresses.
- `neofetch` — system overview: OS, CPU, memory, display resolution, GPUs (VRAM + temperature), fans. Like `neofetch`.
- `asm-flash` — assembles `.asm` source to EEPROM bytecode and flashes it, like `flash` but for assembly: `asm-flash [-q] <file.asm> [label]`. Without a label the EEPROM gets `EEPROM (ASM BIOS)`. Example: `asm-flash -q hello_world.asm "ASM BIOS"` (example source in `/usr/share/asm`). `HALT` stops the program with the machine staying ON (output kept) — ASM BIOS images are standalone, no OpenOS boot.

Mini-ISA: registers `R0–R7`, labels `name:`, `;` comments; `MOV ADD SUB MUL DIV CMP JMP JZ JNZ JGT JLT PRINT SLEEP HALT` plus `PUSH POP CALL RET`, `BEEP freq, dur` and `[label:] DB v, ...` data. `PRINT` goes through the GPU (there is no `print`/`io` in EEPROM context), `SLEEP` uses `computer.uptime()`; keyboard input is unavailable before OpenOS loads.

Install after inserting the floppy (pick one):

```sh
/mnt/<floppy-address>/install.lua
# or
cp -r /mnt/<floppy-address>/usr/bin/* /usr/bin/
cp -r /mnt/<floppy-address>/usr/share/* /usr/share/
```

> **Important:** scripts in `/usr/bin` do NOT update themselves. After every mod update, re-run the installer from the new floppy — otherwise old scripts will talk to the new jar (or vice versa). `opentensor-smi` prints its own version in the header (currently `1.0.9-b5`); if the `Fans` column shows `n/a` instead of numbers, your scripts are older than the jar.

> **Update order matters, strictly:** new jar → world restart → fresh floppy → `/mnt/<address>/install.lua` (full path! bare `install` is the OpenOS installer, not ours) → re-flash EEPROMs. The installer (prints its own version, currently `1.0.9-b5`) scans `/mnt` for an up-to-date disk and **refuses** to install from stale ones — if it aborts, the jar wasn't updated or the world wasn't restarted. Sanity check after install: `grep -c getBootAddress /usr/bin/asm-flash.lua` must print a number above 0.

Direct `/give` (it is an `opencomputers:floppy` with loot-disk data, not an `opentensor:` item):

```mcfunction
/give @p opencomputers:floppy[opencomputers:label="opentensor",opencomputers:loot_disk="opentensor:opentensor",opencomputers:disk_color="cyan",minecraft:custom_name='"OpenTensor"']
```

## Requirements

- Minecraft 1.21.1, NeoForge 21+
- OpenComputers: Rebooted 1.9+ (`opencomputers` jar must be installed **alongside** this mod, on both client and dedicated server)
- Java 21

## Installation

1. Install NeoForge 1.21.1 and OpenComputers: Rebooted.
2. Drop `opentensor-<version>.jar` into `mods/` (client and server).
3. (Optional) Raise `computer.lua.maxTotalRam` as described above if you use the DDR5 sticks.

Items are creative-only (no recipes); on a server use `/give @p opentensor:<id>` or the creative inventory.

## Building from source

```sh
./gradlew build
# output: build/libs/opentensor-<version>.jar
```

Requires JDK 21 (Gradle toolchain). OpenComputers is resolved from Maven (`compileOnly` + non-transitive `runtimeOnly` for dev runs) — no OC sources needed.

## Project layout

- `src/main/java/com/opentensor/` — `TensorGpuSpec` / `TensorMemorySpec` / `TensorCpuSpec` / `TensorSsdSpec` (hardware definitions), `TensorThermal` (shared GPU heat model), item classes, environments (`TensorGpuEnvironment`, `TensorTier5GpuEnvironment`, `GroqLpuEnvironment`, `TensorMemoryEnvironment`, `TensorCpuEnvironment`, `NoctuaFanEnvironment`), drivers (`TensorDriver`, `TensorTier5Driver`, `GroqDriver`, `TensorMemoryDriver`, `TensorCpuDriver`, `TensorSsdDriver`, `NoctuaFanDriver`), `ModItems`, `OpenTensorTabs`, `OpenTensor`.
- `src/main/resources/assets/opentensor/` — models, textures, `en_us`/`ru_ru` locales (tooltips carry short real-world descriptions of each card).
- `src/main/resources/data/opentensor/opencomputers/loot_disks/opentensor/` — the OpenTensor floppy: `usr/bin/opentensor-smi.lua`, `usr/bin/neofetch.lua`, `install.lua`, `README.txt`.
- Root `*.png` files next to `build.gradle` are the source textures copied into `assets/.../textures/item/` at authoring time.

## License

GPL-3.0-only — see [LICENSE](LICENSE). OpenComputers: Rebooted (required dependency) is distributed under its own license — see its repository.
