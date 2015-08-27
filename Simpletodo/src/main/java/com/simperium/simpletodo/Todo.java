package com.simperium.simpletodo;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;

import org.json.JSONException;
import org.json.JSONObject;

public class Todo extends BucketObject {

    private static final String DONE_PROPERTY = "done";
    private static final String TITLE_PROPERTY = "title";
    private static final String ORDER_PROPERTY = "order";

    private static final int DONE = 1;
    private static final int NOT_DONE = 0;

    private Todo(String key, JSONObject properties) {
        super(key, properties);
    }

    // Configure Simperium Schema for this object
    public static class Schema extends BucketSchema<Todo> {

        public static final String BUCKET_NAME = "todo";

        public Schema() {
            // autoIndex indexes all top level properties: done, title and order
            autoIndex();
            setDefault(DONE_PROPERTY, NOT_DONE);
        }

        public String getRemoteName() {
            return BUCKET_NAME;
        }

        @Override
        public Todo build(String key, JSONObject properties) {
            return new Todo(key, properties);
        }

        @Override
        public void update(Todo todo, JSONObject properties) {
            todo.updateProperties(properties);
            android.util.Log.d("Simpletodo", "Updated properties: " + todo);
        }
    }

    public static int countCompleted(Bucket<Todo> bucket) {
        return bucket.query().where(DONE_PROPERTY, Query.ComparisonType.EQUAL_TO, DONE).count();
    }

    public static Query<Todo> queryAll(Bucket<Todo> bucket) {
        Query<Todo> query = bucket.query();
        query.order(ORDER_PROPERTY);
        return query;
    }

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

    public String toString() {
        return "Todo " + getSimperiumKey() + ": " + getTitle() + " [" + (isDone() ? "✓" : " " ) + "]";
    }

    private void updateProperties(JSONObject properties) {
        this.setProperties(properties);
    }

    public void toggleDone() {
        setProperty(DONE_PROPERTY, isDone() ? NOT_DONE : DONE);
        save();
    }

    public String getTitle() {
        return getProperties().optString(TITLE_PROPERTY, "");
    }

    public void setTitle(String title) {
        setProperty(TITLE_PROPERTY, title);
    }

    /**
     * Simpletodo on iOS uses boolean true/false JSON while the web app uses
     * an integer (1=true, 0=done) instead.
     * 
     * We use an int but we need to check for boolean as well.
     */
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

    public void setOrder(int order) {
        setProperty(ORDER_PROPERTY, order);
    }

    public void save() {
        android.util.Log.d("Simpletodo", getProperties().toString());
        super.save();
    }
}