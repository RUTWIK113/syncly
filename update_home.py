import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace actions
pill_start = content.find('actions = {')
if pill_start != -1:
    pill_end = content.find(')', content.find('SynclyHeader', pill_start-100))
    # We will replace from actions = { ... } to the end of SynclyHeader actions
    # The actions block ends before the closing paren of SynclyHeader
    pill_end = content.find(')', pill_start)
    new_actions = '''actions = {
                    val initial = liveSettings.connectedEmail.firstOrNull()?.uppercaseChar() ?: 'U'
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(com.ppicalendar.app.ui.theme.SynclyPrimaryAmber)
                            .border(1.dp, Color(0xFFE5D5A0), androidx.compose.foundation.shape.CircleShape)
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
                }'''
    # Find the closing brace of actions
    braces = 0
    in_block = False
    for i in range(pill_start, len(content)):
        if content[i] == '{':
            braces += 1
            in_block = True
        elif content[i] == '}':
            braces -= 1
        if in_block and braces == 0:
            content = content[:pill_start] + new_actions + content[i+1:]
            break

# Replace AlertDialog with AccountSelectionDialog
alert_start = content.find('if (isAccountDialogVisible)')
if alert_start != -1:
    alert_end = content.find('// ---------------------------------------------------------------------------', alert_start)
    if alert_end == -1:
        alert_end = content.rfind('}', 0, content.find('@Composable', alert_start))
        
    new_dialog = '''
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
                triggerAutoSaveIndicator()
            }
            isAccountDialogVisible = false
        }
    )
'''
    # Using python block replacement
    braces = 0
    in_block = False
    for i in range(alert_start, len(content)):
        if content[i] == '{':
            braces += 1
            in_block = True
        elif content[i] == '}':
            braces -= 1
        if in_block and braces == 0:
            content = content[:alert_start] + new_dialog + content[i+1:]
            break

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("HomeScreen updated")
