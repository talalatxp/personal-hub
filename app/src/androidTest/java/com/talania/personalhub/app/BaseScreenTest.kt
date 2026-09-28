package com.talania.personalhub.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.talania.personalhub.core.designsystem.HubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BaseScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun firstLaunchOpensPrivateStorage() {
        compose.onNodeWithText("Personal Hub").assertIsDisplayed()
        compose.waitUntil(10_000) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasText("Base local preparada"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Base local preparada").assertIsDisplayed()
    }

    @Test fun storageErrorHasExplicitRetry() {
        var retries = 0
        compose.activity.setContentForTest {
            HubTheme { BaseScreen(BootstrapState.StorageError) { retries++ } }
        }
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }
}
