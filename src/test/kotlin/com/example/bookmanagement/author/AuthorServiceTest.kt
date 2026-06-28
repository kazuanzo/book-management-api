package com.example.bookmanagement.author

import com.example.bookmanagement.common.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDate

class AuthorServiceTest {
    private val authorRepository = mock(AuthorRepository::class.java)
    private val service = AuthorService(authorRepository)

    @Test
    fun `更新は著者が存在すればレスポンスを返す`() {
        val birthDate = LocalDate.of(1990, 1, 1)
        given(authorRepository.update(1L, "Robert", birthDate)).willReturn(true)

        val response = service.update(1L, AuthorRequest("Robert", birthDate))

        assertEquals(1L, response.id)
        assertEquals("Robert", response.name)
        assertEquals(birthDate, response.birthDate)
    }

    @Test
    fun `更新は著者が存在しなければ失敗する`() {
        val birthDate = LocalDate.of(1990, 1, 1)
        given(authorRepository.update(99L, "Robert", birthDate)).willReturn(false)

        assertThrows<ResourceNotFoundException> {
            service.update(99L, AuthorRequest("Robert", birthDate))
        }
    }
}
