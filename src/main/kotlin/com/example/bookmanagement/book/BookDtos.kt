package com.example.bookmanagement.book

import com.example.bookmanagement.domain.Book
import com.example.bookmanagement.domain.PublicationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class BookRequest(
    @field:NotBlank
    val title: String,
    @field:Min(0)
    val price: Int,
    val publicationStatus: PublicationStatus,
    @field:NotEmpty
    val authorIds: List<Long>,
)

data class BookResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authorIds: List<Long>,
) {
    companion object {
        fun from(book: Book): BookResponse = BookResponse(book.id, book.title, book.price, book.publicationStatus, book.authorIds)
    }
}
