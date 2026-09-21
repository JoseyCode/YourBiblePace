package com.example.biblepaceproject

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the real app on a device/emulator: launches fresh, switches versions, navigates chapters. */
@RunWith(AndroidJUnit4::class)
class ReaderFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchFresh() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteDatabase(com.example.biblepaceproject.data.AppDatabase.NAME)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() = scenario.close()

    private fun waitForText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount(text) > 0 }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextCount(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text, substring = true)).fetchSemanticsNodes().size

    @Test
    fun opensOnGenesisOneInTheKingJames() {
        waitForText("the heaven and the earth")
        compose.onNodeWithText("KJV").assertExists()
    }

    @Test
    fun switchingVersionChangesTheText() {
        waitForText("the heaven and the earth")
        compose.onNodeWithText("KJV").performClick()
        compose.onNodeWithText("American Standard Version (ASV)").performClick()
        waitForText("the heavens and the earth")
        compose.onNodeWithText("ASV").assertExists()
    }

    @Test
    fun nextChapterAdvances() {
        waitForText("Genesis 1")
        compose.onNodeWithText("›").performClick()
        waitForText("Genesis 2")
    }
}
