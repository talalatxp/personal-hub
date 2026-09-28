package com.talania.personalhub.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

internal fun MainActivity.setContentForTest(content: @Composable () -> Unit) {
    runOnUiThread { setContent(content = content) }
}
