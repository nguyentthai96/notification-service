package com.ntt.notification.client

/**
 * Notification port interface — used by producer services (auth-service, account-service, etc.)
 * to enqueue notifications within their business transactions.
 *
 * Usage:
 * ```
 * @Service
 * class MyService(private val notificationPort: NotificationPort) {
 *     @Transactional
 *     fun doSomething() {
 *         // ... business logic ...
 *         notificationPort.enqueue(NotificationRequest(
 *             recipient = "user@example.com",
 *             templateCode = "NEW_DEVICE_LOGIN",
 *             templateData = mapOf("userName" to "John")
 *         ))
 *     }
 * }
 * ```
 */
interface NotificationPort {

    /**
     * Enqueue a notification within the current transaction.
     * @param request Notification details
     * @return Notification ID (Snowflake)
     */
    fun enqueue(request: NotificationRequest): Long
}
