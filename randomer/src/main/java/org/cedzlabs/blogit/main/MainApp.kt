package org.cedzlabs.blogit.main

import android.app.Application
import org.jetbrains.anko.AnkoLogger
import org.cedzlabs.blogit.models.posts.PostStore
import org.cedzlabs.blogit.models.posts.PostsFirebaseStore
import org.cedzlabs.blogit.models.users.UserStore

class MainApp : Application(), AnkoLogger {

    // Create the list variables for both the posts and users.
    lateinit var posts: PostStore
    lateinit var users: UserStore

    override fun onCreate() {
        // When the app is run.
        super.onCreate()

        // Create an instance of the JSON store for both posts and users.
        posts = PostsFirebaseStore(applicationContext)
//        users = UserJSONStore(applicationContext)
    }
}