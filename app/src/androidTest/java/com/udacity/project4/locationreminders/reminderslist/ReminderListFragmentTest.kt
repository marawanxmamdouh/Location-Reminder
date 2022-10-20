package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.get
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
    //    : test the navigation of the fragments.
//    : test the displayed data on the UI.
//    : add testing for the error messages.
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private lateinit var repository: FakeDataSource
    private lateinit var applicationContext: Application

    @Before
    fun init() {
        stopKoin()
        applicationContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    applicationContext,
                    get() as FakeDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    applicationContext,
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
            single { LocalDB.createRemindersDao(applicationContext) }
        }

        startKoin {
            modules(listOf(myModule))
        }

        repository = get() as FakeDataSource

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun checkRemindersDisplayed() = runTest {
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
            .check(matches(hasDescendant(withText("Random title"))))
    }

    @Test
    fun checkNoReminders_DisplaysNoData() = runTest {
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