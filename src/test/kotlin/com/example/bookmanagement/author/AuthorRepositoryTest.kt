package com.example.bookmanagement.author

import com.example.bookmanagement.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

// 実DBに対する結合テスト。@Transactional で各テストはロールバックされる。
// 著者は本番に findById を持たない（GET著者APIが不要）ため、保存内容の検証は DSLContext で AUTHORS を直接読む。
@SpringBootTest
@Transactional
class AuthorRepositoryTest(
    @Autowired private val authorRepository: AuthorRepository,
    @Autowired private val dsl: DSLContext,
) {
    @Test
    fun `insertは著者を内容ごと保存する`() {
        val id = authorRepository.insert("Robert", LocalDate.of(1990, 1, 1))

        val saved = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(id)).fetchSingle()
        assertEquals("Robert", saved.name)
        assertEquals(LocalDate.of(1990, 1, 1), saved.birthDate)
    }

    @Test
    fun `existsByIdは保存済みをtrue・未保存をfalseで判定する`() {
        val id = authorRepository.insert("Robert", LocalDate.of(1990, 1, 1))

        assertTrue(authorRepository.existsById(id))
        assertFalse(authorRepository.existsById(-1L))
    }

    @Test
    fun `updateは著者情報を更新する`() {
        val id = authorRepository.insert("Old", LocalDate.of(1990, 1, 1))

        authorRepository.update(id, "New", LocalDate.of(1985, 5, 5))

        val saved = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(id)).fetchSingle()
        assertEquals("New", saved.name)
        assertEquals(LocalDate.of(1985, 5, 5), saved.birthDate)
    }

    @Test
    fun `updateは更新件数に応じて結果を返す`() {
        val id = authorRepository.insert("Old", LocalDate.of(1990, 1, 1))

        assertTrue(authorRepository.update(id, "New", LocalDate.of(1985, 5, 5)))
        assertFalse(authorRepository.update(-1L, "X", LocalDate.of(1985, 5, 5)))
    }

    @Test
    fun `findExistingIdsは実在するidのみを返す`() {
        val id1 = authorRepository.insert("A", LocalDate.of(1990, 1, 1))
        val id2 = authorRepository.insert("B", LocalDate.of(1990, 1, 1))

        val result = authorRepository.findExistingIds(listOf(id1, id2, -1L))

        assertEquals(setOf(id1, id2), result)
    }

    @Test
    fun `findExistingIdsは空入力に空集合を返す`() {
        assertEquals(emptySet<Long>(), authorRepository.findExistingIds(emptyList()))
    }
}
