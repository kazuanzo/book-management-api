package com.example.bookmanagement.author

import com.example.bookmanagement.book.BookResponse
import com.example.bookmanagement.book.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/authors")
class AuthorController(
    private val authorService: AuthorService,
    private val bookService: BookService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: AuthorRequest,
    ): AuthorResponse = authorService.create(request)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: AuthorRequest,
    ): AuthorResponse = authorService.update(id, request)

    @GetMapping("/{id}/books")
    fun books(
        @PathVariable id: Long,
    ): List<BookResponse> = bookService.findByAuthor(id)
}
