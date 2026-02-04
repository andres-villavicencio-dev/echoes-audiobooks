package com.echoesapp.audiobooks.ui.screens.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.echoesapp.audiobooks.ui.theme.EchoesTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsButton_isDisplayed() {
        composeTestRule.setContent {
            EchoesTheme {
                LibraryScreenTopBar(onNavigateToSettings = {})
            }
        }

        composeTestRule
            .onNodeWithTag("settings_button")
            .assertIsDisplayed()
    }

    @Test
    fun settingsButton_hasCorrectContentDescription() {
        composeTestRule.setContent {
            EchoesTheme {
                LibraryScreenTopBar(onNavigateToSettings = {})
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun settingsButton_triggersCallback_onClick() {
        var clicked = false

        composeTestRule.setContent {
            EchoesTheme {
                LibraryScreenTopBar(onNavigateToSettings = { clicked = true })
            }
        }

        composeTestRule
            .onNodeWithTag("settings_button")
            .performClick()

        assertTrue("Settings callback should be triggered", clicked)
    }

    @Test
    fun appTitle_isDisplayed() {
        composeTestRule.setContent {
            EchoesTheme {
                LibraryScreenTopBar(onNavigateToSettings = {})
            }
        }

        composeTestRule
            .onNodeWithText("Echoes")
            .assertIsDisplayed()
    }
}
