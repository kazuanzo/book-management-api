package com.example.bookmanagement.book

import com.example.bookmanagement.author.AuthorRepository
import com.example.bookmanagement.common.BadRequestException
import com.example.bookmanagement.common.InvalidStatusTransitionException
import com.example.bookmanagement.common.ResourceNotFoundException
import com.example.bookmanagement.domain.Book
import com.example.bookmanagement.domain.PublicationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BookServiceTest {
    private val bookRepository = mock(BookRepository::class.java)
    private val authorRepository = mock(AuthorRepository::class.java)
    private val service = BookService(bookRepository, authorRepository)

    @Test
    fun `登録は著者が一人でも存在しなければ失敗する`() {
        val request = BookRequest("X", 0, PublicationStatus.UNPUBLISHED, listOf(1L, 2L))
        given(authorRepository.findExistingIds(listOf(1L, 2L))).willReturn(setOf(1L))

        assertThrows<BadRequestException> { service.create(request) }
    }

    @Test
    fun `登録は著者idの重複を排除する`() {
        val request = BookRequest("X", 0, PublicationStatus.UNPUBLISHED, listOf(1L, 1L, 2L))
        given(authorRepository.findExistingIds(listOf(1L, 2L))).willReturn(setOf(1L, 2L))
        given(bookRepository.insert("X", 0, PublicationStatus.UNPUBLISHED)).willReturn(10L)

        val response = service.create(request)

        assertEquals(10L, response.id)
        assertEquals("X", response.title)
        assertEquals(0, response.price)
        assertEquals(PublicationStatus.UNPUBLISHED, response.publicationStatus)
        assertEquals(listOf(1L, 2L), response.authorIds)
        // 重複排除した著者idが、レスポンスのエコーだけでなく永続化(replaceAuthors)にも渡ることを保証する。
        verify(bookRepository).replaceAuthors(10L, listOf(1L, 2L))
    }

    @Test
    fun `更新は許可された状態遷移なら成功する`() {
        val request = BookRequest("X", 100, PublicationStatus.PUBLISHED, listOf(1L))
        given(bookRepository.findById(1L)).willReturn(Book(1L, "X", 100, PublicationStatus.UNPUBLISHED, listOf(1L)))
        given(authorRepository.findExistingIds(listOf(1L))).willReturn(setOf(1L))
        given(bookRepository.update(1L, "X", 100, PublicationStatus.PUBLISHED)).willReturn(true)

        val response = service.update(1L, request)

        assertEquals(1L, response.id)
        assertEquals("X", response.title)
        assertEquals(100, response.price)
        assertEquals(PublicationStatus.PUBLISHED, response.publicationStatus)
        assertEquals(listOf(1L), response.authorIds)
        // 更新時も著者紐付けの貼り替え(replaceAuthors)が実行されることを保証する。
        verify(bookRepository).replaceAuthors(1L, listOf(1L))
    }

    @Test
    fun `更新は著者が一人でも存在しなければ失敗する`() {
        val request = BookRequest("X", 100, PublicationStatus.PUBLISHED, listOf(1L))
        given(bookRepository.findById(1L)).willReturn(Book(1L, "X", 100, PublicationStatus.UNPUBLISHED, listOf(1L)))
        given(authorRepository.findExistingIds(listOf(1L))).willReturn(emptySet())

        assertThrows<BadRequestException> { service.update(1L, request) }
    }

    @Test
    fun `更新は更新直前に書籍が消えていれば失敗する`() {
        val request = BookRequest("X", 100, PublicationStatus.PUBLISHED, listOf(1L))
        given(bookRepository.findById(1L)).willReturn(Book(1L, "X", 100, PublicationStatus.UNPUBLISHED, listOf(1L)))
        given(authorRepository.findExistingIds(listOf(1L))).willReturn(setOf(1L))
        given(bookRepository.update(1L, "X", 100, PublicationStatus.PUBLISHED)).willReturn(false)

        assertThrows<ResourceNotFoundException> { service.update(1L, request) }
    }

    @Test
    fun `著者の書籍取得は著者が存在しなければ失敗する`() {
        given(authorRepository.existsById(99L)).willReturn(false)

        assertThrows<ResourceNotFoundException> { service.findByAuthor(99L) }
    }

    @Test
    fun `著者の書籍取得は著者の本を返す`() {
        val book = Book(10L, "X", 100, PublicationStatus.PUBLISHED, listOf(1L))
        given(authorRepository.existsById(1L)).willReturn(true)
        given(bookRepository.findByAuthorId(1L)).willReturn(listOf(book))

        val result = service.findByAuthor(1L)

        assertEquals(1, result.size)
        assertEquals(10L, result[0].id)
    }

    @Test
    fun `更新は出版済みから未出版への遷移を拒否する`() {
        val request = BookRequest("X", 100, PublicationStatus.UNPUBLISHED, listOf(1L))
        given(bookRepository.findById(1L)).willReturn(Book(1L, "X", 100, PublicationStatus.PUBLISHED, listOf(1L)))

        assertThrows<InvalidStatusTransitionException> { service.update(1L, request) }
    }

    @Test
    fun `更新は書籍が存在しなければ失敗する`() {
        val request = BookRequest("X", 100, PublicationStatus.PUBLISHED, listOf(1L))
        given(bookRepository.findById(99L)).willReturn(null)

        assertThrows<ResourceNotFoundException> { service.update(99L, request) }
    }
}
