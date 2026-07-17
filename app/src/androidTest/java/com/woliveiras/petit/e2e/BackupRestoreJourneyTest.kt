package com.woliveiras.petit.e2e

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.R
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.domain.model.TaskStatus
import java.util.regex.Pattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BackupRestoreJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun exportDeleteAndImport_restoresPet() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val device = UiDevice.getInstance(instrumentation)
    val backupName = "petit_e2e_${System.currentTimeMillis()}.json"
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val edit = context.getString(R.string.action_edit)
    val profile = context.getString(R.string.nav_profile)
    val exportData = context.getString(R.string.settings_export)
    val importData = context.getString(R.string.settings_import)
    val exportSuccess = context.getString(R.string.export_success)
    val deleteAllData = context.getString(R.string.settings_delete_all_data)
    val confirmWord = context.getString(R.string.delete_all_data_confirm_word)
    val confirmInstruction = context.getString(R.string.delete_all_data_confirm_instruction)
    val confirmDelete = context.getString(R.string.pet_delete_confirm_button)
    val deletionSuccess = context.getString(R.string.delete_all_data_success)
    val goHome = context.getString(R.string.pet_delete_go_home)
    val importDialogTitle = context.getString(R.string.import_dialog_title)
    val importPetCount = context.getString(R.string.import_pets_count, 1)
    val confirm = context.getString(R.string.action_confirm)
    val importSuccess = context.getString(R.string.import_success)
    val home = context.getString(R.string.nav_home)
    val petName = "Luna"

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(getStarted).performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(registerPet).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(registerPet).performClick()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(petName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithContentDescription(edit).fetchSemanticsNodes().isNotEmpty()
    }
    val database =
      Room.databaseBuilder(context, PetitDatabase::class.java, PetitDatabase.DATABASE_NAME).build()
    val completedTaskId = "backup-completed-task"
    try {
      val petId = runBlocking { database.petDao().getAllPets().first().single().id }
      runBlocking {
        database
          .taskDao()
          .insertTask(
            TaskEntity(
              id = completedTaskId,
              petId = petId,
              kind = "CUSTOM",
              title = "Completed backup task",
              scheduledFor = System.currentTimeMillis(),
              status = TaskStatus.COMPLETED.name,
            )
          )
      }
    } finally {
      database.close()
    }

    composeRule.onNodeWithText(profile).performClick()
    composeRule.onNodeWithText(exportData).performScrollTo().performClick()
    saveDocument(device, backupName)
    dismissShareSheet(device)
    composeRule.onNodeWithText(exportSuccess).assertIsDisplayed()

    composeRule
      .onNodeWithText(deleteAllData)
      .performScrollTo()
      .performSemanticsAction(SemanticsActions.OnClick)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(confirmInstruction).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(confirmInstruction).performScrollTo().assertIsDisplayed()
    composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(confirmWord)
    closeSoftKeyboard()
    composeRule.onNodeWithText(confirmDelete).performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(deletionSuccess).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(deletionSuccess).assertIsDisplayed()
    composeRule.onNodeWithText(goHome).performClick()
    composeRule.onNodeWithText(registerPet).assertIsDisplayed()
    composeRule.onAllNodesWithText(petName).assertCountEquals(0)

    composeRule.onNodeWithText(profile).performClick()
    composeRule.onNodeWithText(importData).performScrollTo().performClick()
    openDocument(device, backupName)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(importDialogTitle).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(importDialogTitle).assertIsDisplayed()
    composeRule.onNodeWithText(importPetCount).assertIsDisplayed()
    composeRule.onNodeWithText(confirm).performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(importSuccess).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(importSuccess).assertIsDisplayed()

    composeRule.onNodeWithText(home).performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText(petName).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onAllNodesWithText(petName)[0].assertIsDisplayed()
    val restoredDatabase =
      Room.databaseBuilder(context, PetitDatabase::class.java, PetitDatabase.DATABASE_NAME).build()
    try {
      val restoredTask = runBlocking {
        restoredDatabase.taskDao().getCompletedTasks().first().single { it.id == completedTaskId }
      }
      assertThat(restoredTask.status).isEqualTo(TaskStatus.COMPLETED.name)
    } finally {
      restoredDatabase.close()
    }
  }

  private fun saveDocument(device: UiDevice, backupName: String) {
    val fileNameField =
      device.wait(Until.findObject(By.clazz("android.widget.EditText")), SYSTEM_UI_TIMEOUT)
    assertThat(fileNameField).isNotNull()
    fileNameField.text = backupName

    val saveButton =
      device.wait(
        Until.findObject(By.text(Pattern.compile("save|create", Pattern.CASE_INSENSITIVE))),
        SYSTEM_UI_TIMEOUT,
      )
    assertThat(saveButton).isNotNull()
    saveButton.click()
    device.wait(Until.gone(By.pkg("com.google.android.documentsui")), SYSTEM_UI_TIMEOUT)
  }

  private fun openDocument(device: UiDevice, backupName: String) {
    val backupFile = device.wait(Until.findObject(By.text(backupName)), SYSTEM_UI_TIMEOUT)
    assertThat(backupFile).isNotNull()
    backupFile.click()
    device.wait(Until.gone(By.pkg("com.google.android.documentsui")), SYSTEM_UI_TIMEOUT)
  }

  private fun dismissShareSheet(device: UiDevice) {
    assertThat(
        device.wait(Until.hasObject(By.pkg("com.android.intentresolver")), SYSTEM_UI_TIMEOUT)
      )
      .isTrue()
    device.pressBack()
    device.wait(Until.gone(By.pkg("com.android.intentresolver")), SYSTEM_UI_TIMEOUT)
  }

  private companion object {
    const val SYSTEM_UI_TIMEOUT = 10_000L
  }
}
