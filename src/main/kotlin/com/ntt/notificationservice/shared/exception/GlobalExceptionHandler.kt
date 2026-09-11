package com.ntt.notificationservice.shared.exception

import com.ntt.basecore.controller.BaseControllerAdvice
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Global exception handler — extends BaseControllerAdvice (base-core).
 * Converts NotificationException to ProblemDetail RFC 7807 response.
 */
@ControllerAdvice
class GlobalExceptionHandler : BaseControllerAdvice() {

    /**
     * Handle NotificationException — notification-specific error responses.
     */
    @ExceptionHandler(NotificationException::class)
    fun handleNotificationException(ex: NotificationException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(
            ex.httpStatus,
            ex.message
        )
        problemDetail.title = ex.errorCode.name
        problemDetail.setProperty("errorCode", ex.errorCode.toErrorCodeBase().getCode())
        return ResponseEntity.status(ex.httpStatus).body(problemDetail)
    }
}
