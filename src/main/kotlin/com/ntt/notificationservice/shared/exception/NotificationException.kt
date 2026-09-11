package com.ntt.notificationservice.shared.exception

import com.ntt.basecore.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * Base exception for all notification service errors.
 * Bridge pattern: extends BusinessException (base-core) while preserving
 * ProblemDetail response and per-exception HTTP status.
 */
open class NotificationException(
    val errorCode: NotificationErrorCode,
    override val message: String = errorCode.toErrorCodeBase().getDesc() ?: "",
    val httpStatus: HttpStatus = errorCode.httpStatus
) : BusinessException(errorCode.toErrorCodeBase())
