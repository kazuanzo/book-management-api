package com.example.bookmanagement.book

import com.example.bookmanagement.author.AuthorRepository
import com.example.bookmanagement.common.BadRequestException
import com.example.bookmanagement.common.InvalidStatusTransitionException
import com.example.bookmanagement.common.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
) {
    @Transactional
    fun create(request: BookRequest): BookResponse {
        val authorIds = request.authorIds.distinct()
        verifyAuthorsExist(authorIds)
        val id = bookRepository.insert(request.title, request.price, request.publicationStatus)
        bookRepository.replaceAuthors(id, authorIds)
        return BookResponse(id, request.title, request.price, request.publicationStatus, authorIds)
    }

    @Transactional
    fun update(
        id: Long,
        request: BookRequest,
    ): BookResponse {
        val current = bookRepository.findById(id) ?: throw ResourceNotFoundException("Book $id not found")
        if (!current.publicationStatus.canTransitionTo(request.publicationStatus)) {
            throw InvalidStatusTransitionException(
                "Cannot change publication status from ${current.publicationStatus} to ${request.publicationStatus}",
            )
        }
        val authorIds = request.authorIds.distinct()
        verifyAuthorsExist(authorIds)
        if (!bookRepository.update(id, request.title, request.price, request.publicationStatus)) {
            throw ResourceNotFoundException("Book $id not found")
        }
        bookRepository.replaceAuthors(id, authorIds)
        return BookResponse(id, request.title, request.price, request.publicationStatus, authorIds)
    }

    @Transactional(readOnly = true)
    fun findByAuthor(authorId: Long): List<BookResponse> {
        if (!authorRepository.existsById(authorId)) {
            throw ResourceNotFoundException("Author $authorId not found")
        }
        return bookRepository.findByAuthorId(authorId).map(BookResponse::from)
    }

    private fun verifyAuthorsExist(authorIds: List<Long>) {
        val existing = authorRepository.findExistingIds(authorIds)
        val missing = authorIds.filterNot { it in existing }
        if (missing.isNotEmpty()) {
            throw BadRequestException("Authors not found: $missing")
        }
    }
}
