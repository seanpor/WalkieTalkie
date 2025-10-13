package com.limemarmalade.walkietalkie

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var application: Application
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        application = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { sharedPreferences.getString(any(), any()) } returns ""
        viewModel = MainViewModel(application, testCoroutineRule.testDispatcher)
    }

    @Test
    fun `test onConnect saves ip address`() {
        val ip = "192.168.1.1"
        viewModel.onConnect(ip)
        verify(exactly = 1) { editor.putString("last_ip", ip) }
    }

    @Test
    fun `test onModeSelected with HOST`() = testCoroutineRule.testScope.runTest {
        val intentSlot = slot<Intent>()
        every { application.startService(capture(intentSlot)) } returns null

        viewModel.onModeSelected(Mode.HOST)

        assertEquals(Screen.Host, viewModel.screen.value)
        assertEquals("HOST", intentSlot.captured.action)
    }

    @Test
    fun `test onModeSelected with CLIENT`() {
        viewModel.onModeSelected(Mode.CLIENT)

        assertEquals(Screen.Client, viewModel.screen.value)
    }

    @Test
    fun `test updateClientCount`() {
        val count = 5
        viewModel.updateClientCount(count)
        assertEquals(count, viewModel.clientCount.value)
    }

    @Test
    fun `test onCleared`() {
        val intentSlot = slot<Intent>()
        every { application.stopService(capture(intentSlot)) } returns true

        viewModel.onCleared()

        verify { application.stopService(any()) }
    }
}