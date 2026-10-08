import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\AppCalendarScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add states
state_marker = 'var currentMonth by remember { mutableStateOf(YearMonth.now()) }'
if state_marker in content:
    new_states = state_marker + '''
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var selectedCalendarCandidate by remember { mutableStateOf<com.ppicalendar.app.domain.model.CalendarInfo?>(null) }
    val currentCalendar by viewModel.currentCalendar.collectAsState()
'''
    content = content.replace(state_marker, new_states)

# Add clickable to Box
box_marker = '.border(1.dp, Color(0xFFE5D5A0), CircleShape)'
if box_marker in content:
    content = content.replace(box_marker, box_marker + '''
                        .clickable {
                            viewModel.refreshCalendars()
                            selectedCalendarCandidate = currentCalendar
                            isAccountDialogVisible = true
                        }''')

# Add dialog at the end
end_marker = 'fun DayCell'
if end_marker in content:
    idx = content.find(end_marker)
    # find the previous closing brace (end of AppCalendarScreen)
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

print("AppCalendarScreen updated")
