package com.talania.personalhub.core.model

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

sealed interface DomainError {
    data object Validation : DomainError
    data object NotFound : DomainError
    data object Conflict : DomainError
    data object Storage : DomainError
    data object PermissionRequired : DomainError
}
