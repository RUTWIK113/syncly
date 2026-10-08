import sys
import io
import re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\SettingsScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

def get_block(start_str):
    start = content.find(start_str)
    if start == -1: return ''
    # go backwards to find `item {`
    item_start = content.rfind('item {', 0, start)
    
    braces = 0
    in_block = False
    for i in range(item_start, len(content)):
        if content[i] == '{':
            braces += 1
            in_block = True
        elif content[i] == '}':
            braces -= 1
        if in_block and braces == 0:
            return content[item_start:i+1]
    return ''

g_cal = get_block('SettingsSectionCard(title = "Google Calendar Connection"')
acc = get_block('SettingsSectionCard(title = "Accessibility"')
auto = get_block('SettingsSectionCard(title = "Automation Rules"')
det = get_block('SettingsSectionCard(title = "Detection Keywords"')
ai = get_block('SettingsSectionCard(title = "AI Extraction Engine"')
feed = get_block('SettingsSectionCard(title = "Feedback & Bug Report"')

print(f'Gcal: {len(g_cal)}')
print(f'Acc: {len(acc)}')
print(f'Auto: {len(auto)}')
print(f'Det: {len(det)}')
print(f'Ai: {len(ai)}')
print(f'Feed: {len(feed)}')

# We need to construct the Access & Permissions block
# It includes Google Calendar Connection and Notification Listener toggle
# And the Google Calendar Integration toggle + Sync mode

access_block = '''item {
    SettingsSectionCard(title = "Access & Permissions", icon = Icons.Default.Security) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Notification Listener
            val context = LocalContext.current
            val isNotificationListenerEnabled = remember {
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
            SettingsToggleRow(
                title = "Notification Listener",
                subtitle = if (isNotificationListenerEnabled) "Enabled (can read WhatsApp messages)" else "Disabled (app will not function)",
                checked = isNotificationListenerEnabled,
                onCheckedChange = {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            )

            HorizontalDivider()

            // Google Calendar Integration
            SettingsToggleRow(
                title = "Google Calendar Integration",
                subtitle = if (settings.googleCalendarIntegrationEnabled) "Enabled" else "Disabled (Internal Calendar Only)",
                checked = settings.googleCalendarIntegrationEnabled,
                onCheckedChange = { viewModel.setGoogleCalendarIntegrationEnabled(it) }
            )

            if (settings.googleCalendarIntegrationEnabled) {
                // Sync Mode Dropdown
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sync Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "How to add events to Google Calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(settings.googleCalendarSyncMode)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto (Instant)") },
                                onClick = { 
                                    viewModel.setGoogleCalendarSyncMode("Auto")
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manual (On Approval)") },
                                onClick = { 
                                    viewModel.setGoogleCalendarSyncMode("Manual")
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
                
                // Keep the old Gcal connection view inside it
''' + g_cal.replace('item {\n    SettingsSectionCard(title = "Google Calendar Connection", icon = Icons.Default.Email) {', '').rstrip('}') + '''
            }
        }
    }
}'''

# Remove extra } from the end since we stripped the wrapper

# Now construct the final LazyColumn items
lazy_content = f'''{ai}
{access_block}
{auto}
{det}
{acc}
{feed}'''

# Replace in content
start = content.find('LazyColumn(')
if start != -1:
    content_list_start = content.find('{', start)
    # Find matching brace for LazyColumn
    braces = 0
    in_block = False
    content_list_end = -1
    for i in range(content_list_start, len(content)):
        if content[i] == '{':
            braces += 1
            in_block = True
        elif content[i] == '}':
            braces -= 1
        if in_block and braces == 0:
            content_list_end = i
            break
    
    new_lazy_column = content[start:content_list_start+1] + '\n' + lazy_content + '\n' + content[content_list_end:]
    content = content[:start] + new_lazy_column

content = content.replace('title = "Preferences",', 'title = "Settings",')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done rewriting.")
