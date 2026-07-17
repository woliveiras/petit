# Plan: Local tasks and reminders

Spec: [spec.md](./spec.md)

## Sequence

1. Model `TaskEntity` with types `VACCINATION`, `DEWORMING`, `WEIGHT`, and `CUSTOM` and states `PENDING`/`COMPLETED`.
2. Implement the repository and `AutoTaskService` to create/cancel linked tasks.
3. Integrate `TaskScheduler` and `TaskNotificationWorker` with WorkManager.
4. Persist `ReminderPreferences` in DataStore.
5. Integrate the list, form, settings, and completed tasks.

## Architecture

- Tasks and notifications are local and one-shot.
- `referenceEntityId` links a task to the health record that created it.
- `PendingIntent` must be immutable; deletion and completion cancel the scheduled job.
- Routes: `tasks`, `tasks/form?taskId={taskId}`, `tasks/settings`, and `tasks/completed`.

## Dependencies and risks

- Depends on `0001` and receives events from `0002`, `0003`, and `0004`.
- Android restrictions on notifications and background execution require integration tests.
