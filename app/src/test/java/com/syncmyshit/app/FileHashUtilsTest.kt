package com.syncmyshit.app

import com.syncmyshit.app.utils.FileHashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileHashUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSha256Calculation() {
        val file = tempFolder.newFile("test_save.srm")
        file.writeText("POKEMON_EMERALD_SAVE_DATA_BLOCK_12345")

        val hash1 = FileHashUtils.calculateSha256(file)
        val hash2 = FileHashUtils.calculateSha256(file)

        assertEquals("Hash should be deterministic", hash1, hash2)
        assertNotEquals("Hash should not be empty", "", hash1)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testMd5Calculation() {
        val file = tempFolder.newFile("test_save.sav")
        file.writeText("ZELDA_SAVE_FILE_CONTENT")

        val md5 = FileHashUtils.calculateMd5(file)
        assertNotEquals("", md5)
        assertEquals(32, md5.length)
    }

    @Test
    fun testNonExistentFile() {
        val nonExistent = File(tempFolder.root, "does_not_exist.bin")
        val hash = FileHashUtils.calculateSha256(nonExistent)
        assertEquals("", hash)
    }
}
