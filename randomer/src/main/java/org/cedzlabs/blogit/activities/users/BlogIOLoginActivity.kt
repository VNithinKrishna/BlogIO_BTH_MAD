package org.cedzlabs.blogit.activities.users

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import org.cedzlabs.blogit.activities.feed.FeedActivity
import org.cedzlabs.blogit.main.MainApp
import org.cedzlabs.blogit.models.posts.PostsFirebaseStore
import org.cedzlabs.blogit.tools.EspressoIdlingResource


class BlogIOLoginActivity : AppCompatActivity(), AnkoLogger {

    // Get instance of the Firebase Auth
    var auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Get instance of the Google Sign In
    val RC_SIGN_IN = 1

    // Configure sign-in to request the user's ID, email address, and basic
    // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
    var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()


    // Get instance of the Firebase Database
    var fireStore: PostsFirebaseStore? = null

    lateinit var app: MainApp
    override fun onCreate(savedInstanceState: Bundle?) {
        app = application as MainApp

        if (app.posts is PostsFirebaseStore) {
            fireStore = app.posts as PostsFirebaseStore
        }

        super.onCreate(savedInstanceState)
        setContentView(org.cedzlabs.blogit.R.layout.activity_login)
        val blogioSharedPref = BlogIOSharedPreferences(this)

        // Setup the login button functionality.
        firebaseLoginButton.setOnClickListener {
            // Make sure the fields are not empty, if they are tell the user to fill them.
            if (enteredLoginEmail.text.toString().isNotEmpty() && enteredLoginPassword.text.toString().isNotEmpty()) {
                // Tell Espresso to wait for UI testing as we do not know how long the Firebase login will take.
                EspressoIdlingResource.increment()
                // Show the loading indicator.
                showProgress()
                // Use the details entered by the user to sign in to the firebase auth cloud service.
                auth.signInWithEmailAndPassword(enteredLoginEmail.text.toString(), enteredLoginPassword.text.toString())
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                if (fireStore != null) {
                                    // Sign in success, update UI with the signed-in user's information
                                    fireStore!!.fetchPosts {
                                        // If the user logs in, set their email in the blogioSharedPref for use later on.
                                        blogioSharedPref.setCurrentUserEmail(enteredLoginEmail.text.toString())
                                        startActivityForResult(intentFor<FeedActivity>().putExtra("typeOfSignIn", "firebase"), 0)
                                    }

                                }
                            } else {
                                // If sign in fails due to incorrect details, display a message to the user.
                                toast(org.cedzlabs.blogit.R.string.toast_InvalidCreds)
                            }
                            // If user isn't found display this message.
                            if (!task.isSuccessful) {
                                toast(org.cedzlabs.blogit.R.string.toast_AuthFail)
                            }
                            hideProgress()
                            // Tell Espresso to it cant continue now.
                            if (!EspressoIdlingResource.getIdlingResource().isIdleNow) {
                                EspressoIdlingResource.decrement()
                            }
                        }
                // Tell the user to enter their email if blank.
            } else if (enteredLoginEmail.text.toString().isEmpty()) {
                toast("Please Enter Your Email")
                // Tell the user to enter their password if blank.
            } else if (enteredLoginPassword.text.toString().isEmpty()) {
                toast("Please Enter Your Password")
            } else {
                toast(org.cedzlabs.blogit.R.string.hint_EnterAllFields)
            }
        }

        // Set the register button to navigate to the BlogIORegisterActivity.
        navToRegisterButton.setOnClickListener {
            startActivityForResult<BlogIORegisterActivity>(0)
        }

        // Build a GoogleSignInClient with the options specified by gso.
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        googleLoginButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    // Functions to show and hide loading animation.
    fun showProgress() {
        loadingLoginIndicator.visibility = View.VISIBLE
    }

    fun hideProgress() {
        loadingLoginIndicator.visibility = View.GONE
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        val blogioSharedPref = BlogIOSharedPreferences(this)
        try {
            val account = completedTask.getResult(ApiException::class.java)

            if (account != null) {
                val personName = account.displayName
                val personEmail = account.email

                if (personEmail != null && personName != null) {
                    blogioSharedPref.setCurrentUserEmail(personEmail)
                    blogioSharedPref.setCurrentUserName((personName.replace("\\s".toRegex(), "").toLowerCase()).capitalize())
                }

                if (fireStore != null) {
                    // Sign in success, update UI with the signed-in user's information
                    fireStore!!.fetchPosts {
                        startActivityForResult(intentFor<FeedActivity>().putExtra("typeOfSignIn", "Google"), 0)
                    }

                }
            }
        } catch (e: ApiException) {
            toast(org.cedzlabs.blogit.R.string.toast_GoogleAuthFail)
        }

    }

}