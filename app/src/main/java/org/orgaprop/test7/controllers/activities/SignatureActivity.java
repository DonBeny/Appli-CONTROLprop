package org.orgaprop.test7.controllers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.github.gcacace.signaturepad.views.SignaturePad;

import org.orgaprop.test7.R;
import org.orgaprop.test7.utils.AndyUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignatureActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private boolean ctrlHasSigned = false;
    private boolean agtHasSigned = false;

//********* STATIC VARIABLES

    public static final String TAG = "SignatureActivity";

    public static final String SIGNATURE_ACTIVITY_SIG1 = "sig1";
    public static final String SIGNATURE_ACTIVITY_SIG2 = "sig2";
    public static final String SIGNATURE_ACTIVITY_AGT = "agt";

//********* WIDGETS

    @BindView(R.id.take_picture_activity_ctrl_signature_pad) SignaturePad mSignatureCtrl;
    @BindView(R.id.take_picture_activity_agt_signature_pad) SignaturePad mSignatureAgt;
    @BindView(R.id.take_picture_activity_ctrl_clear_btn) Button mClearCtrlBtn;
    @BindView(R.id.take_picture_activity_agt_clear_btn) Button mClearAgtBtn;
    @BindView(R.id.take_picture_activity_agt_name_input) EditText mNameAgtInput;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_signature);

        ButterKnife.bind(this);

        mNameAgtInput.setText(MakeCtrlActivity.fiche.getAgent());
        mNameAgtInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if( actionId == EditorInfo.IME_ACTION_DONE ) {
                InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if( imm != null ) {
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                }

                return true;
            }

            return false;
        });
        mNameAgtInput.setOnFocusChangeListener((v, hasFocus) -> {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if( imm != null ) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        mSignatureCtrl.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                mClearCtrlBtn.setEnabled(true);
                ctrlHasSigned = true;
                mSignatureAgt.setEnabled(true);
            }

            @Override
            public void onClear() {
                mClearCtrlBtn.setEnabled(false);
                ctrlHasSigned = false;
                mSignatureAgt.setEnabled(false);
            }
        });
        mSignatureAgt.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                mClearAgtBtn.setEnabled(true);
                agtHasSigned = true;
            }

            @Override
            public void onClear() {
                mClearAgtBtn.setEnabled(false);
                agtHasSigned = false;
            }
        });

        mClearCtrlBtn.setEnabled(false);
        mClearAgtBtn.setEnabled(false);

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if( imm != null ) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

//********* SURCHARGES

    @Override
    public void onBackPressed() {
        setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_CANCEL);
        super.onBackPressed();
    }

//********* PUBLIC FUNCTIONS

    public void signatureActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "clearPadCtrl": clearPadCtrl(); break;
            case "clearPadAgt": clearPadAgt(); break;
            case "valid": validSignature(); break;
            case "cancel": finishActivity(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void clearPadCtrl() {
        mSignatureCtrl.clear();
    }
    private void clearPadAgt() {
        mSignatureAgt.clear();
    }
    private void validSignature() {
        String sig1 = "";
        String sig2 = "";
        String agt = "";

        if( ctrlHasSigned ) {
            Bitmap imageBitmap = mSignatureCtrl.getSignatureBitmap();
            sig1 = AndyUtils.bitmapToString(imageBitmap);// mSignatureCtrl.getSignatureSvg();// imageBitmap.toString();

            //sig1 = AndyUtils.putBitmapToGallery(SignatureActivity.this, imageBitmap, "sig1.png");

            if( agtHasSigned ) {
                Bitmap imageBitmap2 = mSignatureAgt.getSignatureBitmap();
                sig2 = AndyUtils.bitmapToString(imageBitmap2);// mSignatureAgt.getSignatureSvg();

                //sig2 = AndyUtils.putBitmapToGallery(SignatureActivity.this, imageBitmap2, "sig2.png");
                agt = mNameAgtInput.getText().toString();
            }

            Intent intent = new Intent();

            intent.putExtra(SIGNATURE_ACTIVITY_SIG1, sig1);
            intent.putExtra(SIGNATURE_ACTIVITY_SIG2, sig2);
            intent.putExtra(SIGNATURE_ACTIVITY_AGT, agt);

            setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_OK, intent);
            finish();
        }
    }
    private void finishActivity() {
        setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_CANCEL);
        finish();
    }

}
