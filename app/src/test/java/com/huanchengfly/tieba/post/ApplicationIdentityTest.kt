package com.huanchengfly.tieba.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ApplicationIdentityTest {
    @Test
    fun installationIdIsIndependentFromTheOriginalApp() {
        assertEquals("io.github.miraceo.kejianba", BuildConfig.APPLICATION_ID)
        assertNotEquals("com.huanchengfly.tieba.post", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun generatedCodeKeepsTheOriginalNamespace() {
        assertEquals("com.huanchengfly.tieba.post", BuildConfig::class.java.`package`.name)
    }
}
