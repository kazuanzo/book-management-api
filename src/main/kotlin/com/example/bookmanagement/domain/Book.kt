package com.example.bookmanagement.domain

data class Book(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authorIds: List<Long>,
)
