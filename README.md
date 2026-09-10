# OpenTensor

Datacenter hardware for [OpenComputers: Rebooted](https://github.com/CaitlynMainer/OpenComputers) (Minecraft 1.21.1 / NeoForge).

Adds 17 GPU-tier cards, a Groq LPU (CPU + graphics APU), DDR5 ECC REG memory sticks, AMD EPYC server CPUs and a Samsung 840 Pro SSD — all installable into stock Tier 3 (and better) computers, servers and racks. No OpenComputers sources are modified; integration happens exclusively through the public `li.cil.oc.api.Driver` API.

## Contents

### Graphics cards (Card slot, Tier 3, 190×60, 8-bit)

| Item | Real card | VRAM reporting scale | Speed/energy scale |
|---|---|---|---|
| NVIDIA A100 40GB / 80GB | Ampere, 2020 | 8.0 / 16.0 screens | 1.0 / 0.85 |
| NVIDIA H100 80GB / 96GB | Hopper, 2022 | 16.0 / 20.0 screens | 0.7 / 0.6 |
| NVIDIA H200 141GB | Hopper, 2023 | 28.0 screens | 0.5 |
| NVIDIA RTX PRO 6000 Blackwell 96GB | Blackwell, 2025 | 20.0 screens | 0.55 |
| NVIDIA Tesla P40 24GB | Pascal, 2016 | 4.8 screens | 1.2 |
| NVIDIA Tesla T4 16GB | Turing, 2018 | 3.2 screens | 1.1 |
| NVIDIA Tesla V100 16GB / 32GB | Volta, 2017 | 3.2 / 6.4 screens | 0.95 |
| NVIDIA GeForce RTX 3090 24GB | Ampere, 2020 | 4.8 screens | 0.9 |
| NVIDIA GeForce GT 730 1GB / 2GB / 4GB | Kepler, 2014 | 0.2 / 0.4 / 0.8 screens | 1.5 |
| AMD Radeon RX 580 4GB / 8GB | Polaris, 2017 | 0.8 / 1.6 screens | 1.0 |
| Google TPU v6e 32GB | Trillium, 2024 | 6.4 screens | 0.65 |

Scale semantics: `vramScreens` multiplies one full screen of cells (stock Tier 3 = 4.0) for `totalMemory`; `costScale` multiplies call-budget and energy costs (< 1.0 = faster/cheaper than stock). Low-end cards (GT 730, RX 580) honestly report less VRAM than stock. Energy costs follow stock OC normalization (config values divided by 800 basic-screen pixels), so full-screen operations work on any power setup.

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
| AMD EPYC 9354P | 32-core Genoa, 2023 | 32 | 3.0 |
| AMD EPYC 7303P | 16-core Milan, 2021 | 24 | 2.5 |

Standard Lua architectures (reconfigurable), like stock CPUs.

### Samsung 840 Pro SSD (HDD slot, Tier 2)

Labelled 512 GB, actual capacity 512 MB. Behaves like a stock SSD (save-backed storage, persistent address and label, SSD energy tariffs, silent access, speed class of SSD Tier 2).

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

- `src/main/java/com/opentensor/` — `TensorGpuSpec` / `TensorMemorySpec` / `TensorCpuSpec` / `TensorSsdSpec` (hardware definitions), item classes, environments (`TensorGpuEnvironment`, `GroqLpuEnvironment`, `TensorMemoryEnvironment`, `TensorCpuEnvironment`), drivers (`TensorDriver`, `GroqDriver`, `TensorMemoryDriver`, `TensorCpuDriver`, `TensorSsdDriver`), `ModItems`, `OpenTensorTabs`, `OpenTensor`.
- `src/main/resources/assets/opentensor/` — models, textures, `en_us`/`ru_ru` locales (tooltips carry short real-world descriptions of each card).
- Root `*.png` files next to `build.gradle` are the source textures copied into `assets/.../textures/item/` at authoring time.

## License

GPL-3.0-only — see [LICENSE](LICENSE). OpenComputers: Rebooted (required dependency) is distributed under its own license — see its repository.
