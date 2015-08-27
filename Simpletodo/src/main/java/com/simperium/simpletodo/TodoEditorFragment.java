package com.simperium.simpletodo;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;


public class TodoEditorFragment extends DialogFragment implements TextView.OnEditorActionListener {

    private static final String ARG_KEY = "key";
    private static final String ARG_LABEL = "text";

    private static final int ACTION_ID = 0x100;

    private String mLabel;
    private String mKey;

    private EditText mEditText;

    private OnTodoEditorCompleteListener mListener;


    public static TodoEditorFragment newInstance(Todo todo) {
        return newInstance(todo.getSimperiumKey(), todo.getTitle());
    }

    private static TodoEditorFragment newInstance(String key, String label) {
        TodoEditorFragment fragment = new TodoEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_KEY, key);
        args.putString(ARG_LABEL, label);
        fragment.setArguments(args);
        return fragment;
    }

    public TodoEditorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mLabel = getArguments().getString(ARG_LABEL);
            mKey = getArguments().getString(ARG_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_todo_editor, container, false);
        mEditText = (EditText) view.findViewById(R.id.editor);
        mEditText.setText(mLabel);
        mEditText.setSelection(0, mLabel.length());
        mEditText.requestFocus();
        mEditText.setOnEditorActionListener(this);
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.edit_task_title);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTodoEditorCompleteListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTodoEditorCompleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != ACTION_ID)
            return false;

        mListener.onTodoEdited(mKey, mEditText.getText().toString());

        dismiss();

        return true;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTodoEditorCompleteListener {
        void onTodoEdited(String key, String label);
    }

}
