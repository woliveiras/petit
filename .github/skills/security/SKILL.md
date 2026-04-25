---
name: security
description: "Security best practices for Petit Android app. Use when: handling user input, working with Room database queries, managing file I/O (photo storage, export/import), implementing authentication (Firebase Auth), storing sensitive data (keystore, DataStore), configuring ProGuard, validating data boundaries, implementing export/import functionality, working with notifications, handling deep links."
---

# Security — Petit Android

## When to Use

- Accepting user input (forms, text fields, date pickers)
- Writing Room queries or database operations
- Implementing file operations (photo upload, JSON export/import)
- Working with authentication (future Firebase Auth)
- Storing sensitive data (preferences, credentials)
- Configuring build signing and ProGuard
- Handling notifications and intents
- Processing external data (import from file)

## Input Validation

### Form Validation Rules

All user input MUST be validated before reaching the database layer.

```kotlin
// GOOD — validate at ViewModel layer before saving
fun onSaveCat(name: String, notes: String?) {
    val trimmedName = name.trim()

    if (trimmedName.isEmpty()) {
        _uiState.update { it.copy(nameError = "Nome é obrigatório") }
        return
    }
    if (trimmedName.length > 50) {
        _uiState.update { it.copy(nameError = "Máximo 50 caracteres") }
        return
    }
    if (notes != null && notes.length > 500) {
        _uiState.update { it.copy(notesError = "Máximo 500 caracteres") }
        return
    }

    viewModelScope.launch {
        repository.insertCat(Cat(name = trimmedName, notes = notes?.trim()))
    }
}

// BAD — no validation
fun onSaveCat(name: String) {
    viewModelScope.launch {
        repository.insertCat(Cat(name = name)) // raw input to DB!
    }
}
```

### Validation Boundaries

| Field            | Type    | Max Length | Additional Rules      |
| ---------------- | ------- | ---------- | --------------------- |
| Cat name         | String  | 50         | Non-empty after trim  |
| Notes            | String? | 500        | Nullable              |
| Microchip number | String? | 50         | Alphanumeric only     |
| Passport number  | String? | 50         | Alphanumeric only     |
| Weight (grams)   | Int     | N/A        | > 0, < 50000 (50kg)   |
| Birth date       | Long?   | N/A        | Not in the future     |
| Photo            | URI     | N/A        | Max 5MB, JPG/PNG only |

### Date Validation

```kotlin
fun isValidBirthDate(dateMillis: Long?): Boolean {
    if (dateMillis == null) return true // optional field
    return dateMillis <= System.currentTimeMillis()
}
```

## SQL Injection Prevention

### Room Parameterized Queries

Room uses parameterized queries by design, but you MUST follow these rules:

```kotlin
// GOOD — parameterized query (Room handles escaping)
@Query("SELECT * FROM cats WHERE id = :id AND deletedAt IS NULL")
suspend fun getCatById(id: String): CatEntity?

// GOOD — parameterized with multiple params
@Query("SELECT * FROM weight_entries WHERE catId = :catId AND measuredAt BETWEEN :start AND :end AND deletedAt IS NULL")
fun getWeightRange(catId: String, start: Long, end: Long): Flow<List<WeightEntryEntity>>

// BAD — NEVER concatenate strings in queries
@Query("SELECT * FROM cats WHERE name = '$name'") // SQL INJECTION RISK!
suspend fun findByName(name: String): CatEntity?

// BAD — NEVER use @RawQuery with user input
@RawQuery
suspend fun rawQuery(query: SupportSQLiteQuery): List<CatEntity> // DANGEROUS
```

### Rules:

- **Always** use `:paramName` syntax in `@Query`
- **Never** concatenate user input into query strings
- **Avoid** `@RawQuery` unless absolutely necessary (and never with user-controlled input)
- **Use** `OnConflictStrategy.REPLACE` or `IGNORE` for insert operations

## File I/O Security

### Photo Storage

```kotlin
// GOOD — store in app-internal directory
val photoFile = File(context.filesDir, "photos/${UUID.randomUUID()}.jpg")

// GOOD — validate file size before saving
fun validatePhoto(uri: Uri, context: Context): Boolean {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return false
    val bytes = inputStream.available()
    inputStream.close()
    return bytes <= 5 * 1024 * 1024 // 5MB max
}

// GOOD — validate MIME type
fun isValidImageType(uri: Uri, context: Context): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    return mimeType in listOf("image/jpeg", "image/png")
}

// BAD — storing in external public directory
val photoFile = File(Environment.getExternalStorageDirectory(), "photos/pet.jpg")
```

### Export/Import JSON

```kotlin
// GOOD — validate JSON structure before importing
fun validateImportBundle(json: String): Result<ExportBundle> {
    return runCatching {
        val bundle = gson.fromJson(json, ExportBundle::class.java)
        requireNotNull(bundle.cats) { "Missing cats field" }
        requireNotNull(bundle.exportedAt) { "Missing exportedAt field" }

        // Validate each entity
        bundle.cats.forEach { cat ->
            require(cat.name.isNotBlank()) { "Cat name cannot be blank" }
            require(cat.name.length <= 50) { "Cat name too long" }
        }

        bundle
    }
}

// BAD — blindly trusting imported data
fun importData(json: String) {
    val bundle = gson.fromJson(json, ExportBundle::class.java)
    bundle.cats.forEach { repository.insertCat(it) } // NO VALIDATION!
}
```

### Rules:

- **Validate** file size, MIME type, and content before processing
- **Store** user files in `context.filesDir` (internal storage), not external
- **Sanitize** imported data as if it were user input — validate all fields
- **Limit** file sizes to prevent DoS (5MB for photos, reasonable limit for JSON)

## Secrets Management

### Build Configuration

```kotlin
// GOOD — keystore in properties file (not committed)
// keystore.properties (in .gitignore)
storeFile=release.keystore
storePassword=****
keyAlias=****
keyPassword=****

// build.gradle.kts reads from properties
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(FileInputStream(file))
}

// BAD — hardcoded in build.gradle.kts
signingConfigs {
    create("release") {
        storePassword = "mysecretpassword" // NEVER DO THIS
    }
}
```

### Rules:

- **Never** commit `keystore.properties`, `local.properties`, or any file with secrets
- **Provide** `keystore.properties.template` with placeholder values
- **Use** `.gitignore` to exclude secret files
- **Future Firebase**: Use `google-services.json` per the standard Firebase setup (in `.gitignore` for open-source)

## DataStore Security

```kotlin
// GOOD — use DataStore for non-sensitive preferences
val Context.settingsDataStore by preferencesDataStore(name = "settings")

// For sensitive data in the future (tokens, credentials):
// Use EncryptedSharedPreferences or Android Keystore
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

### Rules:

- **DataStore** for user preferences (theme, notification settings, etc.)
- **Android Keystore** for cryptographic keys (future auth tokens)
- **Never** store credentials in plain text DataStore

## Intent & Notification Security

### Notifications

```kotlin
// GOOD — use PendingIntent with FLAG_IMMUTABLE
val pendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

// BAD — mutable PendingIntent (security risk on Android 12+)
val pendingIntent = PendingIntent.getActivity(
    context, requestCode, intent,
    PendingIntent.FLAG_UPDATE_CURRENT // Missing FLAG_IMMUTABLE
)
```

### Rules:

- **Always** use `FLAG_IMMUTABLE` for PendingIntents unless mutability is required
- **Explicit intents** for internal navigation (never implicit for within-app)
- **Validate** extras from incoming intents before using

## ProGuard / R8

### Rules:

- Keep Room entities and DAOs (R8 usually handles via annotations)
- Keep Hilt-generated code
- Keep data classes used in JSON serialization (`@Keep` or ProGuard rules)
- Test the release build to verify nothing is stripped incorrectly

```proguard
# Keep Room entities
-keep class com.woliveiras.petit.data.local.db.entity.** { *; }

# Keep domain models used in JSON export/import
-keep class com.woliveiras.petit.domain.model.** { *; }
```

## Security Checklist

Before merging any PR, verify:

- [ ] All user inputs are validated (length, format, range)
- [ ] Room queries use parameterized syntax only
- [ ] No hardcoded secrets in source code
- [ ] File operations validate size and type
- [ ] PendingIntents use `FLAG_IMMUTABLE`
- [ ] Import/export validates data structure and field values
- [ ] No `@RawQuery` with user-controlled input
- [ ] Photos stored in internal app storage
- [ ] Release builds have ProGuard enabled
