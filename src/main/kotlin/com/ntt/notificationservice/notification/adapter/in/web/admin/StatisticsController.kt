package com.ntt.notificationservice.notification.adapter.`in`.web.admin

import com.ntt.notificationservice.notification.application.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Admin API for notification statistics and reporting.
 */
@RestController
@RequestMapping("/api/v1/notifications/stats")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping
    fun getStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
        @RequestParam(required = false) channel: String?
    ): ResponseEntity<StatisticsService.StatsResponse> {
        return ResponseEntity.ok(statisticsService.getStats(from, to, channel))
    }

    @GetMapping("/retry")
    fun getRetryReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant
    ): ResponseEntity<StatisticsService.RetryReportResponse> {
        return ResponseEntity.ok(statisticsService.getRetryReport(from, to))
    }
}
