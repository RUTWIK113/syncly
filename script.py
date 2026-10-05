import sys, io
# force stdout to utf-8
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\components\EventCard.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
    
start = -1
for i, line in enumerate(lines):
    if 'Row 1:' in line:
        start = i
        break

if start != -1:
    for j in range(start, min(len(lines), start+180)):
        print(f'{j+1}: {lines[j].rstrip()}')
