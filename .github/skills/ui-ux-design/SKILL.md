---
name: ui-ux-design
description: "UI/UX design patterns and Material 3 best practices for Petit Android app. Use when: creating new screens, building Compose layouts, choosing Material 3 components, designing forms, implementing loading/empty/error states, adding animations and transitions, working with typography hierarchy, spacing and sizing, card and list designs, navigation patterns, visual feedback, color usage, dark theme support, skeleton loaders, confirmation feedback, swipe actions."
---

# UI/UX Design — Petit Android

Based on [Material Design 3 guidelines](https://m3.material.io/), [Android design patterns](https://developer.android.com/design), and industry best practices for health tracking apps.

## When to Use

- Creating new screens or composables
- Choosing between Material 3 component variants
- Laying out content (spacing, alignment, visual hierarchy)
- Designing forms with good UX
- Implementing loading, empty, and error states
- Adding animations and micro-interactions
- Improving visual polish and user feedback

## Design Principles

Based on [Material Design 3 core principles](https://m3.material.io/foundations):

1. **Personal** — Dynamic Color (Material You) makes the app feel like the user's own
2. **Adaptive** — Layouts adapt to screen size, orientation, and input type
3. **Expressive** — Motion, color, and typography create visual hierarchy without clutter
4. **Accessible** — WCAG AA contrast, 48dp touch targets, semantic structure for TalkBack

### App Personality

Petit is a **multi-pet health tracker**. Following [Google's emotional design guidance](https://m3.material.io/foundations/content-design/overview):

- **Warm, not clinical** — round shapes, soft containers, friendly copy
- **Scannable, not dense** — generous whitespace, clear grouping, progressive disclosure
- **Reliable, not flashy** — consistent patterns, predictable navigation, clear feedback
- **Delightful, not distracting** — purposeful animations, subtle transitions

### No Emojis in UI

**Never use emoji characters (Unicode emoji) in layouts, cards, list items, or section headers.** Always use Material Icons (`Icons.Default.*`, `Icons.Outlined.*`) instead. Emojis render inconsistently across Android versions and manufacturers, cannot be tinted to match the theme, don't adapt to dark mode, and lack proper semantic accessibility.

```kotlin
// BAD — emoji as visual element
Text(text = "💉", style = MaterialTheme.typography.headlineMedium)
Text(text = "🔔 UPCOMING", style = MaterialTheme.typography.titleMedium)

// GOOD — Material Icon with theme-aware tint
Icon(
    Icons.Default.Vaccines,
    contentDescription = null, // decorative
    tint = MaterialTheme.colorScheme.primary,
    modifier = Modifier.size(24.dp),
)
Text(text = "UPCOMING", style = MaterialTheme.typography.titleMedium)
```

**Exception:** Emojis are acceptable in system notifications (`NotificationCompat.Builder`) where Material Icons are not available and emojis improve glanceability in the notification shade.

---

## Color System

### Material 3 Color Roles

Follow the [M3 color system](https://m3.material.io/styles/color/roles). Never hardcode hex values for semantic elements — always use `MaterialTheme.colorScheme` tokens.

| Role                               | Use For                                                | NOT For                        |
| ---------------------------------- | ------------------------------------------------------ | ------------------------------ |
| `primary`                          | FAB, key buttons, active nav, links                    | Body text, backgrounds         |
| `primaryContainer`                 | Selected states, emphasis cards                        | All cards indiscriminately     |
| `secondary` / `secondaryContainer` | Filter chips, secondary info, tags                     | Primary actions                |
| `tertiary` / `tertiaryContainer`   | Complementary accents, category badges                 | Anything that secondary covers |
| `error` / `errorContainer`         | Validation errors, overdue alerts, destructive actions | Warnings (use custom color)    |
| `surface`                          | Base background                                        | —                              |
| `surfaceContainerLowest`           | Behind scrolling content                               | —                              |
| `surfaceContainerLow`              | Standard cards                                         | —                              |
| `surfaceContainer`                 | App bars, navigation                                   | —                              |
| `surfaceContainerHigh`             | Dialogs, search bars, elevated cards                   | —                              |
| `surfaceContainerHighest`          | Text fields, highest emphasis                          | —                              |
| `onSurface`                        | Primary text on surface                                | —                              |
| `onSurfaceVariant`                 | Secondary/supporting text, icons                       | Primary text                   |
| `outline`                          | Borders, dividers                                      | —                              |
| `outlineVariant`                   | Subtle dividers                                        | Prominent borders              |

### Dynamic Color (Material You)

```kotlin
// GOOD — support dynamic color on Android 12+ with fallback
val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    darkTheme -> darkColorScheme(/* your dark colors */)
    else -> lightColorScheme(/* your light colors */)
}
```

### Color Rules

```kotlin
// GOOD — semantic tokens that adapt to light/dark/dynamic
Text(text, color = MaterialTheme.colorScheme.onSurface)
Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))

// BAD — hardcoded colors that break in dark theme and ignore dynamic color
Text(text, color = Color.Black)
Card(colors = CardDefaults.cardColors(containerColor = Color.White))
Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)))
```

### Status/Semantic Colors

Per [M3 custom colors guidance](https://m3.material.io/styles/color/advanced/define-new-colors), define custom semantic colors alongside the scheme:

```kotlin
// Health status — always pair with icon + text (never color alone per WCAG)
val HealthGreen = Color(0xFF2E7D32)    // success/healthy — OK status
val HealthAmber = Color(0xFFF57F17)    // warning/attention — upcoming due date
val HealthRed = Color(0xFFC62828)      // critical/overdue — missed deadline

// Dark theme variants (higher luminance for contrast)
val HealthGreenDark = Color(0xFF66BB6A)
val HealthAmberDark = Color(0xFFFFCA28)
val HealthRedDark = Color(0xFFEF5350)
```

Always convey status with **icon + text + color** (never color alone):

```kotlin
@Composable
fun HealthStatusBadge(status: HealthStatus, modifier: Modifier = Modifier) {
    val (icon, color, label) = when (status) {
        HealthStatus.OK -> Triple(Icons.Default.CheckCircle, HealthGreen, "Em dia")
        HealthStatus.ATTENTION -> Triple(Icons.Default.Schedule, HealthAmber, "Atenção")
        HealthStatus.OVERDUE -> Triple(Icons.Default.Warning, HealthRed, "Atrasado")
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}
```

---

## Typography

### Material 3 Type Scale

Follow the [M3 type scale](https://m3.material.io/styles/typography/type-scale-tokens). Define ALL roles in your `Typography()` — don't rely on defaults:

| Role             | Size/Weight    | Petit Usage                                 |
| ---------------- | -------------- | ----------------------------------------- |
| `displayLarge`   | 57sp / Regular | Not used (reserved for tablets)           |
| `displayMedium`  | 45sp / Regular | Hero weight numbers ("4.5 kg")            |
| `displaySmall`   | 36sp / Regular | Dashboard greeting, large stats           |
| `headlineLarge`  | 32sp / Regular | —                                         |
| `headlineMedium` | 28sp / Regular | Screen titles in expanded top bars        |
| `headlineSmall`  | 24sp / Regular | Section headers, prominent card titles    |
| `titleLarge`     | 22sp / Regular | Top app bar title, dialog titles          |
| `titleMedium`    | 16sp / Medium  | Card titles, list item primary text       |
| `titleSmall`     | 14sp / Medium  | Sub-section headers, tab labels           |
| `bodyLarge`      | 16sp / Regular | Primary body text                         |
| `bodyMedium`     | 14sp / Regular | Secondary body text, descriptions         |
| `bodySmall`      | 12sp / Regular | Captions, timestamps, supporting text     |
| `labelLarge`     | 14sp / Medium  | Button text                               |
| `labelMedium`    | 12sp / Medium  | Form labels, chip text, navigation labels |
| `labelSmall`     | 11sp / Medium  | Badges, footnotes, helper text            |

### Hierarchy Rules

Per [M3 typography guidance](https://m3.material.io/styles/typography/applying-type):

1. **Use at least 3 distinct type styles per screen** to create visual hierarchy
2. **Never use raw `fontSize`** — always use `MaterialTheme.typography.*`
3. **Use `fontWeight` for emphasis** within a style, not a different style
4. **Use `onSurfaceVariant` for secondary text** — not a lighter font weight

```kotlin
// GOOD — clear 3-level hierarchy using M3 tokens
Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("Luna", style = MaterialTheme.typography.titleMedium)
    Text("Fêmea • 2 anos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Peso: 4.5 kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// BAD — flat hierarchy, no tokens
Column {
    Text("Luna", fontSize = 16.sp)
    Text("Fêmea • 2 anos", fontSize = 14.sp)
    Text("Peso: 4.5 kg", fontSize = 12.sp, color = Color.Gray)
}
```

---

## Spacing & Layout

### 8dp Grid System

Follow the [M3 spacing system](https://m3.material.io/foundations/layout/understanding-layout/spacing):

| Value | Name         | Usage                                                 |
| ----- | ------------ | ----------------------------------------------------- |
| 4.dp  | Extra-small  | Icon-to-label gap, tight inline spacing               |
| 8.dp  | Small        | Between related items in a group, chip gaps           |
| 12.dp | Medium-small | Inner card padding (compact), list divider insets     |
| 16.dp | Medium       | **Default** — screen padding, card padding, list gaps |
| 24.dp | Large        | Between sections, generous card padding               |
| 32.dp | Extra-large  | Empty state padding, major visual breaks              |
| 48.dp | 2x-large     | Top/bottom page margins on sparse screens             |

### Screen Padding Standards

```kotlin
// Per M3 layout guidelines: 16dp horizontal margin for phone
val ScreenHorizontalPadding = 16.dp

// LazyColumn content padding (accounts for system bars + padding)
LazyColumn(
    contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
        bottom = 88.dp // space for FAB + bottom nav
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp) // gap between items
)
```

### Card Internal Layout

Per [M3 card anatomy](https://m3.material.io/components/cards/guidelines):

```kotlin
Card(shape = RoundedCornerShape(12.dp)) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Header section
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Leading visual (avatar/icon): 40-56dp
            // Title + subtitle column
        }
        Spacer(Modifier.height(16.dp))
        // Content section
        // ...
        Spacer(Modifier.height(8.dp))
        // Action section (buttons/links aligned end)
    }
}
```

### Canonical Shape Values

Per [M3 shape system](https://m3.material.io/styles/shape/shape-scale-tokens):

| Shape       | Value | Usage                       |
| ----------- | ----- | --------------------------- |
| None        | 0.dp  | —                           |
| Extra-small | 4.dp  | Badges, tooltips            |
| Small       | 8.dp  | Chips, small buttons        |
| Medium      | 12.dp | Cards, text fields, dialogs |
| Large       | 16.dp | FAB, navigation drawers     |
| Extra-large | 28.dp | Bottom sheets, large FABs   |
| Full        | 50%   | Avatar circles, pills       |

```kotlin
// Shapes for Petit — use consistently
val CardShape = RoundedCornerShape(12.dp)          // All cards
val TextFieldShape = RoundedCornerShape(12.dp)     // All text fields
val ChipShape = RoundedCornerShape(8.dp)           // Chips, tags
val AvatarShape = CircleShape                       // Cat photos, user icons
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val FABShape = RoundedCornerShape(16.dp)           // M3 FAB default
```

---

## Component Selection

### Buttons — Per [M3 Button Guidance](https://m3.material.io/components/buttons/guidelines)

| Component                      | Role                                            | Usage Rule                                    |
| ------------------------------ | ----------------------------------------------- | --------------------------------------------- |
| `Button` (Filled)              | **Highest emphasis** — final action on screen   | Max 1 per visible area. "Salvar", "Confirmar" |
| `FilledTonalButton`            | **Medium emphasis** — important but not primary | "Ver todos", "Filtrar"                        |
| `OutlinedButton`               | **Medium emphasis** — alternative/back action   | "Cancelar", "Voltar"                          |
| `TextButton`                   | **Lowest emphasis** — tertiary, inline          | Dialog dismiss, "Pular", "Saiba mais"         |
| `IconButton`                   | **Icon-only** action                            | Toolbar buttons, inline row actions           |
| `FloatingActionButton`         | **Primary creation/action** for the screen      | 1 per screen maximum                          |
| `ExtendedFloatingActionButton` | FAB with label when action isn't obvious        | Use if icon alone is ambiguous                |

```kotlin
// GOOD — clear visual hierarchy per M3 guidelines
Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
        Text("Cancelar")
    }
    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
        Text("Salvar")
    }
}
```

### Cards — Per [M3 Card Guidance](https://m3.material.io/components/cards/overview)

| Variant         | Use For                                           | Surface                        |
| --------------- | ------------------------------------------------- | ------------------------------ |
| `Card` (Filled) | Primary content: pet profile, health summary      | `surfaceContainerHighest`      |
| `ElevatedCard`  | Prominent call-out: dashboard highlight, stat     | Elevated `surfaceContainerLow` |
| `OutlinedCard`  | Sequential lists: history entries, timeline items | `surface` with outline         |

### Top App Bars — Per [M3 Top App Bar](https://m3.material.io/components/top-app-bar/overview)

| Variant             | Use For                                |
| ------------------- | -------------------------------------- |
| `TopAppBar` (Small) | Standard inner screens with title      |
| `MediumTopAppBar`   | Detail screens with collapsible title  |
| `LargeTopAppBar`    | Dashboard/home with prominent greeting |

```kotlin
// Detail screen with collapsible title
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

Scaffold(
    topBar = {
        MediumTopAppBar(
            title = { Text("Luna") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            scrollBehavior = scrollBehavior,
        )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { /* content */ }
```

### Bottom Navigation — Per [M3 Navigation Bar](https://m3.material.io/components/navigation-bar/guidelines)

Rules:

- **3-5 destinations only** (M3 guideline)
- **Use filled icons for active, outlined for inactive** (M3 standard)
- **Always show labels** — icon-only is discouraged by M3
- **Persist across screens** — only hide for immersive/full-screen flows

### Dialogs — Per [M3 Dialog Guidelines](https://m3.material.io/components/dialogs/guidelines)

```kotlin
// Destructive confirmation — follow M3 pattern
AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
    title = { Text("Excluir ${catName}?") },
    text = { Text("Esta ação é irreversível. Todos os registros de saúde serão removidos.") },
    confirmButton = {
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Excluir") }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
)
```

Rules per M3:

- **Dismiss button** always on the left, **confirm** on the right
- **Destructive confirm** uses `error` color
- **Icon** for context (optional but recommended for destructive actions)
- **Title**: concise question. **Text**: explains consequences

### Snackbars — Per [M3 Snackbar Guidelines](https://m3.material.io/components/snackbar/guidelines)

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

// Success feedback
snackbarHostState.showSnackbar("Pet salvo com sucesso", duration = SnackbarDuration.Short)

// Delete with undo
val result = snackbarHostState.showSnackbar(
    message = "Registro excluído",
    actionLabel = "Desfazer",
    duration = SnackbarDuration.Long,
)
if (result == SnackbarResult.ActionPerformed) { viewModel.undoDelete() }
```

Rules:

- **Short** (4s) for confirmations: "Salvo", "Enviado"
- **Long** (10s) for actions with undo: "Excluído" + "Desfazer"
- **Never** Snackbar for errors — use inline error or full-screen error state
- **Max 1 Snackbar** at a time — they queue automatically

---

## Screen Patterns

### List Screen (Master)

Per [M3 Lists guidance](https://m3.material.io/components/lists/guidelines):

```kotlin
Scaffold(
    topBar = { TopAppBar(title = { Text("Meus Pets") }) },
    floatingActionButton = {
        ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Novo Pet") })
    }
) { padding ->
    when {
        isLoading -> SkeletonList(modifier = Modifier.padding(padding))
        cats.isEmpty() -> EmptyState(modifier = Modifier.padding(padding), /* ... */)
        else -> LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(pets, key = { it.id }) { pet -> PetCard(pet, onClick = { onPetClick(pet.id) }) }
        }
    }
}
```

### Detail Screen

Per [M3 layout guidelines](https://m3.material.io/foundations/layout/understanding-layout/overview) — use vertical scrolling with clear sections:

```kotlin
Scaffold(
    topBar = {
        MediumTopAppBar(
            title = { Text(pet.name) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") } },
            scrollBehavior = scrollBehavior,
        )
    }
) { padding ->
    Column(
        modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Profile header (photo + name + key info)
        // 2. Quick stats row (weight, vaccine status, deworming status)
        // 3. Detail sections as clickable action cards
        // 4. Recent timeline
    }
}
```

### Form Screen

Per [M3 text field guidelines](https://m3.material.io/components/text-fields/guidelines) and [form design best practices](https://developer.android.com/develop/ui/compose/text/user-input):

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(if (isEditing) "Editar Pet" else "Novo Pet") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fechar") } },
            actions = { TextButton(onClick = onSave, enabled = isValid) { Text("Salvar") } },
        )
    }
) { padding ->
    Column(
        modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp), // 24dp between SECTIONS
    ) {
        // Group fields by topic with 16dp spacing within groups
        FormSection("Informações Básicas") { /* name, birthdate, sex */ }
        FormSection("Aparência") { /* breed, color */ }
        FormSection("Identificação") { /* microchip, passport */ }
        FormSection("Observações") { /* notes multiline */ }
    }
}
```

### Dashboard / Home

Per [M3 canonical layouts](https://m3.material.io/foundations/layout/canonical-layouts/overview):

```kotlin
LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp), // generous spacing between dashboard sections
) {
    // 1. Greeting section (headlineSmall + bodyMedium)
    // 2. Alerts banner (if any overdue items — errorContainer background)
    // 3. Cats overview (horizontal scroll or vertical cards)
    // 4. Timeline sections (recent + upcoming)
}
```

---

## State Handling

### Loading — Skeleton Screens

Per [industry best practice](https://m3.material.io/styles/motion/overview) — content-shaped placeholders reduce perceived wait time vs. spinners:

```kotlin
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Avatar placeholder
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                // Title placeholder
                Box(Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                // Subtitle placeholder
                Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
            }
        }
    }
}

@Composable
fun SkeletonList(modifier: Modifier = Modifier, count: Int = 4) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count) { SkeletonCard() }
    }
}
```

**When to use which:**

| Pattern                     | When                                         |
| --------------------------- | -------------------------------------------- |
| Skeleton/shimmer            | Initial page load, switching tabs/categories |
| `CircularProgressIndicator` | Form submit, short operations (<2s)          |
| `LinearProgressIndicator`   | Upload/download, measurable progress         |
| Pull-to-refresh spinner     | User-triggered refresh on existing content   |

### Empty State

Per [Material guidelines for empty states](https://m3.material.io/foundations/content-design/overview):

```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Large tinted icon
        Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))
        // Title
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        // Description
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        // CTA
        FilledTonalButton(onClick = onAction) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel)
        }
    }
}
```

### Error State

```kotlin
@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
```

---

## Animations & Motion

### M3 Motion Principles

Per [M3 motion system](https://m3.material.io/styles/motion/overview):

1. **Informative** — motion shows spatial relationships and result of actions
2. **Focused** — draws attention to what matters without creating distraction
3. **Expressive** — celebrates moments (success, achievement, delight)

### M3 Duration & Easing Tokens

```kotlin
// M3 standard durations
val DurationShort1 = 50    // Micro-interactions (ripple, toggle)
val DurationShort2 = 100   // Small state changes
val DurationMedium1 = 200  // Standard transitions (fade, scale)
val DurationMedium2 = 300  // Card expand, bottom sheet
val DurationLong1 = 450    // Page transitions
val DurationLong2 = 500    // Complex choreography

// M3 standard easings
val EasingEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)           // Default for most transitions
val EasingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f) // Enter
val EasingEmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f) // Exit
val EasingStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)              // Within-screen changes
```

### Navigation Transitions

Per [M3 navigation transitions](https://m3.material.io/styles/motion/transitions/transition-patterns):

```kotlin
// Forward navigation: slide in from right
enterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 300, easing = EasingEmphasizedDecelerate)
    ) + fadeIn(animationSpec = tween(150))
}

// Backward navigation: slide out to right
popExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 250, easing = EasingEmphasizedAccelerate)
    ) + fadeOut(animationSpec = tween(100))
}
```

### Content Transitions

```kotlin
// Staggered list appearance (industry standard for feed-like screens)
LazyColumn {
    itemsIndexed(items) { index, item ->
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(200, delayMillis = index * 40)) + slideInVertically(initialOffsetY = { 24 }),
        ) {
            ItemCard(item)
        }
    }
}
```

### Shared Element Concepts

```kotlin
// Expand/collapse card detail — smooth size animation
Column(modifier = Modifier.animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))) {
    HeaderRow(onClick = { expanded = !expanded })
    if (expanded) {
        DetailContent()
    }
}
```

### Animated Counters

```kotlin
// Weight display with counting animation
val animatedWeight by animateFloatAsState(
    targetValue = weightKg,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
)
Text(
    text = "%.1f".format(animatedWeight),
    style = MaterialTheme.typography.displayMedium,
    fontWeight = FontWeight.Bold,
)
Text("kg", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
```

### FAB Motion

Per [M3 FAB guidelines](https://m3.material.io/components/floating-action-button/guidelines):

```kotlin
val rotation by animateFloatAsState(if (expanded) 45f else 0f, animationSpec = tween(200))

FloatingActionButton(onClick = { expanded = !expanded }) {
    Icon(Icons.Default.Add, contentDescription = "Ações rápidas", modifier = Modifier.rotate(rotation))
}
```

---

## Form Design

### Per [M3 Text Field Guidelines](https://m3.material.io/components/text-fields/guidelines)

**Use `OutlinedTextField`** (not filled) for forms with multiple fields — it provides clearer field boundaries.

### Field Grouping

Group related fields with clear sections (per [form design best practices](https://developer.android.com/develop/ui/compose/text/user-input)):

```kotlin
@Composable
fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
```

### Field Ordering (UX standard)

1. **Visual fields** (photo picker)
2. **Required fields** — most important first (`name`)
3. **Frequently used** fields (`birthDate`, `sex`)
4. **Optional fields** (`breed`, `color`, `microchip`)
5. **Free text** last (`notes`)

### Keyboard Optimization

Per [Android input docs](https://developer.android.com/develop/ui/compose/text/user-input):

```kotlin
// Name → capitalize words, advance to next
keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

// Weight → decimal pad
keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)

// Notes → multiline, no action (Enter = newline)
keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default)
```

### Inline Validation

Per [M3 text field states](https://m3.material.io/components/text-fields/guidelines#8f217ee2-6b5b-4a56-a7d2-8c4c7e172125):

```kotlin
OutlinedTextField(
    value = name,
    onValueChange = { onNameChange(it) },
    label = { Text("Nome do gato") },
    placeholder = { Text("Ex: Luna") },
    isError = nameError != null,
    supportingText = {
        if (nameError != null) {
            Text(nameError, color = MaterialTheme.colorScheme.error)
        } else if (name.length > 40) {
            Text("${name.length}/50") // character counter near limit
        }
    },
    leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) },
    shape = RoundedCornerShape(12.dp),
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
)
```

Rules:

- **Show errors after first interaction** (blur or submit), not immediately
- **`supportingText`** for hints, counter, or errors — never use a separate `Text()` below
- **`leadingIcon`** for context (optional — don't overuse)
- **`placeholder`** for examples — never duplicate the label

---

## Card Design Patterns

### List Item Card

Per [M3 list items](https://m3.material.io/components/lists/guidelines):

```kotlin
@Composable
fun PetListCard(pet: Pet, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading: avatar (48-56dp per M3)
            AsyncImage(
                model = pet.photoUri,
                contentDescription = "Foto de ${pet.name}",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            // Content: 2-3 lines max
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pet.name, style = MaterialTheme.typography.titleMedium)
                Text("${pet.age} • ${cat.sex.displayName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Trailing: chevron or status badge
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

### Stat/Metric Card

Per [dashboard design patterns](https://m3.material.io/foundations/layout/canonical-layouts/overview):

```kotlin
@Composable
fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
```

### Action Card (Tappable Section)

```kotlin
@Composable
fun ActionCard(icon: ImageVector, iconContainerColor: Color, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = iconContainerColor.copy(alpha = 0.12f), modifier = Modifier.size(48.dp)) {
                Icon(icon, contentDescription = null, tint = iconContainerColor, modifier = Modifier.padding(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

---

## Dark Theme

### Per [M3 Dark Theme Guidelines](https://m3.material.io/styles/color/static/baseline)

Rules:

1. **Never** hardcode `Color.White`, `Color.Black`, or hex colors for text/backgrounds
2. **Always** use `MaterialTheme.colorScheme` tokens — they auto-adapt
3. **Use `.copy(alpha = ...)` for de-emphasis** — not separate "light" color values
4. **Test both themes** — every screen must look correct in light AND dark
5. **Elevated surfaces** in dark mode are tinted (not lighter) — M3 handles this via tonal elevation

```kotlin
// GOOD — theme-aware everywhere
Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) { /* card */ }
Text(text, color = MaterialTheme.colorScheme.onSurface)
Text(secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
Divider(color = MaterialTheme.colorScheme.outlineVariant)

// BAD — will break in dark mode
Surface(color = Color(0xFFF5F5F5)) { /* hardcoded light gray */ }
Text(text, color = Color(0xFF333333)) { /* hardcoded dark gray */ }
```

---

## Visual Polish Checklist

Before shipping any new screen, verify:

### Layout & Hierarchy

- [ ] At least 3 distinct typography styles visible (creates scannable hierarchy)
- [ ] All spacing follows 4/8/12/16/24/32dp grid
- [ ] Content has 16dp horizontal screen margins
- [ ] Bottom content padding accounts for FAB/bottom nav (≥88dp)
- [ ] Sections separated by 24dp, items within section by 12dp

### Components

- [ ] Card corners use `RoundedCornerShape(12.dp)` consistently
- [ ] Text field corners use `RoundedCornerShape(12.dp)` consistently
- [ ] Only 1 filled primary button per visible area
- [ ] FAB positioned bottom-end (M3 standard)
- [ ] Top app bar uses correct variant (Small/Medium/Large)
- [ ] Zero emojis in UI — use Material Icons exclusively

### Color & Theme

- [ ] Zero hardcoded colors — all `MaterialTheme.colorScheme.*`
- [ ] Status shown with icon + text + color (never color alone)
- [ ] Dark theme tested and correct
- [ ] Dynamic color (Material You) supported with fallback scheme

### States

- [ ] Loading: skeleton placeholders matching content shape
- [ ] Empty: icon + headline + description + CTA button
- [ ] Error: icon + message + retry action when applicable
- [ ] Success: Snackbar confirmation for create/update actions
- [ ] Delete: Snackbar with "Undo" for reversible actions

### Motion

- [ ] Navigation transitions (slide + fade)
- [ ] Content appearance animation for lists
- [ ] `animateContentSize` for expandable sections
- [ ] Value changes animated (counters, progress)
- [ ] No janky recompositions during animation

### Forms

- [ ] Fields grouped by topic with section headers
- [ ] Correct keyboard type per field
- [ ] IME action advances to next field (`ImeAction.Next`)
- [ ] Inline validation with `supportingText`
- [ ] Save button in top bar (form screens) or bottom (dialogs)

### Accessibility (cross-reference with accessibility skill)

- [ ] All interactive elements ≥48dp touch target
- [ ] `contentDescription` on all icons in buttons
- [ ] Color is never the sole indicator of state
- [ ] Contrast meets WCAG AA (4.5:1 text, 3:1 large text/icons)
