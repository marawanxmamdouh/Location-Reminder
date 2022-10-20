package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

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
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun getReminders() = runBlockingTest {
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        val list = database.reminderDao().getReminders()

        assertThat(list.size, `is`(2))
        assertThat(list[0].title, `is`("Random title"))
        assertThat(list[1].title, `is`("Random title"))
        assertThat(list, hasItems(reminder1, reminder2))
    }

    @Test
    fun getReminders_IfEmptyList() = runBlockingTest {
        val remindersList = database.reminderDao().getReminders()

        assertThat(remindersList.size, `is`(0))
        assertThat(remindersList, `is`(emptyList()))
    }

    @Test
    fun getReminderById() = runBlockingTest {
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        val reminder = database.reminderDao().getReminderById(reminder1.id)

        assertThat(reminder?.id, `is`(reminder1.id))
        assertThat(reminder?.title, `is`("Random title"))
    }

    @Test
    fun getReminderById_IfEmptyList() = runBlockingTest {
        val reminder = database.reminderDao().getReminderById(reminder1.id)
        assertThat(reminder?.id, `is`(nullValue()))
    }

    @Test
    fun deleteAll() = runBlockingTest {
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        database.reminderDao().deleteAllReminders()
        val reminderList = database.reminderDao().getReminders()

        assertThat(reminderList, `is`(emptyList()))
    }

}