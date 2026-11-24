package com.example.documate.core.common

/**
 * A sealed hierarchy representing the outcome of an operation.
 * Domain and repository layers emit AppResult to isolate error details
 * and pass safe, user-friendly messages to the presentation layer.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
