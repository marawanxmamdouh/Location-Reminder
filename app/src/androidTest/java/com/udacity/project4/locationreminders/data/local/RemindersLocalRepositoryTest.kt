package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: RemindersLocalRepository

    private lateinit var database: RemindersDatabase

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
        )
    )

    private val reminder1 = remindersList[0]
    private val reminder2 = remindersList[1]

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java).build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun getReminders() = runTest {
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        val result = (repository.getReminders() as Result.Success).data

        assertThat(result.size, `is` (2))
        assertThat(result.contains(reminder1), `is` (true))
        assertThat(result.contains(reminder2), `is` (true))
    }

    @Test
    fun getReminders_IfEmptyList() = runTest {
        val result = repository.getReminders()
        result as Result.Success // success because you're still returning a list, its just empty
        assertEquals(result.data.size, 0)
    }

    @Test
    fun getReminderById() = runTest {
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        val result = repository.getReminder(reminder1.id) as Result.Success

        assertEquals(result.data.id, reminder1.id)
    }

    @Test
    fun getReminderById_IfError() = runTest {
        repository.saveReminder(reminder2)
        val result = repository.getReminder(reminder1.id) as Result.Error

        assertEquals(result.message, "Reminder not found!")
    }

    @Test
    fun deleteAll() = runTest {
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        repository.deleteAllReminders()

        val result = (repository.getReminders() as Result.Success).data

        assertEquals(result.size, 0)
        assertThat(result.contains(reminder1), `is` (false))
        assertThat(result.contains(reminder2), `is` (false))
    }
}