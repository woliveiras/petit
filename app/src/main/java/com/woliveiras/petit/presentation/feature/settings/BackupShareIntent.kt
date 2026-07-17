package com.woliveiras.petit.presentation.feature.settings

import android.content.ClipData
import android.content.Intent
import android.net.Uri

/** Creates a share intent that grants compatible apps temporary read access to a backup URI. */
fun createBackupShareIntent(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    type = "application/json"
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newRawUri("petit_backup", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
