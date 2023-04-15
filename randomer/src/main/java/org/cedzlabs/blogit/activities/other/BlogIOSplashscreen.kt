package org.cedzlabs.blogit.activities.other

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.startActivityForResult
import org.cedzlabs.blogit.R
import org.cedzlabs.blogit.activities.users.BlogIOLoginActivity

class BlogIOSplashscreen : AppCompatActivity() {
    // Create a mDelayHandler for the delay.
    private var mDelayHandler: Handler? = null

    // Create the delay value
    private val splashDelay: Long = 1000

    // Finish the splashscreen after delay is done.
    internal val mRunnable: Runnable = Runnable {
        if (!isFinishing) {
            startActivityForResult<BlogIOLoginActivity>(0)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        //Initialize the Handler
        mDelayHandler = Handler()

        //Navigate with delay
        mDelayHandler!!.postDelayed(mRunnable, splashDelay)

    }

    // Nullify the mDelayHandler for future launches.
    public override fun onDestroy() {
        if (mDelayHandler != null) {
            mDelayHandler!!.removeCallbacks(mRunnable)
        }
        super.onDestroy()
    }

}