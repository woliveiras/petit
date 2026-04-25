# TalkBack Accessibility Testing Guide

Goal: validate Petit accessibility with Android TalkBack on a real device.

## Enable TalkBack

### Option 1: Settings

1. Open device Settings.
2. Go to Accessibility.
3. Open TalkBack.
4. Enable TalkBack.

### Option 2: Shortcut

If configured, press and hold both volume keys for 3 seconds.

## Core TalkBack Gestures

| Gesture               | Action                |
| --------------------- | --------------------- |
| Single tap            | Focus and read item   |
| Double tap            | Activate focused item |
| Swipe right           | Next item             |
| Swipe left            | Previous item         |
| Two-finger swipe down | Scroll down           |
| Two-finger swipe up   | Scroll up             |

## Screen Checklist

### Home

- [ ] Empty-state message is announced
- [ ] Primary CTA is announced with clear action
- [ ] Top-bar actions have descriptive labels

### Main Navigation

- [ ] Each tab has a meaningful label
- [ ] FAB announces its action
- [ ] Quick actions are reachable and actionable

### Pet Form

- [ ] Title, fields, and errors are announced
- [ ] Date and selection controls are accessible
- [ ] Save state (enabled/disabled) is announced

### Cat Details

- [ ] Cat identity and health sections are readable
- [ ] Action buttons have specific labels

### Weight, Vaccination, Deworming, Reminders

- [ ] List items announce key info (type, date, status)
- [ ] Add buttons are labeled
- [ ] Status chips are understandable via text, not color alone

### Settings

- [ ] Preferences announce current state
- [ ] Destructive actions clearly communicate impact

## Acceptance Criteria

Critical:

1. Full screen navigation possible with TalkBack only
2. Forms are fully operable
3. Critical actions are understandable and actionable

Important:

1. Logical reading order
2. State changes are announced
3. Related content grouped semantically

## Common Issues

| Issue                 | Likely Cause                          | Fix                                |
| --------------------- | ------------------------------------- | ---------------------------------- |
| Element not announced | Missing semantics/content description | Add semantic label                 |
| Vague announcement    | Generic label text                    | Use explicit action/object wording |
| Hard to tap           | Touch target too small                | Increase target size to >= 48dp    |
| Incorrect order       | Composition order mismatch            | Tune semantic traversal order      |

## Test Log Template

- Date:
- Tester:
- Device:
- Android version:
- TalkBack version:
- App version:

Results:

- Home:
- Navigation:
- Forms:
- Lists:
- Settings:
- Critical issues:
