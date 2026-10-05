import sys

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

import_add = 'import androidx.compose.material.icons.filled.Add\n'
if import_add not in content:
    content = content.replace('import androidx.compose.material.icons.filled.CalendarMonth\n', 'import androidx.compose.material.icons.filled.CalendarMonth\n' + import_add)

fab_old = '''        // Floating Action Button - Removed automatically after used 3 times
        if (testNoticeCount < 3) {
            val remaining = 3 - testNoticeCount
            ExtendedFloatingActionButton(
                onClick = { viewModel.openSimulator() },
                icon = { Icon(Icons.Default.Science, contentDescription = null) },
                text = { Text("Test Notice ( left)", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }'''

fab_new = '''        // Floating Action Button for manual entry
        androidx.compose.material3.FloatingActionButton(
            onClick = { viewModel.openSimulator() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Manual Entry")
        }'''

if fab_old in content:
    content = content.replace(fab_old, fab_new)
else:
    print('Could not find fab_old')
    sys.exit(1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Success')
