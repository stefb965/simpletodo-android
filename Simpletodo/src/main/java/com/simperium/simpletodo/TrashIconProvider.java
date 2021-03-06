package com.simperium.simpletodo;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * A custom ActionProvider that shows the count of completed To-Dos in the Toolbar
 */
public class TrashIconProvider extends ActionProvider {

    public interface OnClearCompletedListener {
        void onClearCompleted();
    }

    private WeakReference<OnClearCompletedListener> mListener;

    private int mBadgeCount = 0;

    public TrashIconProvider(Context context) {
        super(context);
    }

    public void setBadgeCount(int count) {
        mBadgeCount = count;
    }

    public boolean updateBadgeCount(int count) {
        return count != mBadgeCount;
    }

    @Override
    public View onCreateActionView() {
        throw new RuntimeException("Nope");
    }

    @Override
    public View onCreateActionView(MenuItem forItem) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.trash_option_item, null);
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onPerformDefaultAction();
            }

        });
        TextView badge = (TextView) view.findViewById(R.id.badge);
        badge.setText(String.valueOf(mBadgeCount));
        return view;
    }

    @Override
    public boolean onPerformDefaultAction() {
        OnClearCompletedListener listener = getOnClearCompletedListener();
        if (listener == null) {
            return false;
        }

        listener.onClearCompleted();
        return true;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public boolean isVisible() {
        return mBadgeCount > 0;
    }

    public void setOnClearCompletedListener(OnClearCompletedListener listener) {
        mListener = new WeakReference<>(listener);
    }

    private OnClearCompletedListener getOnClearCompletedListener() {
        if (mListener == null)
            return null;

        return mListener.get();
    }

}