package org.orgaprop.test7.controllers.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.orgaprop.test7.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CommentActivity extends AppCompatActivity {

//********* STATIC VARIABLES

    public static final String TAG = "CommentActivity";

    public static final String COMMENT_ACTIVITY_TEXT_COMMENT = "comment";

//********* WIDGETS

    @BindView(R.id.comment_activity_txt) TextView mTextView;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        ButterKnife.bind(this);

        Intent intent = getIntent();

        mTextView.setText(intent.getStringExtra(COMMENT_ACTIVITY_TEXT_COMMENT));

    }

//********* SURCHARGES

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

//********* PUBLIC FUNCTIONS

    public void commentActivityActions(View v) {
        finish();
    }

}