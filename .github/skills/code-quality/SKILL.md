---
name: code-quality
description: "Code quality patterns and conventions for Petit Android app. Use when: writing new Kotlin code, creating ViewModels, implementing repositories, building Compose UI, registering Hilt modules, writing Room entities/DAOs, structuring packages, following naming conventions, formatting code. Covers MVVM pattern, Hilt DI, Room patterns, Compose conventions, Kotlin idioms."
---

# Code Quality — Petit Android

## When to Use

- Creating new features (ViewModel, Screen, Repository, Entity, DAO)
- Refactoring existing code
- Reviewing code for convention compliance
- Setting up dependency injection
- Writing data layer code (Room, DataStore)

## Architecture Rules

### MVVM + Repository Pattern

```
Screen (Composable) → ViewModel → Repository → DAO → Room DB
```

**Every feature must have:**

1. **UiState** data class — immutable, with defaults
2. **ViewModel** — `@HiltViewModel`, exposes `StateFlow<UiState>` and `SharedFlow<Event>`
3. **Screen** — `@Composable`, collects state with `collectAsStateWithLifecycle()`
4. **Repository** — Interface + Impl, `@Singleton` scope

### Package Structure

```
com.woliveiras.petit/
├── data/
│   ├── local/
│   │   ├── db/          → Room database, entities
│   │   ├── dao/         → DAO interfaces
│   │   └── datastore/   → DataStore preferences
│   └── repository/      → Repository interfaces + implementations
├── domain/
│   ├── model/           → Pure domain models (no Android deps)
│   └── usecase/         → Use cases (when business logic is complex)
├── presentation/
│   ├── feature/{name}/  → Screen + ViewModel + UiState per feature
│   ├── components/      → Shared composables
│   ├── navigation/      → Nav graph, routes, bottom bar
│   ├── theme/           → Colors, typography, theme
│   └── util/            → Presentation utilities
├── worker/              → WorkManager workers
├── di/                  → Hilt modules
└── PetitApplication.kt   → Application entry point
```

## ViewModel Pattern

### Standard ViewModel

```kotlin
@HiltViewModel
class CatDetailViewModel @Inject constructor(
    private val catRepository: CatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val catId: String = checkNotNull(savedStateHandle["catId"])

    private val _uiState = MutableStateFlow(CatDetailUiState())
    val uiState: StateFlow<CatDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CatDetailEvent>()
    val events: SharedFlow<CatDetailEvent> = _events.asSharedFlow()

    init {
        loadCat()
    }

    private fun loadCat() {
        viewModelScope.launch {
            catRepository.getCatById(catId)?.let { cat ->
                _uiState.update { it.copy(cat = cat, isLoading = false) }
            }
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            catRepository.softDeleteCat(catId)
            _events.emit(CatDetailEvent.NavigateBack)
        }
    }
}

data class CatDetailUiState(
    val cat: Cat? = null,
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
)

sealed interface CatDetailEvent {
    data object NavigateBack : CatDetailEvent
}
```

### Rules:

- `@HiltViewModel` annotation is mandatory
- Use `SavedStateHandle` for navigation arguments
- `StateFlow` for UI state, `SharedFlow` for one-time events
- `_uiState.update { ... }` for thread-safe state updates
- `viewModelScope.launch` for coroutines (NEVER `GlobalScope`)
- **ONE `StateFlow<UiState>` per ViewModel** — never two separate StateFlows (see State Management Rules below)
- **Inject Repositories only** — never inject DAOs directly into ViewModels

## ViewModel State Management Rules

> These rules prevent state desynchronization bugs and make debugging predictable.

### Rule 1: Single StateFlow per ViewModel — ALWAYS

Never use two `MutableStateFlow` in the same ViewModel. Dual flows can desync when one updates independently.

```kotlin
// BAD — two flows can get out of sync
@HiltViewModel
class VaccinationViewModel @Inject constructor(...) : ViewModel() {
    private val _listState = MutableStateFlow(ListUiState())
    private val _formState = MutableStateFlow(FormUiState())
    // ⚠️ When editing, form reads from _listState.value — stale data risk
}

// GOOD — single sealed interface with screen modes
@HiltViewModel
class VaccinationViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow<VaccinationUiState>(VaccinationUiState.Loading)
    val uiState: StateFlow<VaccinationUiState> = _uiState.asStateFlow()
}
```

### Rule 2: Use Sealed Interface for Multi-Mode Screens

When a screen has distinct modes (list, form, detail), use a `sealed interface`:

```kotlin
sealed interface VaccinationUiState {
    data object Loading : VaccinationUiState
    data class ListMode(
        val catName: String,
        val entries: List<VaccinationEntry>,
    ) : VaccinationUiState
    data class FormMode(
        val catName: String,
        val entries: List<VaccinationEntry>, // shared data stays in sync
        val form: VaccinationFormFields,
        val isEditMode: Boolean = false,
        val isSaving: Boolean = false,
    ) : VaccinationUiState
    data class Error(val message: String) : VaccinationUiState
}

// Transition between modes — data is always consistent
fun openForm(entryId: String? = null) {
    val current = _uiState.value as? VaccinationUiState.ListMode ?: return
    val form = if (entryId != null) {
        current.entries.find { it.id == entryId }?.toFormFields() ?: VaccinationFormFields()
    } else VaccinationFormFields()
    _uiState.value = VaccinationUiState.FormMode(
        catName = current.catName,
        entries = current.entries,
        form = form,
        isEditMode = entryId != null,
    )
}
```

### Rule 3: All Mutable State Inside UiState — No Exceptions

Never store mutable state as a ViewModel property outside of UiState:

```kotlin
// BAD — state leaks outside UiState, lost on ViewModel recreation
class ExportViewModel : ViewModel() {
    private var pendingBundle: ExportBundle? = null  // ⚠️ invisible to UI, not in StateFlow
}

// GOOD — everything in UiState
data class ExportUiState(
    val pendingBundle: ExportBundle? = null,
    val isExporting: Boolean = false,
)
```

### Rule 4: Always Reset Loading Flags with `finally`

If you set `isSaving = true` or `isLoading = true`, always reset it in a `finally` block:

```kotlin
// BAD — if emit() throws, isSaving stays true forever
fun save() {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        try {
            repository.save(entity)
            _events.emit(Event.Saved)
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(Event.Error(e.message))
        }
    }
}

// GOOD — finally guarantees flag reset
fun save() {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        try {
            repository.save(entity)
            _events.emit(Event.Saved)
        } catch (e: Exception) {
            _events.emit(Event.Error(e.message ?: "Erro ao salvar"))
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
```

### Rule 5: Inject Repositories Only — Never DAOs in ViewModels

ViewModels must depend on Repository interfaces, never on DAO classes directly:

```kotlin
// BAD — bypasses repository layer, breaks abstraction
@HiltViewModel
class MyViewModel @Inject constructor(
    private val catDao: CatDao,           // ⚠️ DAO in ViewModel
    private val weightEntryDao: WeightEntryDao,  // ⚠️ DAO in ViewModel
) : ViewModel()

// GOOD — depends on repository abstraction
@HiltViewModel
class MyViewModel @Inject constructor(
    private val catRepository: CatRepository,
    private val weightEntryRepository: WeightEntryRepository,
) : ViewModel()
```

### Rule 6: Handle External Side Effects Defensively

When a ViewModel calls external services (WorkManager, scheduler, file I/O) after a repository write, wrap both in proper error handling:

```kotlin
// BAD — DB saved but scheduler can fail silently
fun saveReminder() {
    viewModelScope.launch {
        reminderRepository.save(reminder)        // ✅ saved in DB
        reminderScheduler.schedule(reminder)     // ❌ if this fails, no notification
        _events.emit(ReminderEvent.Saved)        // UI thinks everything worked
    }
}

// GOOD — both operations in same try, rollback or notify on failure
fun saveReminder() {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        try {
            reminderRepository.save(reminder)
            try {
                reminderScheduler.schedule(reminder)
            } catch (e: Exception) {
                // DB saved but schedule failed — notify user
                _events.emit(ReminderEvent.Warning("Lembrete salvo, mas notificação não agendada"))
                return@launch
            }
            _events.emit(ReminderEvent.Saved)
        } catch (e: Exception) {
            _events.emit(ReminderEvent.Error(e.message ?: "Erro ao salvar"))
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
```

## Repository Pattern

### Interface

```kotlin
interface CatRepository {
    fun getAllCats(): Flow<List<Cat>>
    suspend fun getCatById(id: String): Cat?
    suspend fun insertCat(cat: Cat)
    suspend fun updateCat(cat: Cat)
    suspend fun softDeleteCat(id: String)
}
```

### Implementation

```kotlin
@Singleton
class CatRepositoryImpl @Inject constructor(
    private val catDao: CatDao,
) : CatRepository {

    override fun getAllCats(): Flow<List<Cat>> =
        catDao.getAllCats().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun getCatById(id: String): Cat? =
        catDao.getCatById(id)?.toDomainModel()

    override suspend fun insertCat(cat: Cat) =
        catDao.insertCat(cat.toEntity())

    override suspend fun softDeleteCat(id: String) =
        catDao.softDeleteCat(id)
}
```

### Rules:

- Interface + `Impl` class always
- `@Singleton` scope on implementation
- `Flow<T>` for observable data, `suspend` for one-shot operations
- Map between Entity ↔ Domain Model at the repository layer
- Never expose Room entities outside `data/` layer

## Room Database Patterns

### Entity

```kotlin
@Entity(
    tableName = "weight_entries",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WeightEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val catId: String,
    val weightGrams: Int,
    val measuredAt: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY"
)
```

### DAO

```kotlin
@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries WHERE catId = :catId AND deletedAt IS NULL ORDER BY measuredAt DESC")
    fun getWeightEntriesByCat(catId: String): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Query("UPDATE weight_entries SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}
```

### Rules:

- **Always** include `deletedAt IS NULL` in SELECT queries (soft delete)
- **Always** include `createdAt`, `updatedAt`, `deletedAt`, `syncStatus` fields
- UUID for `@PrimaryKey` (String type)
- `ForeignKey` with `CASCADE` for parent-child relationships
- `Flow<List<T>>` for reactive queries
- `suspend` for insert/update/delete operations

## Hilt Dependency Injection

### Database Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PetitDatabase =
        Room.databaseBuilder(context, PetitDatabase::class.java, "petit-database")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideCatDao(database: PetitDatabase): CatDao = database.catDao()
}
```

### Repository Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCatRepository(impl: CatRepositoryImpl): CatRepository
}
```

### Rules:

- `DatabaseModule`: `@Provides` + `object`
- `RepositoryModule`: `@Binds` + `abstract class`
- `@Singleton` scope for database and repositories
- `@InstallIn(SingletonComponent::class)` for app-wide singletons

## Compose UI Conventions

### Screen Composable

```kotlin
@Composable
fun CatListScreen(
    viewModel: CatListViewModel = hiltViewModel(),
    onNavigateToCat: (String) -> Unit,
    onNavigateToAddCat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CatListEvent.NavigateToCat -> onNavigateToCat(event.catId)
            }
        }
    }

    CatListContent(
        uiState = uiState,
        onAddCat = onNavigateToAddCat,
        modifier = modifier,
    )
}
```

### Rules:

- `collectAsStateWithLifecycle()` for ViewModel state
- `LaunchedEffect(Unit)` for one-time event collection
- Navigation callbacks as lambda parameters
- `modifier: Modifier = Modifier` as last parameter
- Separate `Screen` (stateful) from `Content` (stateless) when complex

## Naming Conventions

| Component  | Pattern                                     | Example             |
| ---------- | ------------------------------------------- | ------------------- |
| Entity     | `{Name}Entity`                              | `CatEntity`         |
| DAO        | `{Name}Dao`                                 | `CatDao`            |
| Repository | `{Name}Repository` / `{Name}RepositoryImpl` | `CatRepositoryImpl` |
| ViewModel  | `{Feature}ViewModel`                        | `CatFormViewModel`  |
| UiState    | `{Feature}UiState`                          | `CatFormUiState`    |
| Screen     | `{Feature}Screen`                           | `CatFormScreen`     |
| Worker     | `{Name}Worker`                              | `ReminderWorker`    |
| Nav Route  | `Screen.{Name}`                             | `Screen.CatDetail`  |

## Kotlin Idioms

- **`data class`** for models and state — always
- **`sealed interface`** for events and navigation routes
- **`?.let { }`** for nullable mapping
- **`?:`** (elvis) for default values
- **`Flow.map { }`** for transforming streams
- **`viewModelScope.launch { }`** for async work
- **No `!!`** — use `checkNotNull()` or `requireNotNull()` with message instead
- **Extension functions** for entity ↔ domain model mapping
