package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisAgentOrchestratorTest {
    @Test
    fun teamHasDistinctPersonasAndCapabilities() {
        assertEquals(6, JarvisAgentOrchestrator.team.size)
        assertEquals(6, JarvisAgentOrchestrator.team.map { it.id }.distinct().size)
        assertTrue(JarvisAgentOrchestrator.team.all { it.personality.isNotBlank() && it.operatingPrinciples.isNotEmpty() })
    }

    @Test
    fun researchTaskGetsSpecialistsWithoutUnnecessaryApproval() {
        val plan = JarvisAgentOrchestrator.createPlan("Research and compare local-first AI agent architectures")
        assertEquals(AgentTaskKind.RESEARCH, plan.taskKind)
        assertTrue(plan.assignedAgents.any { it.id == "athena" })
        assertTrue(plan.assignedAgents.any { it.id == "sentinel" })
        assertFalse(plan.approvalRequired)
        assertTrue(plan.steps.all { !it.requiresUserApproval })
    }

    @Test
    fun repositoryEngineeringPlanIncludesQualityAndSecurityReview() {
        val plan = JarvisAgentOrchestrator.createPlan("Fix repository Gradle build and add tests")
        assertEquals(AgentTaskKind.ENGINEERING, plan.taskKind)
        assertNotNull(plan.assignedAgents.find { it.id == "forge" })
        assertNotNull(plan.assignedAgents.find { it.id == "sentinel" })
        assertTrue(plan.steps.any { it.id == "verify" })
    }

    @Test
    fun externalAndDestructiveActionsAlwaysRequireApproval() {
        val publish = JarvisAgentOrchestrator.createPlan("Publish this app release")
        assertTrue(publish.approvalRequired)
        assertNotNull(publish.approvalReason)

        val erase = JarvisAgentOrchestrator.createPlan("Delete all stored project data")
        assertEquals(AgentTaskKind.DESTRUCTIVE_ACTION, erase.taskKind)
        assertTrue(erase.approvalRequired)
        assertTrue(erase.steps.first { it.id == "execute" }.requiresUserApproval)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTaskIsRejected() {
        JarvisAgentOrchestrator.createPlan("  ")
    }
}
