package com.example.bookmanagement.book

import com.example.bookmanagement.author.AuthorRepository
import com.example.bookmanagement.domain.PublicationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

// 実DB(ローカルはdocker compose / CIはpostgresサービス)に対する結合テスト。
// クラスに @Transactional を付け、各テストはトランザクションごとロールバックされる（後始末不要）。
@SpringBootTest
@Transactional
class BookRepositoryTest(
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val authorRepository: AuthorRepository,
) {
    private fun newAuthor(name: String = "Author"): Long = authorRepository.insert(name, LocalDate.of(1990, 1, 1))

    @Test
    fun `insertで保存しfindByIdで読み戻せる`() {
        val bookId = bookRepository.insert("Clean Code", 3000, PublicationStatus.UNPUBLISHED)

        val book = bookRepository.findById(bookId)

        assertEquals(bookId, book?.id)
        assertEquals("Clean Code", book?.title)
        assertEquals(3000, book?.price)
        assertEquals(PublicationStatus.UNPUBLISHED, book?.publicationStatus)
        // 著者未紐付けの書籍は、authorIds が null でも例外でもなく空リストで返ることを保証する。
        assertTrue(book?.authorIds?.isEmpty() ?: false)
    }

    @Test
    fun `findByIdは存在しない書籍にnullを返す`() {
        assertNull(bookRepository.findById(-1L))
    }

    @Test
    fun `updateは項目を更新しtrueを返す`() {
        val bookId = bookRepository.insert("Old", 100, PublicationStatus.UNPUBLISHED)

        val updated = bookRepository.update(bookId, "New", 200, PublicationStatus.PUBLISHED)

        assertTrue(updated)
        val book = bookRepository.findById(bookId)
        assertEquals("New", book?.title)
        assertEquals(200, book?.price)
        assertEquals(PublicationStatus.PUBLISHED, book?.publicationStatus)
    }

    @Test
    fun `updateは存在しない書籍にfalseを返す`() {
        assertFalse(bookRepository.update(-1L, "X", 0, PublicationStatus.UNPUBLISHED))
    }

    @Test
    fun `replaceAuthorsは著者の紐付けを設定し差し替える`() {
        val bookId = bookRepository.insert("Book", 100, PublicationStatus.UNPUBLISHED)
        val a1 = newAuthor("A1")
        val a2 = newAuthor("A2")
        val a3 = newAuthor("A3")

        bookRepository.replaceAuthors(bookId, listOf(a1, a2))
        assertEquals(setOf(a1, a2), bookRepository.findById(bookId)?.authorIds?.toSet())

        bookRepository.replaceAuthors(bookId, listOf(a3))
        assertEquals(setOf(a3), bookRepository.findById(bookId)?.authorIds?.toSet())
    }

    @Test
    fun `replaceAuthorsは空配列で紐付けを全解除する`() {
        val bookId = bookRepository.insert("Book", 100, PublicationStatus.UNPUBLISHED)
        bookRepository.replaceAuthors(bookId, listOf(newAuthor()))

        // batch(emptyList()) が例外なく全解除として成立することを実DBで保証する。
        bookRepository.replaceAuthors(bookId, emptyList())

        assertTrue(bookRepository.findById(bookId)?.authorIds?.isEmpty() ?: false)
    }

    @Test
    fun `findByAuthorIdは著者の書籍を全著者idつきで返す`() {
        val a1 = newAuthor("A1")
        val a2 = newAuthor("A2")
        val bookId = bookRepository.insert("Co-authored", 100, PublicationStatus.PUBLISHED)
        bookRepository.replaceAuthors(bookId, listOf(a1, a2))

        val books = bookRepository.findByAuthorId(a1)

        assertEquals(1, books.size)
        assertEquals(bookId, books[0].id)
        assertEquals(listOf(a1, a2), books[0].authorIds.sorted())
    }

    @Test
    fun `findByAuthorIdは著者が書いた複数冊をすべて返す`() {
        val author = newAuthor()
        val book1 = bookRepository.insert("Book1", 100, PublicationStatus.UNPUBLISHED)
        val book2 = bookRepository.insert("Book2", 200, PublicationStatus.PUBLISHED)
        bookRepository.replaceAuthors(book1, listOf(author))
        bookRepository.replaceAuthors(book2, listOf(author))

        val books = bookRepository.findByAuthorId(author)

        // 件数も見ることで、結合で行が重複して返るバグ（Set化では素通りする）を拾う。
        assertEquals(2, books.size)
        assertEquals(setOf(book1, book2), books.map { it.id }.toSet())
    }

    @Test
    fun `findByAuthorIdは書籍がなければ空を返す`() {
        val authorId = newAuthor("Lonely")
        assertTrue(bookRepository.findByAuthorId(authorId).isEmpty())
    }
}
