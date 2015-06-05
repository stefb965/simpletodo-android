package com.simperium.simpletodo;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;

public class TodoApplication extends Application {
    Simperium mSimperium;
    Bucket<Todo> mTodoBucket;

    @Override
    public void onCreate() {
        super.onCreate();

        mSimperium = Simperium.newClient(BuildConfig.SIMPERIUM_APP, BuildConfig.SIMPERIUM_KEY, this);

        try {
            mTodoBucket = mSimperium.bucket("todo", new Todo.Schema());
        } catch (BucketNameInvalid bucketNameInvalid) {
            Log.i("SimpleTodo", "Could not create bucket");
        }
    }

    public Simperium getSimperium() {
        return mSimperium;
    }

    public Bucket<Todo> getTodoBucket() {
        return mTodoBucket;
    }
}