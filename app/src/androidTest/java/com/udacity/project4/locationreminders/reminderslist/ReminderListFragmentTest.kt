package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var applicationContext: Application

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

    @Before
    fun init() {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get(),
                    get()
                )
            }
            single {
                FakeDataSource() as ReminderDataSource
            }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }
    }

    @Test
    fun checkRemindersDisplayed() = runTest {
        repository = FakeDataSource(mutableListOf())
        val reminder1 = ReminderDTO(
            "Random title",
            "Random description",
            "Random location",
            10.44206,
            81.58948
        )
        repository.saveReminder(reminder1)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun checkNoReminders_DisplaysNoData() = runTest {
        repository = FakeDataSource()
        repository.deleteAllReminders()

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView))
            .check(matches(withText("No Data")))
    }

    @Test
    fun addReminderFabClick_navigateToSaveReminder() {
        val navController = mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed())).perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}