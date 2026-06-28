package com.example.bookmanagement.common

/** 対象リソースが存在しない（404）。 */
class ResourceNotFoundException(
    message: String,
) : RuntimeException(message)

/** 入力は形式的には妥当だが業務上受け付けられない（400）。 */
class BadRequestException(
    message: String,
) : RuntimeException(message)

/** 出版状況の不正な状態遷移（409）。 */
class InvalidStatusTransitionException(
    message: String,
) : RuntimeException(message)
