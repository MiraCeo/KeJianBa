package com.huanchengfly.tieba.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationIdentityTest {
    @Test
    fun installationIdIsIndependentFromTheOriginalApp() {
        // Build types may append a suffix (.debug/.ci/.benchmark), so match the base id.
        assertTrue(BuildConfig.APPLICATION_ID.startsWith("io.github.miraceo.kejianba"))
        assertNotEquals("com.huanchengfly.tieba.post", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun generatedCodeKeepsTheOriginalNamespace() {
        assertEquals("com.huanchengfly.tieba.post", BuildConfig::class.java.`package`.name)
    }
}
