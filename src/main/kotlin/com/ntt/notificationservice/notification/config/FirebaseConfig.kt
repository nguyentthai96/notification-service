package com.ntt.notificationservice.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

/**
 * Firebase configuration for FCM push notifications.
 * Only active when push channel is enabled.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app.notification.channels.push",
    name = ["enabled"],
    havingValue = "true"
)
class FirebaseConfig(
    private val properties: NotificationProperties
) {
    private val log = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp {
        val credentialsFile = properties.push.fcm.credentialsFile
        require(credentialsFile.isNotBlank()) { "Firebase credentials file path is required" }

        val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsFile))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }

        log.info("Firebase initialized: projectId={}", app.options.projectId)
        return app
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging {
        return FirebaseMessaging.getInstance(firebaseApp)
    }
}
