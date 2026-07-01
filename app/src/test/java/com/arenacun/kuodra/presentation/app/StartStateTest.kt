package com.arenacun.kuodra.presentation.app

import com.arenacun.kuodra.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Test

class StartStateTest {

    @Test
    fun `null session is LoggedOut`() {
        assertEquals(StartState.LoggedOut, resolveStartState(session = null, spaceConfigured = true))
    }

    @Test
    fun `blank name is NeedsName`() {
        val session = Session("u1", "a@b.com", name = "")
        assertEquals(StartState.NeedsName, resolveStartState(session, spaceConfigured = true))
    }

    @Test
    fun `name present but unconfigured space is Onboarding`() {
        val session = Session("u1", "a@b.com", name = "Alex")
        assertEquals(StartState.Onboarding, resolveStartState(session, spaceConfigured = false))
    }

    @Test
    fun `name present and configured space is Ready`() {
        val session = Session("u1", "a@b.com", name = "Alex")
        assertEquals(StartState.Ready, resolveStartState(session, spaceConfigured = true))
    }
}
