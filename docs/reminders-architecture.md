# Reminder and Push Notification Architecture

## Overview

Petit reminders use WorkManager for reliable scheduled execution, even when the app is not in the foreground.

Main flow:

1. User creates or edits a reminder in UI.
2. ViewModel persists data through repository.
3. Repository stores reminder in Room.
4. Scheduler registers work in WorkManager.
5. Worker executes at scheduled time and shows notification.

## Main Components

### ReminderScheduler

Location: `app/src/main/java/com/woliveiras/petit/worker/ReminderScheduler.kt`

Responsibilities:

- Schedule one-time reminder work
- Cancel work by reminder id
- Reschedule for snooze/repeat changes

### ReminderWorker

Location: `app/src/main/java/com/woliveiras/petit/worker/ReminderWorker.kt`

Responsibilities:

- Load reminder by id
- Skip cancelled/completed reminders
- Show notification
- Mark reminder as triggered
- Schedule next occurrence when repeat is enabled

### Notification Channel

Location: `app/src/main/java/com/woliveiras/petit/PetitApplication.kt`

Responsibilities:

- Register reminders channel at app startup
- Configure importance/vibration/description

### Hilt + WorkManager Integration

`PetitApplication` provides WorkManager configuration with `HiltWorkerFactory` to support dependency injection in workers.

## Reminder Lifecycle

### Create Reminder

1. Save reminder data.
2. Enqueue unique work keyed by reminder id.

### Complete or Cancel Reminder

1. Update status in repository.
2. Cancel pending work.

### Snooze Reminder

1. Compute new scheduled time.
2. Save updated reminder.
3. Reschedule work.

### Repeating Reminder

1. Worker fires.
2. Next schedule is calculated from repeat interval.
3. Reminder is updated and rescheduled.

## Permission Model

Android 13+ requires runtime permission:

- `POST_NOTIFICATIONS`

If denied, reminder data can still be saved, but notifications are not shown.

## Known Limitations

- Delivery may be delayed by system power optimizations.
- Reinstall removes WorkManager internal jobs.
- Notification actions may be intentionally limited in MVP versions.

## Reliability Notes

- Prefer unique work names per reminder id.
- Keep reminder status authoritative in Room.
- Always cancel scheduled work when deleting reminders.
