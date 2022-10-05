package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthenticationViewModel>()

    companion object {
        const val TAG = "AuthenticationActivity"
    }

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeAuthenticationState()

        binding.loginBtn.setOnClickListener {
            launchSignInFlow()
        }
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email
        // If users choose to register with their email,
        // they will need to create a password as well
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent.
        // We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code
        startActivity(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )
    }

    private fun observeAuthenticationState() {
        viewModel.authenticationState.observe(this) {
            when (it) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.i(
                        TAG,
                        "observeAuthenticationState (line 88): Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                    )
                    // navigate to RemindersActivity
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                    Log.i(TAG, "observeAuthenticationState (line 89): not authenticated")
                }
            }
        }
    }
}
