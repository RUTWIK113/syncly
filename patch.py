import sys

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\domain\model\EventType.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('PRE_PLACEMENT_TALK("Pre-Placement Talk"),', 'PRE_PLACEMENT_TALK("PPT"),')
content = content.replace('ONLINE_ASSESSMENT("Online Assessment"),', 'ONLINE_ASSESSMENT("OA"),')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
