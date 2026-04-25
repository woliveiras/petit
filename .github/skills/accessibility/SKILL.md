---
name: accessibility
description: "Accessibility patterns for Petit Android app with Jetpack Compose. Use when: adding contentDescription to icons and images, implementing TalkBack support, ensuring touch target sizes, managing semantic structure, handling state announcements (loading/error/empty), setting traversal order, grouping related elements, implementing color contrast, writing accessible forms, creating accessible custom components."
---

# Accessibility — Petit Android

## When to Use

- Creating new UI composables
- Adding icons, buttons, or interactive elements
- Building forms and input fields
- Handling loading, error, and empty states
- Creating custom components
- Implementing navigation elements
- Reviewing existing UI for accessibility gaps

## Core Principles

1. **All interactive elements must be reachable by TalkBack**
2. **All actions must have meaningful descriptions**
3. **All states must be announced to screen readers**
4. **Minimum 48dp touch target for interactive elements**
5. **Logical reading order for screen reader navigation**

Reference the full testing guide at `docs/talkback-testing-guide.md`.

## Content Descriptions

### Icons and Icon Buttons

```kotlin
// GOOD — descriptive contentDescription
IconButton(onClick = onNavigateToSettings) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Configurações"
    )
}

// GOOD — decorative icon (no description needed)
Icon(
    imageVector = Icons.Default.Pets,
    contentDescription = null // purely decorative
)

// BAD — missing contentDescription
IconButton(onClick = onDelete) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = null // user can't know what this button does!
    )
}

// BAD — generic description
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = "Ícone" // not helpful!
)
```

### Rules:

- **Interactive icons** (in `IconButton`): ALWAYS provide `contentDescription`
- **Decorative icons**: Set `contentDescription = null` explicitly
- **Descriptions**: Use the action, not the visual (e.g., "Excluir gato" not "Ícone de lixeira")
- **Language**: Portuguese (pt-BR) for all descriptions, matching the app language

### Images

```kotlin
// Cat photo with description
AsyncImage(
    model = cat.photoUri,
    contentDescription = "Foto de ${cat.name}",
    modifier = Modifier.size(80.dp)
)

// Placeholder image
Image(
    painter = painterResource(R.drawable.cat_placeholder),
    contentDescription = null // decorative placeholder
)
```

## Touch Targets

### Minimum Size: 48dp x 48dp

```kotlin
// GOOD — ensuring minimum touch target
IconButton(
    onClick = onDelete,
    modifier = Modifier.size(48.dp) // meets minimum
) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Excluir",
        modifier = Modifier.size(24.dp) // icon is smaller, button is 48dp
    )
}

// GOOD — using Modifier.sizeIn for minimum
Text(
    text = "Link",
    modifier = Modifier
        .clickable { onLinkClick() }
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .padding(12.dp)
)

// BAD — touch target too small
IconButton(
    onClick = onEdit,
    modifier = Modifier.size(24.dp) // too small for accessibility!
) {
    Icon(Icons.Default.Edit, contentDescription = "Editar")
}
```

### Rules:

- `IconButton` already has 48dp minimum by default — don't override it smaller
- Custom clickable elements: use `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`
- Add padding around small icons to expand the touch area

## Semantic Structure

### Grouping Related Elements

```kotlin
// GOOD — group card as single semantic element
Card(
    modifier = Modifier
        .semantics(mergeDescendants = true) {
            contentDescription = "${cat.name}, ${cat.age}, ${cat.healthStatus}"
        }
        .clickable { onCatClick(cat.id) }
) {
    Text(text = cat.name)
    Text(text = cat.age)
    Text(text = cat.healthStatus)
}

// GOOD — clearAndSetSemantics for custom announcement
Row(
    modifier = Modifier.clearAndSetSemantics {
        contentDescription = "Peso: ${weight}g, registrado em $date"
    }
) {
    Icon(Icons.Default.Scale, contentDescription = null)
    Text("${weight}g")
    Text(date)
}
```

### Traversal Order

```kotlin
// GOOD — control reading order
Box {
    // Header reads first
    Text(
        text = "Perfil do Pet",
        modifier = Modifier.semantics { traversalIndex = 0f }
    )

    // Content reads second
    CatInfo(
        modifier = Modifier.semantics { traversalIndex = 1f }
    )

    // FAB reads last
    FloatingActionButton(
        onClick = onAdd,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .semantics { traversalIndex = 2f }
    ) {
        Icon(Icons.Default.Add, contentDescription = "Adicionar registro")
    }
}
```

### Rules:

- **`mergeDescendants = true`**: Group related text elements in cards/rows
- **`clearAndSetSemantics`**: Override all child semantics with a single description
- **`traversalIndex`**: Control reading order when visual order differs from logical order
- **Don't over-merge**: Keep separate interactive elements independently focusable

## State Announcements

### Loading State

```kotlin
@Composable
fun LoadingIndicator(message: String = "Carregando...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
```

### Error State

```kotlin
@Composable
fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive // important, interrupt
            error(message)
        }
    )
}
```

### Empty State

```kotlin
@Composable
fun EmptyState(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null, // decorative, text below provides meaning
            modifier = Modifier.size(64.dp)
        )
        Text(text = message)
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
```

### Rules:

- **`LiveRegionMode.Polite`**: For non-urgent updates (loading complete, data refreshed)
- **`LiveRegionMode.Assertive`**: For urgent updates (errors, critical alerts)
- **Error semantics**: Use `error()` property for form validation errors
- **Empty states**: Always provide an action, announced by TalkBack

## Accessible Forms

### Text Fields

```kotlin
// GOOD — with label, error, and helper text
OutlinedTextField(
    value = name,
    onValueChange = onNameChange,
    label = { Text("Nome do gato") },
    isError = nameError != null,
    supportingText = nameError?.let { { Text(it) } },
    modifier = Modifier.fillMaxWidth()
)

// Material 3 OutlinedTextField handles accessibility automatically:
// - Announces label
// - Announces error state
// - Announces supporting text
```

### Dropdowns

```kotlin
// GOOD — accessible dropdown
ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    OutlinedTextField(
        value = selectedOption,
        onValueChange = {},
        readOnly = true,
        label = { Text("Sexo") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayName) },
                onClick = {
                    onOptionSelected(option)
                    expanded = false
                }
            )
        }
    }
}
```

### Date Pickers

```kotlin
// GOOD — announce purpose
Button(
    onClick = { showDatePicker = true },
    modifier = Modifier.semantics {
        contentDescription = if (selectedDate != null) {
            "Data de nascimento: $formattedDate. Toque para alterar"
        } else {
            "Selecionar data de nascimento"
        }
    }
) {
    Text(selectedDate?.let { formattedDate } ?: "Selecionar data")
}
```

## Dialogs and Confirmations

```kotlin
// GOOD — accessible confirmation dialog
AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Excluir ${cat.name}?") },
    text = { Text("Esta ação não pode ser desfeita. Todos os dados do gato serão removidos.") },
    confirmButton = {
        TextButton(onClick = onConfirm) {
            Text("Excluir", color = MaterialTheme.colorScheme.error)
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancelar")
        }
    }
)
// AlertDialog handles focus management and announcements automatically
```

## Charts and Data Visualization

```kotlin
// GOOD — provide text alternative for charts
Column {
    // Chart (visual)
    WeightChart(
        entries = weightEntries,
        modifier = Modifier
            .height(200.dp)
            .semantics {
                contentDescription = buildString {
                    append("Gráfico de peso. ")
                    append("${weightEntries.size} registros. ")
                    if (weightEntries.isNotEmpty()) {
                        append("Peso mais recente: ${weightEntries.first().weightGrams}g. ")
                        append("Período: ${firstDate} a ${lastDate}.")
                    }
                }
            }
    )

    // Data table (accessible alternative)
    weightEntries.forEach { entry ->
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "${entry.formattedDate}: ${entry.weightGrams} gramas"
            }
        ) {
            Text(entry.formattedDate)
            Text("${entry.weightGrams}g")
        }
    }
}
```

## Color and Contrast

### WCAG AA Minimum Contrast

| Element                          | Minimum Ratio |
| -------------------------------- | ------------- |
| Normal text                      | 4.5:1         |
| Large text (18sp+ or 14sp+ bold) | 3:1           |
| Icons and UI components          | 3:1           |

### Rules:

- Use `MaterialTheme.colorScheme` tokens — they ensure contrast in both light and dark themes
- **Never** use color alone to convey information (add text labels or icons)
- Health status indicators: Use icon + text + color (not just color)

```kotlin
// GOOD — color + text + icon
Row {
    Icon(
        imageVector = when (status) {
            HealthStatus.OK -> Icons.Default.CheckCircle
            HealthStatus.WARNING -> Icons.Default.Warning
            HealthStatus.OVERDUE -> Icons.Default.Error
        },
        contentDescription = null, // text provides the info
        tint = statusColor
    )
    Text(
        text = when (status) {
            HealthStatus.OK -> "Em dia"
            HealthStatus.WARNING -> "Atenção necessária"
            HealthStatus.OVERDUE -> "Atrasado"
        },
        color = statusColor
    )
}

// BAD — color only
Box(
    modifier = Modifier
        .size(12.dp)
        .background(statusColor, CircleShape)
    // Red dot means nothing to color-blind or screen reader users!
)
```

## Bottom Navigation

```kotlin
// Material 3 NavigationBar handles accessibility:
NavigationBar {
    NavigationBarItem(
        selected = currentRoute == Screen.Home.route,
        onClick = { onNavigate(Screen.Home) },
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("Home") } // label provides the description
    )
}
// The label + selected state are announced automatically
```

## Accessibility Checklist

Before merging any UI change, verify:

### Critical (Must Pass)

- [ ] All buttons and interactive elements are activatable via TalkBack double-tap
- [ ] Navigation is complete — user can reach all screens via TalkBack
- [ ] All form fields are accessible with labels announced
- [ ] Destructive actions (delete) have clear announcements

### Important (Should Pass)

- [ ] Loading, success, and error states are announced
- [ ] Reading order is logical (follows visual flow)
- [ ] Related elements are grouped (`mergeDescendants`)
- [ ] Touch targets are minimum 48dp

### Desirable (Nice to Have)

- [ ] Rich descriptions with contextual information
- [ ] Charts have text alternatives
- [ ] Custom accessibility actions where appropriate
