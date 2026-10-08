import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\CompaniesScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix imports
if 'import androidx.compose.ui.unit.sp' not in content:
    content = content.replace('package com.ppicalendar.app.presentation.screens', 'package com.ppicalendar.app.presentation.screens\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.runtime.collectAsState\nimport androidx.compose.foundation.clickable\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.material3.Text')

# Add states
state_marker = 'var selectedTab by remember { mutableIntStateOf(0) }'
if state_marker in content:
    new_states = state_marker + '''
    val settings by viewModel.settings.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var selectedCalendarCandidate by remember { mutableStateOf<com.ppicalendar.app.domain.model.CalendarInfo?>(null) }
    val currentCalendar = availableCalendars.find { it.id == settings.selectedCalendarId }
'''
    content = content.replace(state_marker, new_states)

# Replace SynclyHeader
header_str = '''SynclyHeader(
                title = "Placement Vault",
                subtitle = "Companies dossiers, JD documents, notes & incentive points tracker."
            )'''

new_header = '''SynclyHeader(
                title = "Placement Vault",
                subtitle = "Companies dossiers, JD documents, notes & incentive points tracker.",
                isHighContrast = settings.isDarkTheme,
                actions = {
                    val initial = settings.connectedEmail.firstOrNull()?.uppercaseChar() ?: 'U'
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(com.ppicalendar.app.ui.theme.SynclyPrimaryAmber)
                            .border(1.dp, Color(0xFFE5D5A0), CircleShape)
                            .clickable {
                                viewModel.refreshCalendars()
                                selectedCalendarCandidate = currentCalendar
                                isAccountDialogVisible = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF231B00),
                            fontSize = 18.sp
                        )
                    }
                }
            )'''
content = content.replace(header_str, new_header)

# Add dialog at the end
end_marker = 'fun CompanyListTab('
if end_marker in content:
    idx = content.find(end_marker)
    last_brace = content.rfind('}', 0, idx)
    
    dialog_code = '''
    com.ppicalendar.app.presentation.components.AccountSelectionDialog(
        isVisible = isAccountDialogVisible,
        onDismiss = { isAccountDialogVisible = false },
        availableCalendars = availableCalendars,
        selectedCalendarCandidate = selectedCalendarCandidate,
        currentCalendar = currentCalendar,
        onCalendarSelect = { selectedCalendarCandidate = it },
        onConfirm = {
            selectedCalendarCandidate?.let {
                viewModel.confirmCalendarConnection(it)
            }
            isAccountDialogVisible = false
        }
    )
'''
    content = content[:last_brace] + dialog_code + '\n' + content[last_brace:]

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("CompaniesScreen updated cleanly")
