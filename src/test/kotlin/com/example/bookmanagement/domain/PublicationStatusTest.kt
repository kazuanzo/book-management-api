package com.example.bookmanagement.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublicationStatusTest {
    @Test
    fun `未出版は出版済みに遷移できる`() {
        assertTrue(PublicationStatus.UNPUBLISHED.canTransitionTo(PublicationStatus.PUBLISHED))
    }

    @Test
    fun `出版済みは未出版に戻せない`() {
        assertFalse(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.UNPUBLISHED))
    }

    @Test
    fun `同一ステータスへの遷移は許可される`() {
        assertTrue(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.PUBLISHED))
        assertTrue(PublicationStatus.UNPUBLISHED.canTransitionTo(PublicationStatus.UNPUBLISHED))
    }
}
