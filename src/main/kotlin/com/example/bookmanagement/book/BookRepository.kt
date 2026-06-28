package com.example.bookmanagement.book

import com.example.bookmanagement.domain.Book
import com.example.bookmanagement.domain.PublicationStatus
import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.jooq.tables.references.BOOKS
import com.example.bookmanagement.jooq.tables.references.BOOK_AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BookRepository(
    private val dsl: DSLContext,
) {
    fun insert(
        title: String,
        price: Int,
        status: PublicationStatus,
    ): Long =
        dsl
            .insertInto(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, status)
            .returning(BOOKS.ID)
            .fetchSingle()
            .id!!

    fun update(
        id: Long,
        title: String,
        price: Int,
        status: PublicationStatus,
    ): Boolean =
        dsl
            .update(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, status)
            .set(BOOKS.UPDATED_AT, LocalDateTime.now())
            .where(BOOKS.ID.eq(id))
            .execute() > 0

    fun findById(id: Long): Book? {
        val record = dsl.selectFrom(BOOKS).where(BOOKS.ID.eq(id)).fetchOne() ?: return null
        return record.toBook(findAuthorIds(id))
    }

    fun replaceAuthors(
        bookId: Long,
        authorIds: List<Long>,
    ) {
        // delete と insert群の原子性は呼び出し側(BookService)の @Transactional が担保する。
        dsl.deleteFrom(BOOK_AUTHORS).where(BOOK_AUTHORS.BOOK_ID.eq(bookId)).execute()
        val inserts =
            authorIds.distinct().map { authorId ->
                dsl
                    .insertInto(BOOK_AUTHORS)
                    .set(BOOK_AUTHORS.BOOK_ID, bookId)
                    .set(BOOK_AUTHORS.AUTHOR_ID, authorId)
            }
        dsl.batch(inserts).execute()
    }

    fun findByAuthorId(authorId: Long): List<Book> {
        val bookIds =
            dsl
                .select(BOOK_AUTHORS.BOOK_ID)
                .from(BOOK_AUTHORS)
                .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
                .fetch(BOOK_AUTHORS.BOOK_ID)
        if (bookIds.isEmpty()) {
            return emptyList()
        }
        val authorsByBook =
            dsl
                .select(BOOK_AUTHORS.BOOK_ID, BOOK_AUTHORS.AUTHOR_ID)
                .from(BOOK_AUTHORS)
                .where(BOOK_AUTHORS.BOOK_ID.`in`(bookIds))
                .fetchGroups(BOOK_AUTHORS.BOOK_ID, BOOK_AUTHORS.AUTHOR_ID)
        return dsl
            .selectFrom(BOOKS)
            .where(BOOKS.ID.`in`(bookIds))
            .fetch()
            .map { it.toBook(authorsByBook[it.id]?.filterNotNull() ?: emptyList()) }
    }

    private fun findAuthorIds(bookId: Long): List<Long> =
        dsl
            .select(BOOK_AUTHORS.AUTHOR_ID)
            .from(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .fetch(BOOK_AUTHORS.AUTHOR_ID)
            // author_id は NOT NULL。fetch(Field) が List<Long?> を返す型都合の除去（null混入対策ではない）。
            .filterNotNull()

    private fun BooksRecord.toBook(authorIds: List<Long>): Book =
        Book(
            id = id!!,
            title = title,
            price = price,
            publicationStatus = publicationStatus,
            authorIds = authorIds,
        )
}
