package com.example.bookmanagement.domain

enum class PublicationStatus {
    UNPUBLISHED,
    PUBLISHED,
    ;

    /** 出版済みから未出版へは戻せない。それ以外の遷移は許可する。 */
    fun canTransitionTo(next: PublicationStatus): Boolean =
        when (this) {
            UNPUBLISHED -> true
            PUBLISHED -> next == PUBLISHED
        }
}
