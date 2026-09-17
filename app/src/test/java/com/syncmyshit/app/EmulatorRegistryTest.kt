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
}
