package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.domain.AgentRole
import com.example.domain.UiState
import com.example.ui.screens.CollaborationScreen
import com.example.ui.theme.ZaytharTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h1600dp")
class CollaborationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun teamPickerLimitsSelectionToFourAndPassesTaskAndRolesToCallback() {
        var submittedTask = ""
        var submittedAgents = emptyList<AgentRole>()

        composeRule.setContent {
            ZaytharTheme {
                CollaborationScreen(
                    agentRunState = UiState.Idle,
                    knowledgeFactsState = UiState.Success(emptyList()),
                    onAgentNodeClick = {},
                    onRunTeam = { task, agents ->
                        submittedTask = task
                        submittedAgents = agents
                    },
                    onAddFact = { _, _, _ -> },
                    onDeleteFact = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("agent_task_input").performTextInput("Research safe multi-agent orchestration")
        composeRule.onNodeWithTag("specialist_pick_writing").performClick()
        composeRule.onNodeWithTag("specialist_pick_creative").performClick()
        composeRule.onNodeWithTag("run_agent_team_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals("Research safe multi-agent orchestration", submittedTask)
        assertEquals(4, submittedAgents.size)
        assertTrue(AgentRole.WRITING in submittedAgents)
        assertTrue(AgentRole.CREATIVE !in submittedAgents)
        assertEquals(submittedAgents.distinct(), submittedAgents)
    }

    @Test
    fun runningStateDisablesTeamSubmission() {
        composeRule.setContent {
            ZaytharTheme {
                CollaborationScreen(
                    agentRunState = UiState.Loading,
                    knowledgeFactsState = UiState.Success(emptyList()),
                    onAgentNodeClick = {},
                    onRunTeam = { _, _ -> },
                    onAddFact = { _, _, _ -> },
                    onDeleteFact = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("run_agent_team_button").assertIsDisplayed()
        composeRule.onNodeWithTag("run_agent_team_button").assertIsNotEnabled()
    }
}
