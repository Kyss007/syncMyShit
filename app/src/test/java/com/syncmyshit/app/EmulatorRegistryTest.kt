package com.syncmyshit.app

import com.syncmyshit.app.data.local.EmulatorRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorRegistryTest {

    @Test
    fun testBuiltInProfilesNotEmpty() {
        val profiles = EmulatorRegistry.BUILT_IN_PROFILES
        assertTrue("Profiles list should not be empty", profiles.isNotEmpty())
        assertTrue("Should support at least 15 emulation systems out of the box", profiles.size >= 15)
    }

    @Test
    fun testUniqueProfileIds() {
        val profiles = EmulatorRegistry.BUILT_IN_PROFILES
        val ids = profiles.map { it.id }
        val distinctIds = ids.distinct()
        assertTrue("All profile IDs must be unique", ids.size == distinctIds.size)
    }

    @Test
    fun testDriveSubfoldersAreValid() {
        val profiles = EmulatorRegistry.BUILT_IN_PROFILES
        for (profile in profiles) {
            assertFalse("Drive subfolder must not be empty", profile.driveSubfolder.isBlank())
            assertFalse("Drive subfolder must not contain slash", profile.driveSubfolder.contains("/"))
        }
    }

    @Test
    fun testRecompProfilesExist() {
        val profiles = EmulatorRegistry.BUILT_IN_PROFILES
        val recomp = profiles.filter { it.category.name == "RECOMP" }
        assertTrue("Should include recomp projects", recomp.isNotEmpty())
        assertTrue("Should include Zelda 64 recomp", recomp.any { it.id.contains("zelda") })
    }

    @Test
    fun testCocoonFeSystemsCovered() {
        val profiles = EmulatorRegistry.BUILT_IN_PROFILES
        val allPackages = profiles.flatMap { it.packageNames }
        // Verify key CocoonFE emulator packages are covered
        assertTrue(allPackages.contains("info.cemu.cemu")) // CEMU Wii U
        assertTrue(allPackages.contains("org.devmiyax.yabasanshioro2")) // Saturn
        assertTrue(allPackages.contains("com.winlator")) // Winlator PC
        assertTrue(allPackages.contains("com.izzy2lost.x1box")) // Xbox
        assertTrue(allPackages.contains("com.fastemulator.gba")) // MyBoy
        assertTrue(allPackages.contains("it.dbtecno.pizzaboygba")) // PizzaBoy
        assertTrue(allPackages.contains("org.scummvm.scummvm")) // ScummVM
    }
}
