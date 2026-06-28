package com.example.bookmanagement.author

import com.example.bookmanagement.common.ResourceNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthorService(
    private val authorRepository: AuthorRepository,
) {
    fun create(request: AuthorRequest): AuthorResponse {
        val id = authorRepository.insert(request.name, request.birthDate)
        return AuthorResponse(id, request.name, request.birthDate)
    }

    fun update(
        id: Long,
        request: AuthorRequest,
    ): AuthorResponse {
        if (!authorRepository.update(id, request.name, request.birthDate)) {
            throw ResourceNotFoundException("Author $id not found")
        }
        return AuthorResponse(id, request.name, request.birthDate)
    }
}
