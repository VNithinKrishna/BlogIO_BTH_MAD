package org.cedzlabs.blogit.activities.posts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.activity_text_post.*
import org.jetbrains.anko.*
import org.cedzlabs.blogit.activities.users.BlogIOSharedPreferences
import org.cedzlabs.blogit.main.MainApp
import org.cedzlabs.blogit.models.posts.PostModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class TextPostActivity : AppCompatActivity(), AnkoLogger {

    var post = PostModel()
    lateinit var app: MainApp
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(org.cedzlabs.blogit.R.layout.activity_text_post)
        toolbarText.title = "Add Text Post"
        setSupportActionBar(toolbarText)

        // Show the up the button to allow the user to navigate back to the feed.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        app = application as MainApp

        // Create a boolean to track if the user is editing or adding a post.
        var edit = false

        // If the intent contains post_edit the user is editing a post.
        if (intent.hasExtra("post_edit")) {
            // Set edit mode true
            edit = true

            // Set toolbar title to Edit Post.
            toolbarText.title = "Edit Post"
            setSupportActionBar(toolbarText)
            post = intent.extras.getParcelable<PostModel>("post_edit")

            // Fill the fields with existing data.
            textPostTitleField.setText(post.title)
            textPostDescriptionField.setText(post.text)

            deleteTextPostBtn.visibility = VISIBLE

            // Set the button to display save instead of add.
            addTextPostBtn.setText(org.cedzlabs.blogit.R.string.button_savePost)
        }

        // If the user is adding a post.
        addTextPostBtn.setOnClickListener {
            // Get the data from all the post fields.
            post.title = textPostTitleField.text.toString()

            post.type = "Text"

            post.text = textPostDescriptionField.text.toString()
            // Create a timestamp for when the post has been created.
            post.timestamp = DateTimeFormatter
                    .ofPattern("dd-MM-yyyy HH:mm:ss.SSSSSS")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())

            // Get an instance of the BlogIOSharedPreferences.
            val redukeSharedPref = BlogIOSharedPreferences(this)
            post.owner = redukeSharedPref.getCurrentUserName()

            // If the fields are empty tell the user they need to fill them.
            if (post.title.isEmpty() or post.text.isEmpty()) {
                toast(org.cedzlabs.blogit.R.string.hint_EnterPostTitle)
            } else {
                if (edit) {
                    // If the user has edited a post update it.
                    app.posts.update(post.copy())
                    recyclerView.adapter?.notifyDataSetChanged()
                } else {
                    // If the user has added a post add it to the posts.
                    app.posts.create(post.copy())
                }
                // Return a result of all ok and finish activity.
                setResult(AppCompatActivity.RESULT_OK)
                finish()
            }

        }

        // Set up the delete buttons functionality.
        deleteTextPostBtn.setOnClickListener {
            // Make sure the user actually wants to the delete the post with a yes or no popup.
            alert(org.cedzlabs.blogit.R.string.deletePrompt) {
                yesButton {
                    app.posts.delete(post)
                    finish()
                }
                noButton {}
            }.show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.cedzlabs.blogit.R.menu.menu_post_add_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Set up the cancel button functionality.
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            org.cedzlabs.blogit.R.id.item_cancel -> {
                // Make sure the user actually wants to discard post with a yes or no popup.
                alert(org.cedzlabs.blogit.R.string.unsavedPrompt) {
                    yesButton {
                        finish()
                    }
                    noButton {}
                }.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Make sure the user actually wants to discard post with a yes or no popup.
    override fun onBackPressed() {
        alert(org.cedzlabs.blogit.R.string.unsavedPrompt) {
            yesButton {
                finish()
                super.onBackPressed()
            }
            noButton {}
        }.show()
    }


}

