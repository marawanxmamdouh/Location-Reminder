package com.udacity.project4.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.data.FakeDataSource
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import com.udacity.project4.R
import kotlinx.coroutines.test.resumeDispatcher

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel

    private val remindersList = listOf(
        ReminderDTO(
            "Random title",
            "Random description",
            "Random location",
            10.44206,
            81.58948
        ),
        ReminderDTO(
            "Random title",
            "Random description",
            "Random location",
            44.78961,
            -138.08021
        ),
        ReminderDTO(
            "Random title",
            "Random description",
            "Random location",
            -9.49210,
            -124.72571
        )
    )

    private val reminder1 = remindersList[0]
    private val reminder2 = remindersList[1]
    private val reminder3 = remindersList[2]

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun getRemindersList() {
        val remindersList = mutableListOf(reminder1, reminder2, reminder3)
        fakeDataSource = FakeDataSource(remindersList)
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.remindersList.value?.size, `is`(3))
    }

    @Test
    fun check_loading() {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun returnError() {
        fakeDataSource = FakeDataSource(null)
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showSnackBar, `is`("No reminders found"))
    }

}