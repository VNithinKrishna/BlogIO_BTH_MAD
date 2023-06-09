package org.cedzlabs.blogit.activities.feed

import android.content.Intent
import android.os.Bundle

import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.card.*
import org.jetbrains.anko.*
import org.cedzlabs.blogit.activities.navbar.CustomExpandableListAdapter
import org.cedzlabs.blogit.activities.posts.ImagePostActivity
import org.cedzlabs.blogit.activities.posts.LinkPostActivity
import org.cedzlabs.blogit.activities.posts.TextPostActivity
import org.cedzlabs.blogit.activities.users.BlogIOSharedPreferences
import org.cedzlabs.blogit.main.MainApp
import org.cedzlabs.blogit.models.posts.PostModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.set



class FeedActivity : AppCompatActivity(), RedukeListener, AnkoLogger {


    // Create an instance of the FirebaseAuth.
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Create an instance of the app.
    lateinit var app: MainApp

    // Set the default feed sorting setting.
    var sortSetting = "Top"

    // Navbar expandable menu resources
    internal var expandableListView: ExpandableListView? = null
    internal var adapter: ExpandableListAdapter? = null
    internal var titleList: List<String>? = null

    var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()


    // When the activity is first created the following is run.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the activity to the feed layout.
        setContentView(org.cedzlabs.blogit.R.layout.activity_feed)
        app = application as MainApp
        // Set the title of the app bar.
        toolbarMain.title = title
        setSupportActionBar(toolbarMain)
        val actionbar: ActionBar? = supportActionBar
        // Show the drawer button in the action bar.
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(org.cedzlabs.blogit.R.drawable.ic_menu)
        }
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Create a list of posts and fill it with all current posts.
        var posts = app.posts.findAll()

        // Assign the sorting menu items their settings.
        when (sortSetting) {
            "Top" -> {
                // Sorts by votes.
                recyclerView.adapter = RedukeAdapter(sortByVotes(posts), this)
            }

            "Newest" -> {
                // Sorts by the newest.
                recyclerView.adapter = RedukeAdapter(sortByNewest(posts), this)
            }
            "Oldest" -> {
                // Sorts by oldest.
                recyclerView.adapter = RedukeAdapter(sortByOldest(posts), this)
            }
            "AlphabeticalAsc" -> {
                // Sorts alphabetically (ascending)
                recyclerView.adapter = RedukeAdapter(sortByAlphabeticalAsc(posts), this)
            }
            "AlphabeticalDec" -> {
                // Sorts alphabetically (descending)
                recyclerView.adapter = RedukeAdapter(sortByAlphabeticalDec(posts), this)
            }
        }

        // Load the posts into the recycler view
        loadPosts()

        // Set the FAB buttons action to allow the user to add a post.
        addPostFabButton.setOnClickListener {

            val postTypes = listOf("Text Post", "Image Post", "Link Post")
            selector(
                "What Type Of Post Do You Want To Create?",
                postTypes
            ) { _, i ->

                val sel = postTypes[i]

                when (sel) {
                    "Text Post" -> {
                        startActivityForResult<TextPostActivity>(0)
                    }
                    "Image Post" -> {
                        startActivityForResult<ImagePostActivity>(0)
                    }
                    "Link Post" -> {
                        startActivityForResult<LinkPostActivity>(0)
                    }
                    else -> error { "Invalid Post Type!" }
                }

            }


        }

        // Set the view as a drawer layout
        var mDrawerLayout: DrawerLayout = findViewById(org.cedzlabs.blogit.R.id.drawer_layout)

        // Find the nav view to set the drawer menu items
        val navigationView: NavigationView = findViewById(org.cedzlabs.blogit.R.id.nav_view)

        // Set the actions when the drawer items are pressed.
        navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            val postTypes = listOf("Text Post", "Image Post", "Link Post")
            when (menuItem.itemId) {
                org.cedzlabs.blogit.R.id.nav_Logout ->
                    alert(org.cedzlabs.blogit.R.string.logoutPrompt) {
                        yesButton {
                            app.posts.clear()
                            auth.signOut()
                            signOutGoogle()
                            finish()
                        }
                        noButton {}
                    }.show()
            }
            when (menuItem.itemId) {
                org.cedzlabs.blogit.R.id.nav_YourPosts -> recyclerView.adapter = RedukeAdapter(filterByPostCreator(posts), this)

            }
            // Close the drawer.
            mDrawerLayout.closeDrawers()
            true
        }

        val listData = HashMap<String, List<String>>()



        val mypreference = BlogIOSharedPreferences(this)

        val userEmail = mypreference.getCurrentUserEmail()

        val userName = mypreference.getCurrentUserName()

        val parentView = nav_view.getHeaderView(0)
        val navHeaderUser = parentView.findViewById(org.cedzlabs.blogit.R.id.current_user_nav_header) as TextView
        val navHeaderEmail = parentView.findViewById(org.cedzlabs.blogit.R.id.current_email_nav_header) as TextView
        val navHeaderPic = parentView.findViewById(org.cedzlabs.blogit.R.id.current_profile_picture_nav_header) as ImageView

        navHeaderEmail.text = userEmail

        val extras = intent.extras

        if (extras != null) {
            val loginType = extras.getString("typeOfSignIn")

            if (loginType == "firebase") {
                val firebaseUsername = userEmail.substringBefore("@")
                mypreference.setCurrentUserName(firebaseUsername)
                navHeaderUser.text = firebaseUsername
            } else {
                navHeaderUser.text = userName
            }

            if (loginType == "google") {
                val acct = GoogleSignIn.getLastSignedInAccount(this)
                Glide.with(this).load(acct!!.photoUrl).into(navHeaderPic)
            }
        } else {
            navHeaderUser.text = mypreference.getCurrentUserName()
        }


    }


    // Inflate the options menu.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.cedzlabs.blogit.R.menu.menu_feed, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Create a list of the sort options.
        val sortOptions = listOf("Top", "Newest", "Oldest", "Alphabetical (Ascending)", "Alphabetical (Descending)")

        // Set the actions of the action bar menu items.
        var mDrawerLayout: DrawerLayout = findViewById(org.cedzlabs.blogit.R.id.drawer_layout)
        when (item?.itemId) {
            android.R.id.home -> mDrawerLayout.openDrawer(GravityCompat.START)
        }
        when (item?.itemId) {
            // Create a popup where the user can select how they want their feed items to be sorted.
            org.cedzlabs.blogit.R.id.item_sort_feed ->
                selector(
                    "Sort Feed By",
                    sortOptions
                ) { _, i ->
                    when {
                        // Call the relevant functions when the relevant item is pressed.
                        sortOptions[i] == "Top" -> sortData("Top")
                        sortOptions[i] == "Newest" -> sortData("Newest")
                        sortOptions[i] == "Oldest" -> sortData("Oldest")
                        sortOptions[i] == "Alphabetical (Ascending)" -> sortData("AlphabeticalAsc")
                        sortOptions[i] == "Alphabetical (Descending)" -> sortData("AlphabeticalDec")
                    }
                }
        }
        val postTypes = listOf("Text Post", "Image Post", "Link Post")
        // Add a post
        when (item?.itemId) {
            org.cedzlabs.blogit.R.id.item_add_post ->
                selector(
                    "What Type Of Post Do You Want To Create?",
                    postTypes
                ) { _, i ->

                    val sel = postTypes[i]

                    when (sel) {
                        "Text Post" -> {
                            startActivityForResult<TextPostActivity>(0)
                        }
                        "Image Post" -> {
                            startActivityForResult<ImagePostActivity>(0)
                        }
                        "Link Post" -> {
                            startActivityForResult<LinkPostActivity>(0)
                        }
                        else -> error { "Invalid Post Type!" }
                    }
                }
        }

        // Log the user but create a pop to confirm they really want to log out.
        when (item?.itemId) {
            org.cedzlabs.blogit.R.id.item_logout ->
                alert(org.cedzlabs.blogit.R.string.logoutPrompt) {
                    yesButton {
                        app.posts.clear()
                        auth.signOut()
                        signOutGoogle()
                        finish()
                    }
                    noButton {}
                }.show()
        }
        return super.onOptionsItemSelected(item)
    }

    // Function which handles all the sorting.
    private fun sortData(sortOption: String) {
        var posts = app.posts.findAll()
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        when (sortOption) {
            "Top" -> {
                toast("Sorting By Upvotes")
                sortSetting = "Top"
                recyclerView.adapter = RedukeAdapter(sortByVotes(posts), this)
            }

            "Newest" -> {
                toast("Sorting By Newest")
                sortSetting = "Newest"
                recyclerView.adapter = RedukeAdapter(sortByNewest(posts), this)
            }
            "Oldest" -> {
                toast("Sorting By Oldest")
                sortSetting = "Oldest"
                recyclerView.adapter = RedukeAdapter(sortByOldest(posts), this)
            }
            "AlphabeticalAsc" -> {
                toast("Sorting By Alphabetical (Ascending)")
                sortSetting = "AlphabeticalAsc"
                recyclerView.adapter = RedukeAdapter(sortByAlphabeticalAsc(posts), this)
            }
            "AlphabeticalDec" -> {
                toast("Sorting By Alphabetical (Descending)")
                sortSetting = "AlphabeticalDec"
                recyclerView.adapter = RedukeAdapter(sortByAlphabeticalDec(posts), this)

            }
        }
        // Run the feed animation after sort.
        runAnimation(recyclerView)
    }

    // When a card is clicked bring the user to the edit activity of that post they clicked.
    override fun onTextPostCardClick(post: PostModel) {
        toast("onTextPostCardClick")
        startActivityForResult(intentFor<TextPostActivity>().putExtra("post_edit", post), 0)
    }

    // When a card is clicked bring the user to the edit activity of that post they clicked.
    override fun onImagePostCardClick(post: PostModel) {
        toast("onImagePostCardClick")
        startActivityForResult(intentFor<ImagePostActivity>().putExtra("post_edit", post), 0)
    }

    // When a card is clicked bring the user to the edit activity of that post they clicked.
    override fun onLinkPostCardClick(post: PostModel) {
        toast("onLinkPostCardClick")
        startActivityForResult(intentFor<LinkPostActivity>().putExtra("post_edit", post), 0)
    }

    // Handle the upvote button being pressed by a user.
    override fun onPostUpvote(post: PostModel) {
        // Get the current user email from BlogIOSharedPreferences
        val mypreference = BlogIOSharedPreferences(this)
        val userEmail = mypreference.getCurrentUserEmail()

        // Check if the user who just pressed the upvote button has pressed the downvote
        // button for this post. If they have remove their name from the downvotedBy field
        // as a user is only allowed to either upvote or downvote a post.
        if (userEmail in post.downvotedBy) {
            post.downvotedBy.remove(userEmail)
            recyclerView.adapter?.notifyDataSetChanged()
        }

        // If the user hasnt already upvoted the post add them to the upvotedBy field and
        // add one onto the posts votes.
        if (userEmail !in post.upvotedBy) {
            post.upvotedBy.add(userEmail)
            post.votes = post.votes + 1
            cardUpvotePost.isEnabled = false
            cardDownvotePost.isEnabled = true
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    override fun onPostDownvote(post: PostModel) {

        val userEmail = BlogIOSharedPreferences(this).getCurrentUserEmail()

        // Check if the user who just pressed the downvote button has pressed the upvote
        // button for this post. If they have remove their name from the upvotedBy field
        // as a user is only allowed to either upvote or downvote a post.
        if (userEmail in post.upvotedBy) {
            post.upvotedBy.remove(userEmail)
            recyclerView.adapter?.notifyDataSetChanged()
        }

        // If the user hasnt already downvoted the post add them to the downvotedBy field and
        // subtract one onto the posts votes.
        if (userEmail !in post.downvotedBy) {
            post.downvotedBy.add(userEmail)
            post.votes = post.votes - 1
            cardDownvotePost.isEnabled = false
            cardUpvotePost.isEnabled = true
            recyclerView.adapter?.notifyDataSetChanged()
        }

    }

    // When the add/edit activity finishes load the posts and execute the feed animation.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        loadPosts()
        runAnimation(recyclerView)
    }

    // Load the posts from JSON.
    private fun loadPosts() {
        initFeed(app.posts.findAll())
    }

    // Sorting functions

    // Sorts by votes.
    fun allPosts(list: List<PostModel>): List<PostModel> {
        toolbarMain.title = "Feed"
        return list
    }

    // Sorts by votes.
    fun sortByVotes(list: List<PostModel>): List<PostModel> {
        return list.sortedByDescending { post -> post.votes }
    }

    // Sorts by the newest.
    fun sortByNewest(list: List<PostModel>): List<PostModel> {
        return list.sortedWith(compareByDescending { LocalDateTime.parse(it.timestamp, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSSSSS")) })
    }

    // Sorts by oldest.
    fun sortByOldest(list: List<PostModel>): List<PostModel> {
        return list.sortedWith(compareBy { LocalDateTime.parse(it.timestamp, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSSSSS")) })
    }

    // Sorts alphabetically (ascending)
    fun sortByAlphabeticalAsc(list: List<PostModel>): List<PostModel> {
        return list.sortedBy { post -> post.title }
    }

    // Sorts alphabetically (descending)
    fun sortByAlphabeticalDec(list: List<PostModel>): List<PostModel> {
        return list.sortedByDescending { post -> post.title }
    }

    // Sorts alphabetically (descending)
    fun filterBySubreddit(list: List<PostModel>, subreddit: String): List<PostModel> {
        toolbarMain.title = subreddit
        return list.filter { post -> post.subreddit == subreddit }
    }

    // Sorts alphabetically (descending)
    fun filterByPostCreator(list: List<PostModel>): List<PostModel> {
        toolbarMain.title = "Your Posts"
        val mypreference = BlogIOSharedPreferences(this)
        return list.filter { post -> post.owner == mypreference.getCurrentUserName() }
    }

    // Initialize the feed by setting the users email and username in the BlogIOSharedPreferences
    // and pass the RedukeAdapter to the recyclerView's adapter.
    fun initFeed(imagePosts: List<PostModel>) {
        recyclerView.adapter = RedukeAdapter(imagePosts, this)
        recyclerView.adapter?.notifyDataSetChanged()
    }

    // When the user presses the back button log them out but display a pop up to
    // make sure they want to do this.
    override fun onBackPressed() {
        alert(org.cedzlabs.blogit.R.string.logoutPrompt) {
            yesButton {
                app.posts.clear()
                auth.signOut()
                signOutGoogle()
                finish()
                super.onBackPressed()
            }
            noButton {}
        }.show()
    }

    private fun runAnimation(recyclerView: RecyclerView) {
        // Get the context.
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, org.cedzlabs.blogit.R.anim.feedslideanimation)
        recyclerView.layoutAnimation = controller
        // Run the animation.
        recyclerView.scheduleLayoutAnimation()
    }

    private fun signOutGoogle() {
        // Build a GoogleSignInClient with the options specified by gso.
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        mGoogleSignInClient.signOut()
    }
}