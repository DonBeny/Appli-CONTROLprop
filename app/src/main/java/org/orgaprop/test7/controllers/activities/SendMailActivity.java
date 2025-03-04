package org.orgaprop.test7.controllers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.orgaprop.test7.R;
import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Storage;
import org.orgaprop.test7.services.Contacts;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.utils.AndyUtils;
import org.orgaprop.test7.utils.UploadImage;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SendMailActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "SendMailActivity";

    private String typeCtrl;
    private Boolean isContrat;
    private String idPlan;

    private String imgAttach;
    private Bitmap bitmap = null;

//********* PUBLIC VARIABLES

    public static final String SEND_MAIL_ACTIVITY_TYPE = "type";
    public static final String SEND_MAIL_ACTIVITY_PROBLEM_TECH = "tech";
    public static final String SEND_MAIL_ACTIVITY_CTRL = "ctrl";
    public static final String SEND_MAIL_ACTIVITY_PLAN = "plan";
    public static final String SEND_MAIL_ACTUVUTY_IS_CONTRAT = "contra";

    public static final String SEND_MAIL_ACTIVITY_TITLE_TECH = "DESORDRE TECHNIQUE";
    public static final String SEND_MAIL_ACTIVITY_TITLE_CTRL = "RAPPORT";

    public static final int SEND_MAIL_ACTIVITY_REQUEST_CODE = 100;

//********* WIDGETS

    @BindView(R.id.send_mail_activity_title_txt) TextView mTitle;
    @BindView(R.id.send_mail_activity_dest_input_1) AutoCompleteTextView mDest1;
    @BindView(R.id.send_mail_activity_dest_input_2) AutoCompleteTextView mDest2;
    @BindView(R.id.send_mail_activity_dest_input_3) AutoCompleteTextView mDest3;
    @BindView(R.id.send_mail_activity_dest_input_4) AutoCompleteTextView mDest4;
    @BindView(R.id.send_mail_activity_text_input) EditText mMessage;
    @BindView(R.id.send_mail_activity_capture_btn) Button mCaptureBtn;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_send_mail);

        ButterKnife.bind(this);
        Intent intent = getIntent();
        getHistoDest();

        typeCtrl = intent.getStringExtra(SEND_MAIL_ACTIVITY_TYPE);
        isContrat = intent.getBooleanExtra(SEND_MAIL_ACTUVUTY_IS_CONTRAT, false);
        imgAttach = "";
        idPlan = "";

        mDest1.setOnFocusChangeListener((v, hasFocus) -> {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if( imm != null ) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        if( typeCtrl != null ) {
            if( typeCtrl.equals(SEND_MAIL_ACTIVITY_PROBLEM_TECH) ) {
                mTitle.setText(SEND_MAIL_ACTIVITY_TITLE_TECH);
                mDest2.setVisibility(View.INVISIBLE);
                mDest3.setVisibility(View.INVISIBLE);
                mDest4.setVisibility(View.INVISIBLE);
            }
            if( typeCtrl.equals(SEND_MAIL_ACTIVITY_CTRL) || typeCtrl.equals(SEND_MAIL_ACTIVITY_PLAN) ) {
                mTitle.setText(SEND_MAIL_ACTIVITY_TITLE_CTRL);
                mMessage.setVisibility(View.INVISIBLE);
                mCaptureBtn.setVisibility(View.INVISIBLE);
            }
            if( typeCtrl.equals(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE) ) {
                idPlan = intent.getStringExtra(SEND_MAIL_ACTIVITY_PLAN);
                mMessage.setVisibility(View.INVISIBLE);
                mCaptureBtn.setVisibility(View.INVISIBLE);
            }
        }

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if( imm != null ) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            if( !AndyUtils.isNetworkAcceptable(SendMailActivity.this) ) {
                //Toast.makeText(SendMailActivity.this, getResources().getString(R.string.conextion_lost), Toast.LENGTH_LONG).show();
                Storage storage = PrefDatabase.getInstance(SendMailActivity.this).mStorageDao().getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId())).getValue();

                if( storage != null ) {
                    StringBuilder dest = new StringBuilder();

                    dest.append(mDest1.getText().toString().trim());
                    if( !mDest2.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest2.getText().toString().trim());
                    }
                    if( !mDest3.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest3.getText().toString().trim());
                    }
                    if( !mDest4.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest4.getText().toString().trim());
                    }

                    storage.setSend_src((isContrat) ? "contra" : "proxi");
                    storage.setSend_dest(dest.toString());

                    if( typeCtrl.equals(SEND_MAIL_ACTIVITY_PLAN) ) {
                        storage.setSend_idPlan(Integer.parseInt(MakeCtrlActivity.fiche.getPlanAction("id")));
                    } else {
                        storage.setSend_dateCtrl(Integer.parseInt(MakeCtrlActivity.fiche.getCtrl()));
                    }

                    AndyUtils.ProtectResidence(SendMailActivity.this, storage);
                }
            } else {
                if( requestCode == SEND_MAIL_ACTIVITY_REQUEST_CODE ) {
                    if( resultCode == RESULT_OK  && data != null ) {
                        Bundle extras = data.getExtras();
                        if( extras != null ) {
                            bitmap = (Bitmap) extras.get("data");

                            uploadPicture();
                        }
                    } else {
                        Toast.makeText(SendMailActivity.this, "ECHEC RECUPERATION PRISE DE VUE", Toast.LENGTH_LONG).show();
                    }
                }
            }

            Looper.loop();
        });
    }

//********* PUBLIC FUNCTIONS

    public void sendMailActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "send": send(); break;
            case "capture": takePicture(); break;
            case "cancel": finishActivity(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void send() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            if( !AndyUtils.isNetworkAcceptable(SendMailActivity.this) ) {
                //Toast.makeText(SendMailActivity.this, getResources().getString(R.string.conextion_lost), Toast.LENGTH_LONG).show();
                Storage storage = PrefDatabase.getInstance(SendMailActivity.this).mStorageDao().getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId())).getValue();

                if( storage != null ) {
                    StringBuilder dest = new StringBuilder();

                    dest.append(mDest1.getText().toString().trim());
                    if( !mDest2.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest2.getText().toString().trim());
                    }
                    if( !mDest3.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest3.getText().toString().trim());
                    }
                    if( !mDest4.getText().toString().trim().equals("") ) {
                        dest.append(";");
                        dest.append(mDest4.getText().toString().trim());
                    }

                    storage.setSend_src((isContrat) ? "contra" : "proxi");
                    storage.setSend_dest(dest.toString());

                    if( typeCtrl.equals(SEND_MAIL_ACTIVITY_PLAN) ) {
                        storage.setSend_idPlan(Integer.parseInt(MakeCtrlActivity.fiche.getPlanAction("id")));
                    } else {
                        storage.setSend_dateCtrl(Integer.parseInt(MakeCtrlActivity.fiche.getCtrl()));
                    }

                    AndyUtils.ProtectResidence(SendMailActivity.this, storage);
                }
            } else if( !mDest1.getText().toString().equals("") || !mDest2.getText().toString().equals("") || !mDest3.getText().toString().equals("") || !mDest4.getText().toString().equals("") ) {
                String functionName = "send::";
                String cbl = MakeCtrlActivity.fiche.getId() + "a" + MakeCtrlActivity.fiche.getCtrl();

                HashMap<String, String> mapGet = new HashMap<>();
                HashMap<String, String> mapPost = new HashMap<>();

                mapPost.put("mbr", MainActivity.idMbr);
                mapPost.put("typ", typeCtrl);
                mapPost.put("dest", mDest1.getText().toString().trim());
                mapPost.put("msg", mMessage.getText().toString());

                if( isContrat ) {
                    mapGet.put("src", "contra");
                } else {
                    mapGet.put("src", "proxi");
                }
                if( typeCtrl.equals(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE) ) {
                    mapPost.put("plan", idPlan);
                }

                if( !mDest1.getText().toString().equals("") ) {
                    addHistoDest(mDest1.getText().toString().trim());
                }
                if( !mDest2.getText().toString().equals("") ) {
                    mapPost.put("dest2", mDest2.getText().toString().trim());
                    addHistoDest(mDest2.getText().toString().trim());
                }
                if( !mDest3.getText().toString().equals("") ) {
                    mapPost.put("dest3", mDest3.getText().toString().trim());
                    addHistoDest(mDest3.getText().toString().trim());
                }
                if( !mDest4.getText().toString().equals("") ) {
                    mapPost.put("dest4", mDest4.getText().toString().trim());
                    addHistoDest(mDest4.getText().toString().trim());
                }

                if( !imgAttach.equals("") ) {
                    mapPost.put("img", imgAttach.substring(0, imgAttach.indexOf(".")));
                }

                //Log.e(TAG, functionName+"cbl => "+cbl);
                //Log.e(TAG, functionName+"mapGet => "+mapGet);
                //Log.e(TAG, functionName+"mapPost => "+mapPost);

                try {
                    HttpTask task = new HttpTask(SendMailActivity.this, HttpTask.HTTP_TASK_ACT_SEND, cbl, mapGet, mapPost);
                    task.execute(MainActivity.ACCESS_CODE);

                    //Log.e(TAG, functionName+HttpTask.HTTP_TASK_ACT_SEND+" => "+cbl);
                    //Log.e(TAG, functionName+"mapGet => "+mapGet);
                    //Log.e(TAG, functionName+"mapPost => "+mapPost);

                    String result = task.get();

                    //Log.e(TAG, functionName+"result => "+result);

                    if( result != null && result.startsWith("1") ) {
                        Toast.makeText(SendMailActivity.this, "Mail envoyé avec succés", Toast.LENGTH_SHORT).show();

                        finish();
                    } else if( result != null ) {
                        Toast.makeText(SendMailActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SendMailActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                    }
                } catch(InterruptedException | ExecutionException | UnsupportedEncodingException e ) {
                    e.printStackTrace();
                }
            }

            Looper.loop();
        });
    }
    private void finishActivity() {
        finish();
    }
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if( intent.resolveActivity(getPackageManager()) != null ) {
            startActivityForResult(intent, SEND_MAIL_ACTIVITY_REQUEST_CODE);
        }
    }

    private void getHistoDest() {
        String functionName = "getHistoDest";

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            Contacts contacts = new Contacts(SendMailActivity.this);
            List<String> list = contacts.getListContacts();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(SendMailActivity.this, android.R.layout.simple_list_item_1, list);

            setAdapterOnThis(mDest1, adapter);
            setAdapterOnThis(mDest2, adapter);
            setAdapterOnThis(mDest3, adapter);
            setAdapterOnThis(mDest4, adapter);

            Looper.loop();
        });
    }
    private void addHistoDest(String dest) {
        String functionName = "addHistoDest";

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            Contacts contacts = new Contacts(SendMailActivity.this);
            List<String> list = contacts.getListContacts();
            StringTokenizer tokenizer = new StringTokenizer(dest, ";");

            while( tokenizer.hasMoreElements() ) {
                String line = tokenizer.nextToken();

                if( !list.contains(line) ) {
                    contacts.setContact(line.trim());
                }
            }

            Looper.loop();
        });
    }
    private void uploadPicture() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            imgAttach = (int)(System.currentTimeMillis()/1000) + ".png";

            //String image = AndyUtils.putBitmapToGallery(SendMailActivity.this, bitmap, imgAttach);

            new UploadImage(SendMailActivity.this, bitmap, imgAttach, UploadImage.UPLOAD_IMAGE_TYPE_SEND);

            Looper.loop();
        });
    }
    private void setAdapterOnThis(AutoCompleteTextView v, ArrayAdapter<String> a) {
        runOnUiThread(() -> {
            v.setAdapter(a);
            v.setThreshold(1);
            v.setOnClickListener(vv -> {
                v.showDropDown();
            });
            v.setAdapter(a);
        });
    }

}
