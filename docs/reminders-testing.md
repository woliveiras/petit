# Reminder and Push Notification Testing Guide

## Preconditions

1. Android 8+ emulator or physical device
2. Petit debug build installed
3. Notification permission granted on Android 13+

## Manual Tests

### 1. Create Reminder

1. Open reminders screen.
2. Add a reminder for near-future time.
3. Save.

Expected:

- Reminder appears in upcoming list.

### 2. Push Notification with App Closed

1. Create reminder for +1 minute.
2. Fully close app.
3. Wait for trigger time.

Expected:

- Notification appears with reminder title.

### 3. Android 13 Notification Permission

1. Fresh install on Android 13+.
2. Open reminders and attempt to add first reminder.

Expected:

- Runtime permission flow appears.
- If denied, reminder can still be saved but no notification is delivered.

### 4. Snooze

1. Pick active reminder.
2. Apply snooze duration.

Expected:

- Reminder moves to snoozed/updated schedule and triggers later.

### 5. Complete Reminder

1. Create reminder in future.
2. Mark as completed.
3. Wait past original schedule.

Expected:

- No notification is shown.

### 6. Delete Reminder

1. Create reminder in future.
2. Delete reminder.
3. Wait past original schedule.

Expected:

- No notification is shown.

### 7. Repeat Reminder

1. Create reminder with repeat interval.
2. Wait for first trigger.

Expected:

- Next occurrence is scheduled automatically.

## Edge Cases

### Past Date

Expected:

- Validation error; save is blocked.

### Empty Title

Expected:

- Validation error; save is blocked.

### Device Reboot

1. Create future reminder.
2. Reboot device.
3. Wait for schedule.

Expected:

- Reminder still triggers (small delay may occur).

## ADB Checks (Advanced)

```bash
adb shell dumpsys jobscheduler | grep -A 10 "com.woliveiras.petit"
adb logcat -s ReminderWorker:V
```

## Release Checklist

- [ ] Trigger with app foreground
- [ ] Trigger with app background
- [ ] Trigger with app closed
- [ ] Snooze reschedules correctly
- [ ] Complete cancels pending trigger
- [ ] Delete cancels pending trigger
- [ ] Repeat intervals reschedule correctly
- [ ] Android 13 permission flow validated
