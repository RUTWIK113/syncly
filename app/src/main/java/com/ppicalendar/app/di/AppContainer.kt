package com.ppicalendar.app.di

import android.content.Context
import com.ppicalendar.app.data.calendar.CalendarContractManager
import com.ppicalendar.app.data.datastore.DataStoreManager
import com.ppicalendar.app.data.extractor.GeminiApiExtractor
import com.ppicalendar.app.data.extractor.HybridEventExtractor
import com.ppicalendar.app.data.extractor.RuleBasedExtractor
import com.ppicalendar.app.data.local.AppDatabase
import com.ppicalendar.app.data.notification.NotificationHelper
import com.ppicalendar.app.data.repository.CalendarRepositoryImpl
import com.ppicalendar.app.data.repository.CompanyRepositoryImpl
import com.ppicalendar.app.data.repository.PlacementEventRepositoryImpl
import com.ppicalendar.app.data.repository.ProcessedNotificationRepositoryImpl
import com.ppicalendar.app.data.repository.SettingsRepositoryImpl
import com.ppicalendar.app.domain.repository.CalendarRepository
import com.ppicalendar.app.domain.repository.CompanyRepository
import com.ppicalendar.app.domain.repository.EventExtractor
import com.ppicalendar.app.domain.repository.PlacementEventRepository
import com.ppicalendar.app.domain.repository.ProcessedNotificationRepository
import com.ppicalendar.app.domain.repository.SettingsRepository
import com.ppicalendar.app.domain.usecase.CheckDuplicateUseCase
import com.ppicalendar.app.domain.usecase.CreateCalendarEventUseCase
import com.ppicalendar.app.domain.usecase.ExtractPlacementEventUseCase
import com.ppicalendar.app.domain.usecase.ProcessNotificationUseCase
import com.ppicalendar.app.domain.usecase.ResolveDateUseCase

interface AppContainer {
    val database: AppDatabase
    val dataStoreManager: DataStoreManager
    val settingsRepository: SettingsRepository
    val processedNotificationRepository: ProcessedNotificationRepository
    val placementEventRepository: PlacementEventRepository
    val companyRepository: CompanyRepository
    val incentivePointRepository: com.ppicalendar.app.domain.repository.IncentivePointRepository
    val calendarRepository: CalendarRepository
    val eventExtractor: EventExtractor
    val notificationHelper: NotificationHelper
    val telemetryManager: com.ppicalendar.app.data.telemetry.SynclyTelemetryManager

    // Use cases
    val resolveDateUseCase: ResolveDateUseCase
    val extractPlacementEventUseCase: ExtractPlacementEventUseCase
    val createCalendarEventUseCase: CreateCalendarEventUseCase
    val checkDuplicateUseCase: CheckDuplicateUseCase
    val processNotificationUseCase: ProcessNotificationUseCase
    val checkAppUpdateUseCase: com.ppicalendar.app.domain.usecase.CheckAppUpdateUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(dataStoreManager)
    }

    override val processedNotificationRepository: ProcessedNotificationRepository by lazy {
        ProcessedNotificationRepositoryImpl(database.processedNotificationDao())
    }

    override val placementEventRepository: PlacementEventRepository by lazy {
        PlacementEventRepositoryImpl(database.placementEventDao())
    }

    override val companyRepository: CompanyRepository by lazy {
        CompanyRepositoryImpl(database.companyDao(), database.companyAttachmentDao())
    }

    override val incentivePointRepository: com.ppicalendar.app.domain.repository.IncentivePointRepository by lazy {
        com.ppicalendar.app.data.repository.IncentivePointRepositoryImpl(database.incentivePointDao())
    }

    private val calendarContractManager: CalendarContractManager by lazy {
        CalendarContractManager(context)
    }

    override val calendarRepository: CalendarRepository by lazy {
        CalendarRepositoryImpl(calendarContractManager)
    }

    override val eventExtractor: EventExtractor by lazy {
        HybridEventExtractor(
            ruleBasedExtractor = RuleBasedExtractor(resolveDateUseCase),
            geminiApiExtractor = GeminiApiExtractor()
        )
    }

    override val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    override val telemetryManager: com.ppicalendar.app.data.telemetry.SynclyTelemetryManager by lazy {
        com.ppicalendar.app.data.telemetry.SynclyTelemetryManager(
            context = context,
            dataStoreManager = dataStoreManager,
            companyDao = database.companyDao(),
            placementEventDao = database.placementEventDao()
        )
    }

    override val resolveDateUseCase: ResolveDateUseCase by lazy {
        ResolveDateUseCase()
    }

    override val extractPlacementEventUseCase: ExtractPlacementEventUseCase by lazy {
        ExtractPlacementEventUseCase(eventExtractor)
    }

    override val createCalendarEventUseCase: CreateCalendarEventUseCase by lazy {
        CreateCalendarEventUseCase(
            calendarRepository = calendarRepository,
            placementEventRepository = placementEventRepository,
            settingsRepository = settingsRepository
        )
    }

    override val checkDuplicateUseCase: CheckDuplicateUseCase by lazy {
        CheckDuplicateUseCase(
            processedNotificationRepository = processedNotificationRepository,
            placementEventRepository = placementEventRepository,
            calendarRepository = calendarRepository
        )
    }

    override val processNotificationUseCase: ProcessNotificationUseCase by lazy {
        ProcessNotificationUseCase(
            settingsRepository = settingsRepository,
            processedNotificationRepository = processedNotificationRepository,
            placementEventRepository = placementEventRepository,
            extractPlacementEventUseCase = extractPlacementEventUseCase,
            resolveDateUseCase = resolveDateUseCase,
            createCalendarEventUseCase = createCalendarEventUseCase,
            companyRepository = companyRepository
        )
    }

    override val checkAppUpdateUseCase: com.ppicalendar.app.domain.usecase.CheckAppUpdateUseCase by lazy {
        com.ppicalendar.app.domain.usecase.CheckAppUpdateUseCase(context)
    }
}
