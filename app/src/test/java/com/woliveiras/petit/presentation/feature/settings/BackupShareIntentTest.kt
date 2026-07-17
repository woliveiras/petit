package com.woliveiras.petit.presentation.feature.settings

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupShareIntentTest {

  @Test
  fun sharesBackupUriWithClipDataAndTemporaryReadPermission() {
    val uri = Uri.parse("content://com.woliveiras.petit.backups/petit_backup.json")

    val intent = createBackupShareIntent(uri)

    assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
    assertThat(intent.type).isEqualTo("application/json")
    assertThat(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).isEqualTo(uri)
    assertThat(intent.clipData?.getItemAt(0)?.uri).isEqualTo(uri)
    assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
  }
}
