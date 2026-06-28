package com.example.bookmanagement.author

import com.example.bookmanagement.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class AuthorRepository(
    private val dsl: DSLContext,
) {
    fun insert(
        name: String,
        birthDate: LocalDate,
    ): Long =
        dsl
            .insertInto(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .returning(AUTHORS.ID)
            .fetchSingle()
            .id!!

    fun update(
        id: Long,
        name: String,
        birthDate: LocalDate,
    ): Boolean =
        dsl
            .update(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .set(AUTHORS.UPDATED_AT, LocalDateTime.now())
            .where(AUTHORS.ID.eq(id))
            .execute() > 0

    fun existsById(id: Long): Boolean = dsl.fetchExists(dsl.selectOne().from(AUTHORS).where(AUTHORS.ID.eq(id)))

    fun findExistingIds(ids: Collection<Long>): Set<Long> =
        dsl
            .select(AUTHORS.ID)
            .from(AUTHORS)
            .where(AUTHORS.ID.`in`(ids))
            .fetch(AUTHORS.ID)
            // id は NOT NULL。fetch(Field) が List<Long?> を返す型都合の除去（null混入対策ではない）。
            .filterNotNull()
            .toSet()
}
