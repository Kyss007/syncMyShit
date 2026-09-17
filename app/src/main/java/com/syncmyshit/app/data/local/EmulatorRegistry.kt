package com.syncmyshit.app.data.local

import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.ProfileCategory

object EmulatorRegistry {

    val BUILT_IN_PROFILES: List<EmulatorProfile> = listOf(
        // RetroArch
        EmulatorProfile(
            id = "retroarch",
            name = "RetroArch",
            system = "Multi-System",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "com.retroarch",
                "com.retroarch.aarch64",
                "com.retroarch.ra32"
            ),
            candidatePaths = listOf(
                "RetroArch/saves",
                "RetroArch/states",
                "Android/data/com.retroarch/files/saves",
                "Android/data/com.retroarch.aarch64/files/saves",
                "Android/data/com.retroarch.ra32/files/saves"
            ),
            fileExtensions = listOf(".srm", ".state", ".state.auto", ".rtc", ".brm", ".cht"),
            driveSubfolder = "RetroArch"
        ),

        // Sony PlayStation Portable (PPSSPP)
        EmulatorProfile(
            id = "ppsspp",
            name = "PPSSPP",
            system = "Sony PSP",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold"),
            candidatePaths = listOf(
                "PSP/SAVEDATA",
                "PSP/PPSSPP_STATE",
                "Android/data/org.ppsspp.ppsspp/files/PSP/SAVEDATA",
                "Android/data/org.ppsspp.ppssppgold/files/PSP/SAVEDATA"
            ),
            fileExtensions = listOf(".bin", ".sfo", ".png", ".ppst"),
            driveSubfolder = "PPSSPP"
        ),

        // Sony PlayStation 2 (AetherSX2 / NetherSX2)
        EmulatorProfile(
            id = "aethersx2",
            name = "AetherSX2 / NetherSX2",
            system = "Sony PlayStation 2",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("xyz.aethersx2.android", "xyz.nether.android"),
            candidatePaths = listOf(
                "Android/data/xyz.aethersx2.android/files/memcards",
                "Android/data/xyz.aethersx2.android/files/sstates",
                "Android/data/xyz.nether.android/files/memcards",
                "Android/data/xyz.nether.android/files/sstates",
                "AetherSX2/memcards"
            ),
            fileExtensions = listOf(".ps2", ".mcd", ".p2s"),
            driveSubfolder = "AetherSX2"
        ),

        // Nintendo GameCube / Wii (Dolphin)
        EmulatorProfile(
            id = "dolphin",
            name = "Dolphin Emulator",
            system = "Nintendo GameCube & Wii",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.dolphinemu.dolphinemu",
                "org.mm.jr",
                "org.dolphinemu.mmjr"
            ),
            candidatePaths = listOf(
                "dolphin-emu/GC",
                "dolphin-emu/Wii/title",
                "dolphin-emu/StateSaves",
                "Android/data/org.dolphinemu.dolphinemu/files/GC",
                "Android/data/org.dolphinemu.dolphinemu/files/Wii/title"
            ),
            fileExtensions = listOf(".raw", ".gci", ".sav", ".ssp", ".bin"),
            driveSubfolder = "Dolphin"
        ),

        // Nintendo 3DS (Citra / Lime3DS / Azahar)
        EmulatorProfile(
            id = "citra",
            name = "Citra / Lime3DS / Azahar",
            system = "Nintendo 3DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.citra.citra_emu",
                "org.citra.citra_emu.canary",
                "io.github.lime3ds",
                "org.citra.emu"
            ),
            candidatePaths = listOf(
                "citra-emu/sdmc",
                "lime3ds-emu/sdmc",
                "Android/data/org.citra.citra_emu/files/sdmc",
                "Android/data/io.github.lime3ds/files/sdmc"
            ),
            fileExtensions = listOf(".sav", ".dat", ".bin", ".db"),
            driveSubfolder = "Citra_3DS"
        ),

        // Nintendo Switch (Yuzu / Suyu / Sudachi / Uzuy)
        EmulatorProfile(
            id = "switch_emu",
            name = "Yuzu / Suyu / Sudachi / Uzuy",
            system = "Nintendo Switch",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.yuzu.yuzu_emu",
                "org.yuzu.yuzu_emu.ea",
                "org.suyu.suyu_emu",
                "org.sudachi.sudachi_emu",
                "app.uzuy.edge"
            ),
            candidatePaths = listOf(
                "Android/data/org.yuzu.yuzu_emu/files/nand/user/save",
                "Android/data/org.suyu.suyu_emu/files/nand/user/save",
                "Android/data/org.sudachi.sudachi_emu/files/nand/user/save",
                "yuzu/nand/user/save"
            ),
            fileExtensions = listOf(".bin", ".dat", ".sav"),
            driveSubfolder = "Switch"
        ),

        // Sony PlayStation 1 (DuckStation)
        EmulatorProfile(
            id = "duckstation",
            name = "DuckStation",
            system = "Sony PlayStation",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.github.stenzek.duckstation"),
            candidatePaths = listOf(
                "Android/data/com.github.stenzek.duckstation/files/memcards",
                "Android/data/com.github.stenzek.duckstation/files/savestates",
                "duckstation/memcards"
            ),
            fileExtensions = listOf(".mcd", ".mcr", ".sav"),
            driveSubfolder = "DuckStation"
        ),

        // Sony PlayStation Vita (Vita3K)
        EmulatorProfile(
            id = "vita3k",
            name = "Vita3K",
            system = "Sony PS Vita",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("org.vita3k.emulator"),
            candidatePaths = listOf(
                "Android/data/org.vita3k.emulator/files/vita3k/ux0/user/00/savedata",
                "vita3k/ux0/user/00/savedata"
            ),
            fileExtensions = listOf(".bin", ".dat", ".png"),
            driveSubfolder = "Vita3K"
        ),

        // Nintendo 64 (Mupen64Plus FZ)
        EmulatorProfile(
            id = "mupen64plus",
            name = "Mupen64Plus FZ",
            system = "Nintendo 64",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.mupen64plusae.v3.fzurita",
                "org.mupen64plusae.v3.fzurita.pro"
            ),
            candidatePaths = listOf(
                "Mupen64PlusFZ/GameSaves",
                "Android/data/org.mupen64plusae.v3.fzurita/files/GameSaves"
            ),
            fileExtensions = listOf(".mpk", ".fla", ".sra", ".eep", ".st0", ".st1", ".st2"),
            driveSubfolder = "Mupen64"
        ),

        // Sega Dreamcast (Flycast & Redream)
        EmulatorProfile(
            id = "flycast",
            name = "Flycast",
            system = "Sega Dreamcast & Naomi",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.flyinghead.Flycast"),
            candidatePaths = listOf(
                "Flycast/data",
                "Android/data/com.flyinghead.Flycast/files/data"
            ),
            fileExtensions = listOf(".bin", ".state", ".nvmem"),
            driveSubfolder = "Flycast"
        ),
        EmulatorProfile(
            id = "redream",
            name = "Redream",
            system = "Sega Dreamcast",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("io.recompiled.redream"),
            candidatePaths = listOf(
                "redream",
                "Android/data/io.recompiled.redream/files"
            ),
            fileExtensions = listOf(".bin", ".state"),
            driveSubfolder = "Redream"
        ),

        // Nintendo DS (DraStic & MelonDS)
        EmulatorProfile(
            id = "drastic",
            name = "DraStic",
            system = "Nintendo DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.dsemu.drastic"),
            candidatePaths = listOf(
                "DraStic/backup",
                "DraStic/savestates",
                "Android/data/com.dsemu.drastic/files/backup"
            ),
            fileExtensions = listOf(".dsv", ".dss"),
            driveSubfolder = "DraStic"
        ),
        EmulatorProfile(
            id = "melonds",
            name = "MelonDS",
            system = "Nintendo DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("me.magnum.melonds"),
            candidatePaths = listOf(
                "Android/data/me.magnum.melonds/files",
                "melonDS"
            ),
            fileExtensions = listOf(".sav", ".mln"),
            driveSubfolder = "MelonDS"
        ),

        // Game Boy Advance / Color (Pizza Boy, MyBoy)
        EmulatorProfile(
            id = "pizzaboy",
            name = "Pizza Boy GBA / GBC",
            system = "Game Boy Advance & Color",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "it.dbtecno.pizzaboygba",
                "it.dbtecno.pizzaboygbapro",
                "it.dbtecno.pizzaboypro",
                "it.dbtecno.pizzaboy"
            ),
            candidatePaths = listOf(
                "Android/data/it.dbtecno.pizzaboygba/files/savegames",
                "Android/data/it.dbtecno.pizzaboygbapro/files/savegames",
                "PizzaBoyGBA/savegames",
                "PizzaBoyGBC/savegames"
            ),
            fileExtensions = listOf(".sav", ".sta"),
            driveSubfolder = "PizzaBoy"
        ),
        EmulatorProfile(
            id = "myboy",
            name = "MyBoy! / MyOldBoy!",
            system = "Game Boy Advance & Color",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.fastemulator.gba", "com.fastemulator.gbc"),
            candidatePaths = listOf("MyBoy/save", "MyOldBoy/save"),
            fileExtensions = listOf(".sav", ".st0", ".st1", ".st2"),
            driveSubfolder = "MyBoy"
        ),

        // Recompiled Projects & Native Ports
        EmulatorProfile(
            id = "zelda64_recomp",
            name = "Zelda 64: Recompiled (MM & OoT)",
            system = "Recomp",
            category = ProfileCategory.RECOMP,
            packageNames = listOf(
                "com.zelda64recomp.mm",
                "com.zelda64recomp.oot",
                "recomp.zelda64",
                "com.zelda64.recomp"
            ),
            candidatePaths = listOf(
                "Android/data/com.zelda64recomp.mm/files/saves",
                "Android/data/com.zelda64recomp.oot/files/saves",
                "Zelda64Recomp/saves",
                "ZeldaRecomp/saves"
            ),
            fileExtensions = listOf(".sav", ".bin", ".json"),
            driveSubfolder = "Recomp_Zelda64"
        ),
        EmulatorProfile(
            id = "ship_of_harkinian",
            name = "Ship of Harkinian (SoH & 2S2H)",
            system = "Native Port",
            category = ProfileCategory.RECOMP,
            packageNames = listOf(
                "com.soh.android",
                "com.soh.twoship",
                "com.harbourmasters.shipofharkinian"
            ),
            candidatePaths = listOf(
                "ShipOfHarkinian/saves",
                "2Ship2Harkinian/saves",
                "Android/data/com.soh.android/files/saves",
                "Android/data/com.soh.twoship/files/saves"
            ),
            fileExtensions = listOf(".sav", ".json"),
            driveSubfolder = "Recomp_SoH"
        ),
        EmulatorProfile(
            id = "sm64_android",
            name = "Super Mario 64 (sm64ex)",
            system = "Native Port",
            category = ProfileCategory.RECOMP,
            packageNames = listOf("com.retro.sm64ex", "com.n64.sm64"),
            candidatePaths = listOf(
                "Android/data/com.retro.sm64ex/files/save",
                "sm64ex/save"
            ),
            fileExtensions = listOf(".eep", ".sav"),
            driveSubfolder = "Recomp_SM64"
        ),
        EmulatorProfile(
            id = "perfect_dark_recomp",
            name = "Perfect Dark Recompiled",
            system = "Recomp",
            category = ProfileCategory.RECOMP,
            packageNames = listOf("com.perfectdark.recomp"),
            candidatePaths = listOf(
                "Android/data/com.perfectdark.recomp/files/saves",
                "PerfectDarkRecomp/saves"
            ),
            fileExtensions = listOf(".sav", ".eep"),
            driveSubfolder = "Recomp_PerfectDark"
        ),
        EmulatorProfile(
            id = "am2r_android",
            name = "AM2R (Another Metroid 2 Remake)",
            system = "Native Port",
            category = ProfileCategory.STANDALONE_GAME,
            packageNames = listOf("com.am2r.android"),
            candidatePaths = listOf(
                "Android/data/com.am2r.android/files/save",
                "AM2R/save"
            ),
            fileExtensions = listOf(".sav", ".dat"),
            driveSubfolder = "Game_AM2R"
        ),
        EmulatorProfile(
            id = "portmaster",
            name = "PortMaster Saves",
            system = "Handheld Ports",
            category = ProfileCategory.STANDALONE_GAME,
            packageNames = listOf("com.portmaster.launcher"),
            candidatePaths = listOf(
                "PortMaster/saves",
                "roms/ports/savedata",
                "ports/savedata"
            ),
            fileExtensions = listOf(".sav", ".dat", ".json", ".bin"),
            driveSubfolder = "PortMaster"
        ),
        EmulatorProfile(
            id = "balatro_mobile",
            name = "Balatro (Love2D / Mobile Port)",
            system = "Standalone Game",
            category = ProfileCategory.STANDALONE_GAME,
            packageNames = listOf("org.love2d.android", "com.balatro.apk"),
            candidatePaths = listOf(
                "Android/data/org.love2d.android/files/save",
                "Balatro/saves"
            ),
            fileExtensions = listOf(".jkr", ".dat"),
            driveSubfolder = "Game_Balatro"
        )
    )
}
