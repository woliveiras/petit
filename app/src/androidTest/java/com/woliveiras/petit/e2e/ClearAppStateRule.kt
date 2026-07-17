package com.woliveiras.petit.e2e

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Clears persisted app state before an E2E Activity is launched. */
class ClearAppStateRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement =
    object : Statement() {
      override fun evaluate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        context.databaseList().forEach(context::deleteDatabase)
        context.filesDir.resolve("datastore").deleteRecursively()
        context.cacheDir.deleteRecursively()

        base.evaluate()
      }
    }
}
