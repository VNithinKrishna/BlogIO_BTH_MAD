package org.cedzlabs.blogit.activities.posts

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.widget.Spinner
import kotlinx.android.synthetic.main.activity_image_post.*
import org.jetbrains.anko.*
import org.cedzlabs.blogit.activities.users.BlogIOSharedPreferences
import org.cedzlabs.blogit.helpers.readImage
import org.cedzlabs.blogit.helpers.readImageFromPath
import org.cedzlabs.blogit.helpers.showImagePicker
import org.cedzlabs.blogit.main.MainApp
import org.cedzlabs.blogit.models.posts.PostModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class ImagePostActivity : AppCompatActivity(), AnkoLogger {

    val subreddits = listOf("Android", "Education", "Fashion", "Funny", "Ireland", "Music", "News", "Photography", "Technology", "Video")

    val GALLERY_IMAGE_REQUEST = 1
    val CAMERA_IMAGE_REQUEST = 2
    var mCurrentPhotoPath: String = ""

    var post = PostModel()
    lateinit var app: MainApp
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(org.cedzlabs.blogit.R.layout.activity_image_post)
        toolbarImage.title = "Add Image Post"
        setSupportActionBar(toolbarImage)

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
            toolbarImage.title = "Edit Post"
            setSupportActionBar(toolbarImage)
            post = intent.extras.getParcelable<PostModel>("post_edit")

            // Fill the fields with existing data.
            imagePostTitleField.setText(post.title)

            imagePostSubredditSpinner.setSelection(subreddits.indexOf(post.subreddit))
            imagePostSubredditSpinner.isEnabled = false

            deleteImagePostBtn.visibility = VISIBLE

            // Set the button to display save instead of add.
            addImagePostBtn.setText(org.cedzlabs.blogit.R.string.button_savePost)

            if (post.image.isNotEmpty()) {
                postImage.setImageBitmap(readImageFromPath(this, post.image))
                postImage.visibility = View.VISIBLE
                chooseImageFromGallery.visibility = View.VISIBLE
                chooseImageFromCamera.visibility = View.VISIBLE
            }
        }

        // Gallery
        chooseImageFromGallery.setOnClickListener {
            showImagePicker(this, GALLERY_IMAGE_REQUEST)
        }

        // Camera
        chooseImageFromCamera.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                                this,
                                "org.cedzlabs.blogit.fileprovider", it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, CAMERA_IMAGE_REQUEST)
                    }
                }
            }
        }

        // If the user is adding a post.
        addImagePostBtn.setOnClickListener {
            // Get the data from all the post fields.
            post.title = imagePostTitleField.text.toString()
            post.type = "Image"
            // Create a timestamp for when the post has been created.
            post.timestamp = DateTimeFormatter
                    .ofPattern("dd-MM-yyyy HH:mm:ss.SSSSSS")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())
            // Get the value from the subreddit spinner.
            val subredditSpinner = findViewById<Spinner>(org.cedzlabs.blogit.R.id.imagePostSubredditSpinner)
            post.subreddit = subredditSpinner.selectedItem.toString()

            // Get an instance of the BlogIOSharedPreferences.
            val redukeSharedPref = BlogIOSharedPreferences(this)
            post.owner = redukeSharedPref.getCurrentUserName()

            // If the fields are empty tell the user they need to fill them.
            if (post.title.isEmpty() or post.image.isEmpty()) {
                toast(org.cedzlabs.blogit.R.string.hint_EnterPostTitle)
            } else {
                if (edit) {
                    // If the user has edited a post update it.
                    app.posts.update(post.copy())
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
        deleteImagePostBtn.setOnClickListener {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // GALLERY
            GALLERY_IMAGE_REQUEST -> {
                if (data != null) {
                    post.image = data.getData().toString()
                    postImage.setImageBitmap(readImage(this, resultCode, data))
                    postImage.visibility = View.VISIBLE
                    chooseImageFromCamera.isClickable = false
                    chooseImageFromCamera.setBackgroundColor(Color.parseColor("#FF9E9E9E"))
                }
            }

            // CAMERA
            CAMERA_IMAGE_REQUEST -> {
                if (data != null) {
                    if (resultCode == Activity.RESULT_OK) {
                        postImage.setImageBitmap(decodeBitmap())
                        post.image = cameraPicSaveAndGet()
                    }
                    chooseImageFromGallery.setBackgroundColor(Color.parseColor("#FF9E9E9E"))
                    chooseImageFromGallery.isClickable = false
                    postImage.visibility = View.VISIBLE
                }
            }
        }
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    fun decodeBitmap(): Bitmap {
        return BitmapFactory.decodeFile(mCurrentPhotoPath)
    }

    fun cameraPicSaveAndGet(): String {
        val f = File(mCurrentPhotoPath)
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
        return Uri.fromFile(f).toString()
    }


}

