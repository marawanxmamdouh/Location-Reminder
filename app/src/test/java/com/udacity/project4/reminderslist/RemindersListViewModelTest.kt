package com.udacity.project4.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.data.FakeDataSource
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

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

    // Delete all reminders after each test
    @After
    fun clearDataSource() = runTest {
        fakeDataSource.deleteAllReminders()
    }

    @Test
    fun invalidateShowNoData_showNoData_isTrue() = runTest {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        //GIVEN - Empty DB
        fakeDataSource.deleteAllReminders()

        //WHEN - Try to load Reminders
        reminderListViewModel.loadReminders()

        //THEN - We expect that our reminder list Live data size is 0 and show no data is true
        assertThat(reminderListViewModel.remindersList.getOrAwaitValue().size, `is`(0))
        assertThat(reminderListViewModel.showNoData.getOrAwaitValue(), `is`(true))

    }

    /**In this function we test to retrieve the 3 reminders we're inserting**/
    @Test
    fun loadReminders_loadsThreeReminders() = mainCoroutineRule.runBlockingTest {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        //GIVEN - Only 3 Reminders in the DB
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder1)
        fakeDataSource.saveReminder(reminder2)
        fakeDataSource.saveReminder(reminder3)

        //WHEN - We try to load Reminders
        reminderListViewModel.loadReminders()

        //THEN - We expect to have only 3 reminders in remindersList and showNoData is false cause we have data
        assertThat(reminderListViewModel.remindersList.getOrAwaitValue().size, `is`(3))
        assertThat(reminderListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    /**Here in this test we testing checkLoading*/
    @Test
    fun loadReminders_checkLoading() = mainCoroutineRule.runTest {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        //GIVEN - Only 1 Reminder
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder1)

        //WHEN - We load Reminders
        reminderListViewModel.loadReminders()

        //THEN - loading indicator is shown and after we finishes we get the loading indicator hidden again.
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then loading indicator is hidden
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }


    @Test
    fun loadReminders_shouldReturnError() = mainCoroutineRule.runTest {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        fakeDataSource.shouldReturnError = true

        //WHEN - We load Reminders
        reminderListViewModel.loadReminders()

        //THEN - We get showSnackBar in the view model giving us "Reminders not found"
        assertThat(
            reminderListViewModel.showSnackBar.getOrAwaitValue(),
            `is`("Reminders not found")
        )
    }

    @Test
    fun getRemindersList() {
        fakeDataSource = FakeDataSource(remindersList.toMutableList())
        reminderListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        reminderListViewModel.loadReminders()
        // check list is right size
        assertThat(reminderListViewModel.remindersList.getOrAwaitValue().size, `is`(3))
    }

    @Test
    fun check_loading() {
        fakeDataSource = FakeDataSource(remindersList.toMutableList())
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showLoading.value, `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(reminderListViewModel.showLoading.value, `is`(false))
    }

    @Test
    fun returnError() {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        fakeDataSource.shouldReturnError = true
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showSnackBar.value, `is`("Reminders not found"))
    }
}