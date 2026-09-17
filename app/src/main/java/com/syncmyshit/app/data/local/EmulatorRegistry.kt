package com.syncmyshit.app.data.local

import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.ProfileCategory

object EmulatorRegistry {

    val BUILT_IN_PROFILES: List<EmulatorProfile> = listOf(
        // RetroArch (Covers 100+ libretro cores across all platforms)
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
            packageNames = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspplegacy"),
            candidatePaths = listOf(
                "PSP/SAVEDATA",
                "PSP/PPSSPP_STATE",
                "Android/data/org.ppsspp.ppsspp/files/PSP/SAVEDATA",
                "Android/data/org.ppsspp.ppssppgold/files/PSP/SAVEDATA"
            ),
            fileExtensions = listOf(".bin", ".sfo", ".png", ".ppst"),
            driveSubfolder = "PPSSPP"
        ),

        // Sony PlayStation 2 (AetherSX2, NetherSX2, ARMSX2)
        EmulatorProfile(
            id = "aethersx2",
            name = "AetherSX2 / NetherSX2 / ARMSX2",
            system = "Sony PlayStation 2",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "xyz.aethersx2.android",
                "xyz.nether.android",
                "xyz.aethersx2.custom",
                "xyz.aethersx2.tturnip",
                "come.nanodata.armsx2",
                "come.nanodata.armsx2.debug",
                "com.armsx2"
            ),
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

        // Sony PlayStation 1 (DuckStation, ePSXe, FPse, ARMSX1)
        EmulatorProfile(
            id = "duckstation",
            name = "DuckStation / ePSXe / FPse",
            system = "Sony PlayStation",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "com.github.stenzek.duckstation",
                "com.epsxe.ePSXe",
                "com.emulator.fpse",
                "com.emulator.fpse64",
                "com.nanodata.armsx"
            ),
            candidatePaths = listOf(
                "Android/data/com.github.stenzek.duckstation/files/memcards",
                "Android/data/com.github.stenzek.duckstation/files/savestates",
                "duckstation/memcards",
                "epsxe/memcards",
                "Android/data/com.epsxe.ePSXe/files"
            ),
            fileExtensions = listOf(".mcd", ".mcr", ".sav", ".bin"),
            driveSubfolder = "DuckStation"
        ),

        // Sony PlayStation Vita (Vita3K)
        EmulatorProfile(
            id = "vita3k",
            name = "Vita3K",
            system = "Sony PS Vita",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("org.vita3k.emulator", "org.vita3k.emulator.ikhoeyZX"),
            candidatePaths = listOf(
                "Android/data/org.vita3k.emulator/files/vita3k/ux0/user/00/savedata",
                "vita3k/ux0/user/00/savedata"
            ),
            fileExtensions = listOf(".bin", ".dat", ".png"),
            driveSubfolder = "Vita3K"
        ),

        // Sony PlayStation 3 (aPS3e, ARMSX3)
        EmulatorProfile(
            id = "aps3e",
            name = "aPS3e / ARMSX3",
            system = "Sony PlayStation 3",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("aenu.aps3e", "com.armsx3"),
            candidatePaths = listOf(
                "Android/data/aenu.aps3e/files/dev_hdd0/home/00000001/savedata",
                "aps3e/savedata"
            ),
            fileExtensions = listOf(".bin", ".dat", ".png"),
            driveSubfolder = "PS3_aPS3e"
        ),

        // Nintendo GameCube / Wii / WiiWare (Dolphin, MMJR, Ishiiruka, PrimeHack)
        EmulatorProfile(
            id = "dolphin",
            name = "Dolphin (Official / MMJR / Ishiiruka)",
            system = "Nintendo GameCube & Wii",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.dolphinemu.dolphinemu",
                "org.dolphinemu.dolphinemu.debug",
                "org.dolphinemu.handheld",
                "org.dolphinemu.mmjr",
                "org.dolphinemu.mmjr3",
                "org.mm.jr",
                "org.mm.j",
                "org.dolphin.ishiirukadark",
                "org.shiiion.primehack"
            ),
            candidatePaths = listOf(
                "dolphin-emu/GC",
                "dolphin-emu/Wii/title",
                "dolphin-emu/StateSaves",
                "Android/data/org.dolphinemu.dolphinemu/files/GC",
                "Android/data/org.dolphinemu.dolphinemu/files/Wii/title",
                "Android/data/org.dolphinemu.mmjr/files/GC"
            ),
            fileExtensions = listOf(".raw", ".gci", ".sav", ".ssp", ".bin"),
            driveSubfolder = "Dolphin"
        ),

        // Nintendo Wii U (CEMU Android)
        EmulatorProfile(
            id = "cemu_android",
            name = "CEMU (Wii U)",
            system = "Nintendo Wii U",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("info.cemu.cemu"),
            candidatePaths = listOf(
                "Android/data/info.cemu.cemu/files/mlc01/usr/save",
                "cemu/mlc01/usr/save"
            ),
            fileExtensions = listOf(".bin", ".dat", ".txt"),
            driveSubfolder = "WiiU_Cemu"
        ),

        // Nintendo 3DS (Citra, Lime3DS, Azahar, Mandarine, Borked 3DS, Lemonade)
        EmulatorProfile(
            id = "citra",
            name = "Citra / Lime3DS / Azahar / Mandarine",
            system = "Nintendo 3DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.citra.citra_emu",
                "org.citra.citra_emu.canary",
                "org.citra.emu",
                "io.github.lime3ds.android",
                "org.azahar_emu.azahar",
                "org.azahar_emu.azahar.debug",
                "io.github.mandarine3ds.mandarine",
                "io.github.borked3ds.android",
                "org.gamerytb.lemonade.canary"
            ),
            candidatePaths = listOf(
                "citra-emu/sdmc",
                "lime3ds-emu/sdmc",
                "Android/data/org.citra.citra_emu/files/sdmc",
                "Android/data/io.github.lime3ds.android/files/sdmc",
                "Android/data/org.azahar_emu.azahar/files/sdmc"
            ),
            fileExtensions = listOf(".sav", ".dat", ".bin", ".db"),
            driveSubfolder = "Citra_3DS"
        ),

        // Nintendo Switch (Yuzu, Suyu, Sudachi, Citron, Eden, Benji-SC, Kenji-NX, Skyline, Strato)
        EmulatorProfile(
            id = "switch_emu",
            name = "Switch (Yuzu / Suyu / Sudachi / Citron / Eden / Skyline)",
            system = "Nintendo Switch",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.yuzu.yuzu_emu",
                "org.yuzu.yuzu_emu.ea",
                "org.suyu.suyu_emu",
                "dev.suyu.suyu_emu.relWithDebInfo",
                "org.sudachi.sudachi_emu",
                "org.sudachi.sudachi_emu.ea",
                "org.citron.citron_emu",
                "dev.eden.eden_emulator",
                "dev.eden.eden_emulator.nightly",
                "dev.legacy.eden_emulator",
                "dev.legacy.eden_emulator.nightly",
                "com.miHoYo.Yuanshen",
                "com.miHoYo.Yuanshen.nightly",
                "org.benjisc.android",
                "org.kenjinx.android",
                "skyline.emu",
                "org.stratoemu.strato"
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

        // Nintendo DS / DSi (DraStic, MelonDS, NooDS)
        EmulatorProfile(
            id = "drastic",
            name = "DraStic",
            system = "Nintendo DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.dsemu.drastic"),
            candidatePaths = listOf(
                "DraStic/backup",
                "DraStic/savestates",
                "Android/data/com.dsemu.drastic/files/backup",
                "Android/data/com.dsemu.drastic/files/savestates"
            ),
            fileExtensions = listOf(".dsv", ".dss"),
            driveSubfolder = "DraStic"
        ),
        EmulatorProfile(
            id = "melonds",
            name = "MelonDS / DualDS / NooDS",
            system = "Nintendo DS",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "me.magnum.melonds",
                "me.magnum.melonds.dev",
                "me.magnum.melonds.nightly",
                "me.magnum.melondualds",
                "com.hydra.noods"
            ),
            candidatePaths = listOf(
                "Android/data/me.magnum.melonds/files",
                "melonDS"
            ),
            fileExtensions = listOf(".sav", ".mln"),
            driveSubfolder = "MelonDS"
        ),

        // Nintendo 64 (Mupen64Plus FZ / Pro / AE)
        EmulatorProfile(
            id = "mupen64plus",
            name = "Mupen64Plus FZ Edition",
            system = "Nintendo 64",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.mupen64plusae.v3.fzurita",
                "org.mupen64plusae.v3.fzurita.pro",
                "org.mupen64plusae.v3.alpha",
                "paulscode.android.mupen64plusae"
            ),
            candidatePaths = listOf(
                "Mupen64PlusFZ/GameSaves",
                "Android/data/org.mupen64plusae.v3.fzurita/files/GameSaves",
                "Android/data/org.mupen64plusae.v3.fzurita.pro/files/GameSaves"
            ),
            fileExtensions = listOf(".mpk", ".fla", ".sra", ".eep", ".st0", ".st1", ".st2"),
            driveSubfolder = "Mupen64"
        ),

        // Sega Saturn (YabaSanshiro 2 / Saturn.emu)
        EmulatorProfile(
            id = "yabasanshiro",
            name = "YabaSanshiro 2 / Saturn.EMU",
            system = "Sega Saturn",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "org.devmiyax.yabasanshioro2",
                "org.devmiyax.yabasanshioro2.pro",
                "org.uoyabause.android",
                "org.uoyabause.android.pro",
                "com.explusalpha.SaturnEmu"
            ),
            candidatePaths = listOf(
                "Android/data/org.devmiyax.yabasanshioro2/files",
                "Android/data/org.devmiyax.yabasanshioro2.pro/files",
                "yabasanshioro"
            ),
            fileExtensions = listOf(".bin", ".dat", ".sav"),
            driveSubfolder = "SegaSaturn"
        ),

        // Sega Dreamcast & Naomi (Flycast, Redream, Reicast)
        EmulatorProfile(
            id = "flycast",
            name = "Flycast",
            system = "Sega Dreamcast & Naomi",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.flycast.emulator", "com.flyinghead.Flycast"),
            candidatePaths = listOf(
                "Flycast/data",
                "Android/data/com.flycast.emulator/files/data",
                "Android/data/com.flyinghead.Flycast/files/data"
            ),
            fileExtensions = listOf(".bin", ".state", ".nvmem"),
            driveSubfolder = "Flycast"
        ),
        EmulatorProfile(
            id = "redream",
            name = "Redream / Reicast",
            system = "Sega Dreamcast",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("io.recompiled.redream", "com.reicast.emulator"),
            candidatePaths = listOf(
                "redream",
                "Android/data/io.recompiled.redream/files"
            ),
            fileExtensions = listOf(".bin", ".state"),
            driveSubfolder = "Redream"
        ),

        // Game Boy Advance & Color (Pizza Boy, MyBoy, SkyEmu, Linkboy)
        EmulatorProfile(
            id = "pizzaboy",
            name = "Pizza Boy GBA / GBC",
            system = "Game Boy Advance & Color",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "it.dbtecno.pizzaboygba",
                "it.dbtecno.pizzaboygbapro",
                "it.dbtecno.pizzaboypro",
                "it.dbtecno.pizzaboy",
                "it.dbtecno.pizzaboyscpro"
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
            packageNames = listOf(
                "com.fastemulator.gba",
                "com.fastemulator.gbafree",
                "com.fastemulator.gbc",
                "com.fastemulator.gbcfree",
                "com.pixelrespawn.linkboy",
                "com.sky.SkyEmu"
            ),
            candidatePaths = listOf("MyBoy/save", "MyOldBoy/save"),
            fileExtensions = listOf(".sav", ".st0", ".st1", ".st2"),
            driveSubfolder = "MyBoy"
        ),

        // Robert Broglia .EMU Suite (SNES, NES, MD, NeoGeo, Atari, MSX, C64)
        EmulatorProfile(
            id = "broglia_emu_suite",
            name = ".EMU Suite (Snes9x EX+, NES, MD, NeoGeo)",
            system = "Classic 8/16-Bit Consoles",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "com.explusalpha.Snes9xPlus",
                "com.explusalpha.NesEmu",
                "com.explusalpha.MdEmu",
                "com.explusalpha.GbaEmu",
                "com.explusalpha.GbcEmu",
                "com.explusalpha.NeoEmu",
                "com.explusalpha.neoemu",
                "com.explusalpha.NgpEmu",
                "com.explusalpha.SwanEmu",
                "com.explusalpha.LynxEmu",
                "com.explusalpha.A2600Emu",
                "com.explusalpha.C64Emu",
                "com.explusalpha.MsxEmu",
                "com.PceEmu",
                "com.androidemu.gens",
                "com.androidemu.nes",
                "com.androidemu.atari",
                "com.androidemu.gg",
                "com.fms.mg"
            ),
            candidatePaths = listOf(
                "Android/data/com.explusalpha.Snes9xPlus/files",
                "Android/data/com.explusalpha.MdEmu/files",
                "Android/data/com.explusalpha.NesEmu/files"
            ),
            fileExtensions = listOf(".sav", ".sta", ".srm"),
            driveSubfolder = "Broglia_Emu"
        ),

        // Microsoft Xbox & Xbox 360 (X1 BOX, hakuX, ax360e, X360 Mobile, xendroid)
        EmulatorProfile(
            id = "xbox_emulators",
            name = "Xbox & Xbox 360 (X1 BOX, ax360e)",
            system = "Xbox & Xbox 360",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "com.izzy2lost.x1box",
                "com.rfandango.haku_x",
                "aenu.ax360e",
                "aenu.ax360e.free",
                "emu.x360.mobile",
                "emu.x360mobile.com",
                "xendroid.compose"
            ),
            candidatePaths = listOf(
                "Android/data/com.izzy2lost.x1box/files",
                "Android/data/aenu.ax360e/files"
            ),
            fileExtensions = listOf(".bin", ".dat", ".sav"),
            driveSubfolder = "Xbox"
        ),

        // Windows & PC Emulation (Winlator, MiceWine, GameHub)
        EmulatorProfile(
            id = "winlator_pc",
            name = "Winlator / MiceWine (Windows PC)",
            system = "Windows PC",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf(
                "com.winlator",
                "com.winlator.cmod",
                "com.cmodded.winlator",
                "com.micewine.emu",
                "gamehub.lite",
                "emuready.gamehub.lite"
            ),
            candidatePaths = listOf(
                "Android/data/com.winlator/files/image/home",
                "Android/data/com.winlator/files",
                "Winlator"
            ),
            fileExtensions = listOf(".sav", ".dat", ".ini"),
            driveSubfolder = "Winlator_PC"
        ),

        // Arcade & MAME
        EmulatorProfile(
            id = "mame4droid",
            name = "MAME4droid (Arcade)",
            system = "Arcade (MAME)",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.seleuco.mame4droid", "com.seleuco.mame4d2024"),
            candidatePaths = listOf("MAME4droid/sta", "MAME4droid/nvram"),
            fileExtensions = listOf(".sta", ".nv"),
            driveSubfolder = "MAME"
        ),

        // Point & Click Adventure (ScummVM)
        EmulatorProfile(
            id = "scummvm",
            name = "ScummVM",
            system = "ScummVM Adventures",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("org.scummvm.scummvm", "org.scummvm.scummvm.debug"),
            candidatePaths = listOf(
                "ScummVM/saves",
                "Android/data/org.scummvm.scummvm/files/saves"
            ),
            fileExtensions = listOf(".s??", ".sav", ".0??"),
            driveSubfolder = "ScummVM"
        ),

        // Fantasy Console (PICO-8, Pixl8)
        EmulatorProfile(
            id = "pico8",
            name = "PICO-8 (Pixl8 / Infinity P8)",
            system = "PICO-8",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("be.codedreams.pixl8", "io.wip.pico8", "me.dt2dev.infinity"),
            candidatePaths = listOf("pico-8/cdata", "Android/data/be.codedreams.pixl8/files"),
            fileExtensions = listOf(".p8", ".png", ".txt"),
            driveSubfolder = "PICO8"
        ),

        // Virtual Boy (vvb)
        EmulatorProfile(
            id = "virtual_boy",
            name = "Virtual Boy (vvb)",
            system = "Nintendo Virtual Boy",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.simongellis.vvb"),
            candidatePaths = listOf("Android/data/com.simongellis.vvb/files/saves"),
            fileExtensions = listOf(".sav", ".srm"),
            driveSubfolder = "VirtualBoy"
        ),

        // Nokia N-Gage (eka2l1)
        EmulatorProfile(
            id = "nokia_ngage",
            name = "EKA2L1 (Nokia N-Gage)",
            system = "Nokia N-Gage & Symbian",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("com.github.eka2l1"),
            candidatePaths = listOf("EKA2L1/data/drives/c"),
            fileExtensions = listOf(".dat", ".bin"),
            driveSubfolder = "Ngage_EKA2L1"
        ),

        // Java Mobile (J2ME Loader)
        EmulatorProfile(
            id = "j2me_loader",
            name = "J2ME Loader",
            system = "Java ME (Mobile)",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("ru.playsoftware.j2meloader", "ru.woesss.j2meloader"),
            candidatePaths = listOf("j2meloader/rms", "Android/data/ru.playsoftware.j2meloader/files"),
            fileExtensions = listOf(".db", ".rms"),
            driveSubfolder = "J2ME"
        ),

        // Atari Jaguar (IrataJaguar)
        EmulatorProfile(
            id = "atari_jaguar",
            name = "IrataJaguar",
            system = "Atari Jaguar & CD",
            category = ProfileCategory.EMULATOR,
            packageNames = listOf("ru.vastness.altmer.iratajaguar"),
            candidatePaths = listOf("IrataJaguar/saves", "Android/data/ru.vastness.altmer.iratajaguar/files"),
            fileExtensions = listOf(".eep", ".sav"),
            driveSubfolder = "AtariJaguar"
        ),

        // Recompiled Projects & Native Source Ports
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
            packageNames = listOf("com.portmaster.launcher", "org.force9.starboard"),
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
