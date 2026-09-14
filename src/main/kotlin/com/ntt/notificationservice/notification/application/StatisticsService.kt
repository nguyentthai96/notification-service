package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationDlqRepository
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Statistics service — aggregate delivery metrics using native SQL.
 */
@Service
class StatisticsService(
    private val queueRepository: NotificationQueueRepository,
    private val dlqRepository: NotificationDlqRepository
) {

    data class ChannelStats(
        val channel: String,
        val status: String,
        val count: Long
    )

    data class StatsResponse(
        val from: Instant,
        val to: Instant,
        val breakdown: List<ChannelStats>,
        val totalSent: Long,
        val totalFailed: Long,
        val totalDelivered: Long,
        val dlqPending: Long
    )

    data class RetryReportEntry(
        val channel: String,
        val totalRetries: Long,
        val failedCount: Long,
        val topError: String?
    )

    data class RetryReportResponse(
        val from: Instant,
        val to: Instant,
        val entries: List<RetryReportEntry>,
        val dlqPending: Long
    )

    fun getStats(from: Instant, to: Instant, channel: String?): StatsResponse {
        val rawStats = queueRepository.findStatsByChannelAndDateRange(from, to, channel)
        val breakdown = rawStats.map { row ->
            ChannelStats(
                channel = row[0] as String,
                status = row[1] as String,
                count = (row[2] as Number).toLong()
            )
        }

        val totalSent = breakdown.filter { it.status in listOf("SENT", "DELIVERED", "READ") }.sumOf { it.count }
        val totalFailed = breakdown.filter { it.status == "FAILED" }.sumOf { it.count }
        val totalDelivered = breakdown.filter { it.status in listOf("DELIVERED", "READ") }.sumOf { it.count }
        val dlqPending = dlqRepository.countByResolved(false)

        return StatsResponse(
            from = from,
            to = to,
            breakdown = breakdown,
            totalSent = totalSent,
            totalFailed = totalFailed,
            totalDelivered = totalDelivered,
            dlqPending = dlqPending
        )
    }

    fun getRetryReport(from: Instant, to: Instant): RetryReportResponse {
        val rawStats = queueRepository.findRetryStatsByDateRange(from, to)
        val entries = rawStats.map { row ->
            RetryReportEntry(
                channel = row[0] as String,
                totalRetries = (row[1] as Number).toLong(),
                failedCount = (row[2] as Number).toLong(),
                topError = row[3] as? String
            )
        }

        return RetryReportResponse(
            from = from,
            to = to,
            entries = entries,
            dlqPending = dlqRepository.countByResolved(false)
        )
    }
}
