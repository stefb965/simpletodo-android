package com.simperium.simpletodo;

import android.app.Application;
import android.util.Log;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;

/**
 * The main application class. It is recommended that you create your Simperium client here and keep
 * private instances of all buckets needed for your application with getters.
 */

public class TodoApplication extends Application {

    // Simperium and Bucket objects
    private Simperium mSimperium;
    private Bucket<Todo> mTodoBucket;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create Simperium client
        mSimperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP, BuildConfig.SIMPERIUM_KEY, this);

        try {
            mTodoBucket = mSimperium.bucket("todo", new Todo.Schema());
        } catch (BucketNameInvalid bucketNameInvalid) {
            Log.i("SimpleTodo", "Could not create bucket");
        }
    }

    // Getters
    public Simperium getSimperium() {
        return mSimperium;
    }

    public Bucket<Todo> getTodoBucket() {
        return mTodoBucket;
    }
}