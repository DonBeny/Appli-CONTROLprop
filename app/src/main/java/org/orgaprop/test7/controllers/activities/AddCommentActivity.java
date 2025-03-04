package org.orgaprop.test7.controllers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.orgaprop.test7.R;
import org.orgaprop.test7.utils.AndyUtils;
import org.orgaprop.test7.utils.UploadImage;

import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddCommentActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private int cibleZone;
    private int cibleElement;
    private int cibleCriter;

    private String imgAttach;
    private Bitmap bitmap = null;

//********* STATIC VARIABLES

    public static final String TAG = "AddCommentActivity";

    public static final String ADD_COMMENT_ZONE = "zone";
    public static final String ADD_COMMENT_ELEMENT = "element";
    public static final String ADD_COMMENT_CRITER = "criter";

    public static final int ADD_COMMENT_REQUEST_RESULT = 100;

    public static final int ADD_COMMENT_REQUEST_RESULT_OK = 1;
    public static final int ADD_COMMENT_REQUEST_RESULT_CANCEL = 0;

//********* WIDGETS

    @BindView(R.id.add_comment_activity_comment_input) EditText mComment;
    @BindView(R.id.add_comment_activity_capture_img) ImageView mView;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_add_comment);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        String functionName = "onCreate::";

        cibleZone = intent.getIntExtra(ADD_COMMENT_ZONE, -1);
        cibleElement = intent.getIntExtra(ADD_COMMENT_ELEMENT, -1);
        cibleCriter = intent.getIntExtra(ADD_COMMENT_CRITER, -1);

        //Log.e(TAG, functionName+"cibleZone => "+cibleZone);
        //Log.e(TAG, functionName+"cibleElement => "+cibleElement);
        //Log.e(TAG, functionName+"cibleCriter => "+cibleCriter);

        if( (cibleZone >= 0) && (cibleElement >= 0) && (cibleCriter >= 0) ) {
            mComment.setText(MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).getComment());

            if( !MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).getCaptureUri().isEmpty() ) {
                Size size = new Size(640, 480);
                Bitmap bitmap = AndyUtils.StringToBitMap(MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).getCaptureUri());

                if( bitmap != null ) {
                    mView.setImageBitmap(bitmap);
                }
            }
        } else {
            finish();
        }

        mComment.setOnFocusChangeListener((v, hasFocus) -> {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if( imm != null ) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if( imm != null ) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == ADD_COMMENT_REQUEST_RESULT ) {
            if( resultCode == RESULT_OK && data != null ) {
                Bundle bundle = data.getExtras();

                if( bundle != null ) {
                    bitmap = (Bitmap) bundle.get("data");
                    mView.setImageBitmap(bitmap);
                }
            }
        }
    }
    @Override
    public void onBackPressed() {
        setResult(ADD_COMMENT_REQUEST_RESULT_CANCEL);
        super.onBackPressed();
    }

//********* PUBLIC FUNCTIONS

    public void addCommentActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "picture": takePicture(); break;
            case "save": saveComment(); break;
            case "cancel": cancelActivity(); break;
        }
    }


//********* PRIVATE FUNCTIONS

    private void uploadPicture() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            //Log.e(TAG, "uploadPicture::START");

            imgAttach = (int)(System.currentTimeMillis()/1000) + ".png";

            String image = AndyUtils.bitmapToString(bitmap);// AndyUtils.putBitmapToGallery(AddCommentActivity.this, bitmap, imgAttach);

            /*if( AndyUtils.isNetworkAcceptable(AddCommentActivity.this) ) {
                new UploadImage(AddCommentActivity.this, bitmap, imgAttach, UploadImage.UPLOAD_IMAGE_TYPE_CAPTURE);
            }*/

            //Log.e(TAG, "uploadPicture::zone => "+cibleZone);
            //Log.e(TAG, "uploadPicture::element => "+cibleElement);
            //Log.e(TAG, "uploadPicture::criter => "+cibleCriter);
            //Log.e(TAG, "uploadPicture::img => "+imgAttach);
            //Log.e(TAG, "uploadPicture::encodage => "+image);

            MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).setCapture(imgAttach);
            MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).setCaptureUri(image);

            //Log.e(TAG, "uploadPicture::END");

            Looper.loop();
        });
    }
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if( intent.resolveActivity(getPackageManager()) != null ) {
            startActivityForResult(intent, ADD_COMMENT_REQUEST_RESULT);
        }
    }
    private void cancelActivity() {
        setResult(ADD_COMMENT_REQUEST_RESULT_CANCEL);
        finish();
    }
    private void saveComment() {
        String functionName = "saveComment::";

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"cibleZone => "+cibleZone);
        //Log.e(TAG, functionName+"cibleElement => "+cibleElement);
        //Log.e(TAG, functionName+"cibleCriter => "+cibleCriter);
        //Log.e(TAG, functionName+"txt => "+mComment.getText().toString());

        MakeCtrlActivity.fiche.getZone(cibleZone).getElement(cibleElement).getCriter(cibleCriter).setComment(mComment.getText().toString());//.setCapture(mCapture.getTag().toString());

        if( bitmap != null ) {
            uploadPicture();
        }

        setResult(ADD_COMMENT_REQUEST_RESULT_OK);
        finish();
    }

}
