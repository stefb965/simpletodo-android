package com.simperium.simpletodo;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This model extends BucketObject to support syncing its properties with Simperium (done, title and
 * order)
 */

public class Todo extends BucketObject {

    private static final String DONE_PROPERTY = "done";
    private static final String TITLE_PROPERTY = "title";
    private static final String ORDER_PROPERTY = "order";

    private static final int DONE = 1;
    private static final int NOT_DONE = 0;

    private Todo(String key, JSONObject properties) {
        super(key, properties);
    }

    // Configure the Simperium Schema for this object
    public static class Schema extends BucketSchema<Todo> {

        public static final String BUCKET_NAME = "todo";

        public Schema() {
            // Indexing will improve retrieving and querying objects in Simperium
            // autoIndex indexes all top level properties: done, title and order
            autoIndex();
            setDefault(DONE_PROPERTY, NOT_DONE);
        }

        // Required, and should always match the bucket name set in Simperium
        public String getRemoteName() {
            return BUCKET_NAME;
        }

        // Build a new To-do from a JSONObject
        @Override
        public Todo build(String key, JSONObject properties) {
            return new Todo(key, properties);
        }

        // Updates a To-do with the JSONObject
        @Override
        public void update(Todo todo, JSONObject properties) {
            todo.updateProperties(properties);
            android.util.Log.d("Simpletodo", "Updated properties: " + todo);
        }
    }

    // Return a count of completed To-dos
    public static int countCompleted(Bucket<Todo> bucket) {
        return bucket.query().where(DONE_PROPERTY, Query.ComparisonType.EQUAL_TO, DONE).count();
    }

    // Returns a Simperium ObjectCursor for all existing To-dos
    public static Query<Todo> queryAll(Bucket<Todo> bucket) {
        Query<Todo> query = bucket.query();
        query.order(ORDER_PROPERTY);
        return query;
    }

    // Delete all completed To-dos from the bucket
    public static void deleteCompleted(final Bucket<Todo> bucket) {
        bucket.executeAsync(new Runnable() {
            @Override
            public void run() {
                Query<Todo> query = bucket.query();
                Bucket.ObjectCursor<Todo> cursor = query.where(DONE_PROPERTY, Query.ComparisonType.EQUAL_TO, DONE).execute();
                while (cursor.moveToNext()) {
                    Todo todo = cursor.getObject();
                    todo.delete();
                }
                cursor.close();
            }
        });
    }

    // Update the properties for the passed JSONObject
    private void updateProperties(JSONObject properties) {
        this.setProperties(properties);
    }

    // Toggle the completed state for a To-do
    // save() will save the object and sync the changes with Simperium
    public void toggleDone() {
        setProperty(DONE_PROPERTY, isDone() ? NOT_DONE : DONE);
        save();
    }

    // Get the To-do title
    public String getTitle() {
        return getProperties().optString(TITLE_PROPERTY, "");
    }

    // Set the To-do title
    public void setTitle(String title) {
        setProperty(TITLE_PROPERTY, title);
    }

    // Set the order property of this To-do
    public void setOrder(int order) {
        setProperty(ORDER_PROPERTY, order);
    }

    // Not usually needed to override, but used for logging in this sample app
    @Override
    public void save() {
        android.util.Log.d("Simpletodo", getProperties().toString());
        super.save();
    }

    // Checks if a To-Do is marked done
    // A bit of extra work is done to check if the value was set from the iOS Simpletodo app
    public boolean isDone() {
        try {
            // is done property an int of 1?
            return getProperties().getInt(DONE_PROPERTY) == DONE;
        } catch (JSONException e) {
            // done wasn't an int
            try {
                // is done boolean true?
                return getProperties().getBoolean(DONE_PROPERTY);
            } catch (JSONException e1) {
                // this was unexpected, it should have been an int or a boolean but was neither, we'll just return not done in this case
                return false;
            }
        }
    }

    // Used for debug logging
    public String toString() {
        return "Todo " + getSimperiumKey() + ": " + getTitle() + " [" + (isDone() ? "✓" : " " ) + "]";
    }
}