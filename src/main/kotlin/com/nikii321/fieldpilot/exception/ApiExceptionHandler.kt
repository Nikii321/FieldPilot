package com.nikii321.fieldpilot.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class, HttpMessageNotReadableException::class)
    fun bad(e: Exception) =
        ResponseEntity.status(400)
            .body(
                mapOf(
                    "error" to
                        (if (e is HttpMessageNotReadableException) "Некорректный JSON или тип поля"
                        else e.message ?: "Неверный запрос"),
                ),
            )

    @ExceptionHandler(NotFoundException::class)
    fun missing(e: Exception) = ResponseEntity.status(404).body(mapOf("error" to e.message))

    @ExceptionHandler(ConflictException::class, DataIntegrityViolationException::class)
    fun conflict(e: Exception) =
        ResponseEntity.status(409)
            .body(
                mapOf(
                    "error" to
                        if (e is DataIntegrityViolationException)
                            "Конфликт: имя уже занято или объект используется"
                        else e.message,
                ),
            )

    @ExceptionHandler(InfeasiblePlanException::class)
    fun infeasible(e: Exception) = ResponseEntity.status(422).body(mapOf("error" to e.message))
}
