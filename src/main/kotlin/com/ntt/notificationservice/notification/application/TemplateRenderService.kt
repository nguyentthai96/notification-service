package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationTemplateEntity
import org.springframework.stereotype.Service

/**
 * Template render service — substitutes {{key}} placeholders in templates.
 * Pure function: no side effects, no database access.
 */
@Service
class TemplateRenderService {

    companion object {
        private val PLACEHOLDER_REGEX = Regex("""\{\{(\w+)}}""")
    }

    /**
     * Render subject template with data.
     */
    fun renderSubject(template: NotificationTemplateEntity, data: Map<String, Any>): String {
        return render(template.subjectTemplate, data)
    }

    /**
     * Render body template with data.
     */
    fun renderBody(template: NotificationTemplateEntity, data: Map<String, Any>): String {
        return render(template.bodyTemplate, data)
    }

    /**
     * Replace all {{key}} placeholders with values from data map.
     * Unmatched placeholders are left as-is.
     */
    private fun render(templateText: String, data: Map<String, Any>): String {
        return PLACEHOLDER_REGEX.replace(templateText) { match ->
            val key = match.groupValues[1]
            data[key]?.toString() ?: match.value
        }
    }
}
