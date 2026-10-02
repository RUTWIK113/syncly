package com.ppicalendar.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.R
import com.ppicalendar.app.data.notification.NotificationHelper
import com.ppicalendar.app.presentation.dialogs.AppRatingDialog
import com.ppicalendar.app.presentation.dialogs.EventDetailDialog
import com.ppicalendar.app.presentation.dialogs.TestSimulatorDialog
import com.ppicalendar.app.presentation.screens.CompaniesScreen
import com.ppicalendar.app.presentation.screens.HomeScreen
import com.ppicalendar.app.presentation.screens.PermissionsScreen
import com.ppicalendar.app.presentation.screens.SettingsScreen
import com.ppicalendar.app.ui.theme.PPICalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Events", Icons.Default.Home),
    COMPANIES("Vault", Icons.Default.Business),
    SETTINGS("Settings", Icons.Default.Settings),
    PERMISSIONS("Access", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Proactively request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Handle notification click intent if an event ID is passed
        val targetEventId = intent.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, -1L)
        if (targetEventId != -1L) {
            val event = viewModel.allEvents.value.find { it.id == targetEventId }
            if (event != null) {
                viewModel.openEditDialog(event)
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            var isAppReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(650)
                isAppReady = true
            }

            PPICalendarTheme(darkTheme = settings.isDarkTheme) {
                if (!isAppReady) {
                    SynclySplashScreen()
                } else {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun SynclySplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.syncly_logou),
                contentDescription = "Syncly Logo",
                modifier = Modifier
                    .size(105.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Syncly",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Turn campus updates into calendar events.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(Screen.HOME, Screen.COMPANIES, Screen.SETTINGS, Screen.PERMISSIONS)

    val snackbarHostState = remember { SnackbarHostState() }

    val selectedEventForEdit by viewModel.selectedEventForEdit.collectAsState()
    val selectedCompanyForView by viewModel.selectedCompanyForView.collectAsState()
    val isSimulatorOpen by viewModel.isSimulatorOpen.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { notification ->
            snackbarHostState.showSnackbar(message = notification.message)
        }
    }

    // Full Screen Company Dossier View
    val currentCompanyView = selectedCompanyForView
    if (currentCompanyView != null) {
        com.ppicalendar.app.presentation.screens.CompanyDetailScreen(
            company = currentCompanyView,
            viewModel = viewModel,
            onBack = { viewModel.clearSelectedCompany() }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = if (selectedScreenIndex == index) FontWeight.Bold else FontWeight.Normal) },
                            selected = selectedScreenIndex == index,
                            onClick = { selectedScreenIndex = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedScreenIndex) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPermissions = { selectedScreenIndex = 3 }
                    )
                    1 -> CompaniesScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                    3 -> PermissionsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Edit / Review Event Dialog
    selectedEventForEdit?.let { event ->
        EventDetailDialog(
            event = event,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { updatedEvent, createImmediately ->
                viewModel.saveEditedEvent(updatedEvent, createImmediately)
            }
        )
    }

    // Message Simulator Dialog
    if (isSimulatorOpen) {
        TestSimulatorDialog(
            isLoading = isSimulating,
            onDismiss = { viewModel.closeSimulator() },
            onSimulate = { sender, message ->
                viewModel.simulateWhatsAppMessage(sender, message)
            }
        )
    }

    // App Rating / Review Prompt Dialog (5 stars at 5 opens, 10 stars at 50/100 opens)
    val ratingPromptState by viewModel.ratingPromptState.collectAsState()
    ratingPromptState?.let { prompt ->
        AppRatingDialog(
            promptType = prompt.promptType,
            appOpenCount = prompt.appOpenCount,
            onDismiss = { viewModel.dismissRatingDialog() },
            onSubmitRating = { rating, maxStars, reviewText ->
                viewModel.submitRating(rating, maxStars, reviewText)
            }
        )
    }
}
