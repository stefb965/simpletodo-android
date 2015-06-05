package com.simperium.simpletodo;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.simperium.android.LoginActivity;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

public class TodoListActivity extends AppCompatActivity
        implements Bucket.Listener<Todo>, OnItemClickListener, OnEditorActionListener,
        TrashIconProvider.OnClearCompletedListener, TodoEditorFragment.OnTodoEditorCompleteListener {

    public final int ADD_ACTION_ID = 100;

    private static final String EMPTY_STRING = "";
    private static final String EDITOR_FRAGMENT = "editor_dialog";

    protected TodoAdapter mAdapter;
    protected Bucket<Todo> mTodoBucket;
    protected EditText mEditText;
    protected TrashIconProvider mTrashIconProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.todo_list);

        mAdapter = new TodoAdapter();
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        mEditText = (EditText) findViewById(R.id.new_task_text);
        mEditText.setOnEditorActionListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        TodoApplication app = (TodoApplication) getApplication();

        // Prompt for login if we don't have an authorized user
        if (app.getSimperium().needsAuthorization()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        mTodoBucket = app.getTodoBucket();

        if (mTodoBucket != null) {
            mTodoBucket.addListener(this);
            mTodoBucket.start();
            refreshTodos(mTodoBucket);
        }
    }

    @Override
    protected void onPause() {
        if (mTodoBucket != null) {
            mTodoBucket.removeListener(this);
            mTodoBucket.stop();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.todo_options, menu);

        MenuItem item = menu.findItem(R.id.action_clear_done);
        mTrashIconProvider = (TrashIconProvider) MenuItemCompat.getActionProvider(item);

        if (mTrashIconProvider != null) {
            mTrashIconProvider.setOnClearCompletedListener(this);
            if (mTodoBucket != null) {
                mTrashIconProvider.setBadgeCount(Todo.countCompleted(mTodoBucket));
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
        if (actionId != ADD_ACTION_ID || mTodoBucket == null)
            return false;

        // clear the text view
        String label = tv.getText().toString();

        // we don't create blank tasks, but leave the keyboard
        if (label.equals(EMPTY_STRING)) {
            return true;
        }

        tv.getEditableText().clear();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tv.getWindowToken(), 0x0);

        // Create a new Todo object, and save to Simperium
        Todo todo = mTodoBucket.newObject();
        todo.setTitle(label);
        todo.setOrder(mAdapter.getCount());
        todo.save();

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
        Todo todo = mAdapter.getItem(position);
        todo.toggleDone();
        CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
        checkbox.setChecked(todo.isDone());
    }

    public void refreshTodos(final Bucket<Todo> todos) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.requeryBucket(todos);
                if (mTrashIconProvider != null) {
                    boolean changed = mTrashIconProvider.updateBadgeCount(Todo.countCompleted(todos));
                    if (changed) {
                        supportInvalidateOptionsMenu();
                    }
                }

            }
        });
    }

    @Override
    public void onClearCompleted() {
        if (mTodoBucket == null) return;

        Todo.deleteCompleted(mTodoBucket);
    }

    public void onEditTodo(Todo todo) {
        TodoEditorFragment fragment = TodoEditorFragment.newInstance(todo);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(fragment, EDITOR_FRAGMENT);
        transaction.commit();

    }

    @Override
    public void onTodoEdited(String key, String label) {
        try {
            Todo todo = mTodoBucket.get(key);
            todo.setTitle(label);
            todo.save();
        } catch (BucketObjectMissingException e) {
            e.printStackTrace();
        }

        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0x0);
        }
    }

    // List adapter
    class TodoAdapter extends CursorAdapter {

        TodoAdapter() {
            super(TodoListActivity.this, null, false);
        }

        public void requeryBucket(Bucket<Todo> todos) {
            swapCursor(Todo.queryAll(todos).execute());
        }

        public Todo getItem(int position) {
            Bucket.ObjectCursor<Todo> cursor = (Bucket.ObjectCursor<Todo>) super.getItem(position);
            return cursor.getObject();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Bucket.ObjectCursor<Todo> bucketCursor = (Bucket.ObjectCursor<Todo>) cursor;
            final Todo todo = bucketCursor.getObject();

            TodoRowHolder viewHolder = (TodoRowHolder) view.getTag(R.id.todo_row_holder);

            Spannable title = new SpannableString(todo.getTitle());

            if (TextUtils.isEmpty(title)) {
                title = emptyTitle();
            }

            boolean done = todo.isDone();

            if (done)
                title.setSpan(new StrikethroughSpan(), 0, title.length(), 0x0);

            viewHolder.labelView.setText(title);
            viewHolder.checkBox.setChecked(done);
            viewHolder.button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onEditTodo(todo);
                }

            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.todo_row, parent, false);
            TextView textView = (TextView) view.findViewById(R.id.label);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
            ImageButton button = (ImageButton) view.findViewById(R.id.edit_button);
            TodoRowHolder holder = new TodoRowHolder(textView, checkBox, button);
            view.setTag(R.id.todo_row_holder, holder);
            return view;
        }

        private final class TodoRowHolder {

            public final TextView labelView;
            public final CheckBox checkBox;
            public final ImageButton button;

            public TodoRowHolder(TextView tv, CheckBox cb, ImageButton b) {
                labelView = tv;
                checkBox = cb;
                button = b;
            }
        }
    }

    protected SpannableString emptyTitle() {
        SpannableString title = new SpannableString(getString(R.string.empty_task_title));
        int length = title.length();
        title.setSpan(new StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.empty_task_text_color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return title;
    }

    // Simperium Bucket Listeners
    @Override
    public void onSaveObject(Bucket<Todo> todos, Todo todo) {
        refreshTodos(todos);
    }

    @Override
    public void onDeleteObject(Bucket<Todo> todos, Todo todo) {
        refreshTodos(todos);
    }


    @Override
    public void onBeforeUpdateObject(Bucket<Todo> bucket, Todo todo) {
        // noop
    }

    @Override
    public void onNetworkChange(Bucket<Todo> todos, Bucket.ChangeType changeType, String s) {
        refreshTodos(todos);
    }
}