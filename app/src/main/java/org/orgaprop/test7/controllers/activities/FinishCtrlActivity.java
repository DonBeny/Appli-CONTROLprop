package org.orgaprop.test7.controllers.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.orgaprop.test7.R;
import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Storage;
import org.orgaprop.test7.services.CalendarServices;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.utils.AndyUtils;
import org.orgaprop.test7.utils.UploadImage;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FinishCtrlActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "FinishCtrlActivity";

    private SharedPreferences Preferences;
    private String noteCtrl;
    private String typeCtrl;
    private Boolean proxi;
    private Boolean contrat;

    private boolean isFinishClicked;
    private boolean isBusy;

    private boolean planActIsValid;
    private String planActDate;
    private String planActTxt;

    private boolean isSigned;
    private String sig1URI;
    private String sig2URI;
    private String sig2Agt;

    private String rapportDest;

//********* PUBLIC VARIABLES

    public static boolean resultUpload;
    public static boolean waitUpload;

    public static final String FINISH_ACTIVITY_RESULT = "finish";
    public static final String FINISH_ACTIVITY_TYPE_CTRL = "type";
    public static final String FINISH_ACTIVITY_PROXI = "proxi";
    public static final String FINISH_ACTIVITY_CONTRAT = "contrat";

    public static final int FINISH_ACTIVITY_RESULT_REQUEST_PLAN = 100;
    public static final int FINISH_ACTIVITY_RESULT_REQUEST_PLAN_VALID = 101;
    public static final int FINISH_ACTIVITY_RESULT_REQUEST_PLAN_NEW = 102;
    public static final int FINISH_ACTIVITY_RESULT_REQUEST_PLAN_CAL = 103;

    public static final String FINISH_ACTIVITY_REQUEST = "finish";

    public static final String FINISH_ACTIVITY_RESULT_REQUEST = "signed";
    public static final int FINISH_ACTIVITY_RESULT_REQUEST_NOT_SIGNED = 0;
    public static final int FINISH_ACTIVITY_RESULT_REQUEST_SIGNED = 1;

//********* WIDGETS

    @BindView(R.id.finish_ctrl_activity_plan_btn) LinearLayout mPlanAction;
    @BindView(R.id.finish_ctrl_activity_sign_btn) LinearLayout mSign;
    @BindView(R.id.finish_ctrl_activity_wait_img) pl.droidsonroids.gif.GifImageView mWaitImg;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_finish_ctrl);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        noteCtrl = intent.getStringExtra(MakeCtrlActivity.MAKE_CTRL_NOTE_CTRL);
        typeCtrl = intent.getStringExtra(FINISH_ACTIVITY_TYPE_CTRL);
        proxi = intent.getBooleanExtra(FINISH_ACTIVITY_PROXI, false);
        contrat = intent.getBooleanExtra(FINISH_ACTIVITY_CONTRAT, false);

        Preferences = getSharedPreferences(MainActivity.PREF_NAME_APPLI, MODE_PRIVATE);

        resultUpload = false;
        waitUpload = false;

        isFinishClicked = false;
        isBusy = false;

        planActIsValid = false;
        planActDate = "";
        planActTxt = "";

        isSigned = false;
        sig1URI = "";
        sig2URI = "";
        sig2Agt = "";

        rapportDest = "";

        if( Integer.parseInt(noteCtrl) < Integer.parseInt(Objects.requireNonNull(Preferences.getString(MainActivity.PREF_KEY_LIM_RAPPORT, "0"))) ) {
            startActivityAddPlanAction();
        }
    }


//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String functionName = "onActivityResult::";

        if( requestCode == FINISH_ACTIVITY_RESULT_REQUEST_PLAN ) {
            if( resultCode == FINISH_ACTIVITY_RESULT_OK && data != null ) {
                showWait(true);

                int result = data.getIntExtra(FINISH_ACTIVITY_RESULT, 0);

                //Log.e(TAG, functionName+"result => "+result);

                if( result == FINISH_ACTIVITY_RESULT_REQUEST_PLAN_VALID ) {
                    //Log.e(TAG, functionName+"validPlan");

                    validPlan();
                }
                if( result == FINISH_ACTIVITY_RESULT_REQUEST_PLAN_NEW || result == FINISH_ACTIVITY_RESULT_REQUEST_PLAN_CAL ) {
                    planActDate = Objects.requireNonNull(data.getStringExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE)).trim();
                    planActTxt = Objects.requireNonNull(data.getStringExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT)).trim();

                    //Log.e(TAG, functionName+"addPlan => "+planActDate+" => "+planActTxt);

                    addPlann();
                }
                if( result == FINISH_ACTIVITY_RESULT_REQUEST_PLAN_CAL ) {
                    if( !planActDate.equals("") && !planActTxt.equals("") ) {
                        //Log.e(TAG, functionName+"addAlert");

                        addAlert();
                    }
                }
            }
        }
        if( requestCode == FINISH_ACTIVITY_RESULT_REQUEST_SIGN ) {
            if( resultCode == FINISH_ACTIVITY_RESULT_OK && data != null ) {
                showWait(true);

                isSigned = true;

                sig1URI = data.getStringExtra(SignatureActivity.SIGNATURE_ACTIVITY_SIG1);
                sig2URI = data.getStringExtra(SignatureActivity.SIGNATURE_ACTIVITY_SIG2);
                sig2Agt = data.getStringExtra(SignatureActivity.SIGNATURE_ACTIVITY_AGT);

                mPlanAction.setEnabled(false);
                mSign.setEnabled(false);

                signCtrl();
            }
        }
    }
    @Override
    public void onBackPressed() {
        if( !isSigned ) {
            setResult(MakeCtrlActivity.MAKE_CTRL_RESULT_CANCEL);
            super.onBackPressed();
        } else {
            Toast.makeText(FinishCtrlActivity.this, getResources().getString(R.string.mess_use_finish), Toast.LENGTH_LONG).show();
        }
    }

//********* PUBLIC FUNCTIONS

    public void finishCtrlActivityActions(View v) {
        String functionName = "finishCtrlActivityActions::";

        if( !isBusy ) {
            String viewTag = v.getTag().toString();

            switch (viewTag) {
                case "planAction": startActivityAddPlanAction(); break;
                case "sign": startActivitySignature(); break;
                case "sendMail": startActivitySendMail(); break;
                case "valid": finishCtrl(); break;
                case "cancel": cancelFinishCtrl(); break;
            }
        }
    }

//********* PRIVATE FUNCTIONS

    private void startActivityAddPlanAction() {
        if( !isSigned ) {
            Intent intent = new Intent(FinishCtrlActivity.this, AddPlanActionActivity.class);

            intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, MakeCtrlActivity.fiche.getPlanAction("id"));
            intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, MakeCtrlActivity.fiche.getPlanAction("date"));
            intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, MakeCtrlActivity.fiche.getPlanAction("text"));
            intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TYPE, typeCtrl);

            startActivityForResult(intent, FINISH_ACTIVITY_RESULT_REQUEST_PLAN);
        }
    }
    private void startActivitySignature() {
        if( !isSigned ) {
            Intent intent = new Intent(FinishCtrlActivity.this, SignatureActivity.class);

            startActivityForResult(intent, FINISH_ACTIVITY_RESULT_REQUEST_SIGN);
        }
    }
    private void startActivitySendMail() {
        Intent intent = new Intent(FinishCtrlActivity.this, SendMailActivity.class);

        intent.putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_CTRL);

        if( !proxi && contrat ) {
            intent.putExtra(SendMailActivity.SEND_MAIL_ACTUVUTY_IS_CONTRAT, true);
        }

        startActivity(intent);
    }
    private void finishCtrl() {
        isFinishClicked = true;

        if( planActIsValid ) {
            validPlan();
        }
        if( !planActDate.equals("") || !planActTxt.equals("") ) {
            addPlann();
        }
        if( !sig1URI.equals("") || !sig2URI.equals("") ) {
            signCtrl();
        }

        if( !planActIsValid && planActDate.equals("") && planActTxt.equals("") && sig1URI.equals("") && sig2URI.equals("") ) {
            closeCtrl();
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                Looper.prepare();

                if( AndyUtils.isNetworkAcceptable(FinishCtrlActivity.this) ) {
                    String m = (!planActIsValid) ? "1" : "0";

                    m += (planActDate.equals("")) ? "1" : "0";
                    m += (planActTxt.equals("")) ? "1" : "0";
                    m += (sig1URI.equals("")) ? "1" : "0";
                    m += (sig2URI.equals("")) ? "1" : "0";

                    Toast.makeText(FinishCtrlActivity.this, "La totalité des données ne sont pas encore transmises. Ré-essayez plus tard ... (code: "+m+")", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(FinishCtrlActivity.this, "Récupérez une connection et ré-essayez plus tard ...", Toast.LENGTH_LONG).show();
                }

                Looper.loop();
            });

        }
    }
    private void cancelFinishCtrl() {
        if( !isSigned ) {
            setResult(MakeCtrlActivity.MAKE_CTRL_RESULT_CANCEL);
            finish();
        }
    }

    private void validPlan() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            if( AndyUtils.isNetworkAcceptable(FinishCtrlActivity.this) ) {
                HttpTask task = new HttpTask(FinishCtrlActivity.this, HttpTask.HTTP_TASK_ACT_VALID_PLAN, MakeCtrlActivity.fiche.getId(), "dat=" + MakeCtrlActivity.fiche.getCtrl() + "&plan=" + MakeCtrlActivity.fiche.getPlanAction("id") + "&typ=c", "mbr=" + MainActivity.idMbr);
                task.execute(MainActivity.ACCESS_CODE);

                try {
                    String result = task.get();

                    if( result != null && result.equals("1") ) {
                        MakeCtrlActivity.fiche.setPlanAction("new", Preferences.getString(MainActivity.PREF_KEY_CURRENT_DATE, ""), Preferences.getString(MainActivity.PREF_KEY_PLAN_ACTION, ""));
                        planActIsValid = false;
                    } else if( result != null ) {
                        planActIsValid = true;
                        Toast.makeText(FinishCtrlActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FinishCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                    }

                    showWait(false);
                } catch (InterruptedException | ExecutionException e) {
                    planActIsValid = true;
                    e.printStackTrace();
                }
            } else {
                Storage storage = PrefDatabase.getInstance(FinishCtrlActivity.this).mStorageDao().getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId())).getValue();

                if( storage != null ) {
                    storage.setPlan_validate(true);

                    AndyUtils.ProtectResidence(FinishCtrlActivity.this, storage);
                }
            }

            Looper.loop();
        });
    }
    private void addPlann() {
        String functionName = "addPlann::";
        String date = planActDate;
        String txt = planActTxt;

        if( !date.equals("") && !txt.equals("") ) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Looper.prepare();

                if( AndyUtils.isNetworkAcceptable(FinishCtrlActivity.this) ) {
                    String post = "mbr=" + MainActivity.idMbr;

                    MakeCtrlActivity.fiche.setPlanAction(date, txt);

                    post += "&id=" + MakeCtrlActivity.fiche.getPlanAction("id").trim();
                    post += "&dat=" + MakeCtrlActivity.fiche.getCtrl();
                    post += "&lim=" + date;
                    post += "&txt=" + txt;

                    HttpTask task = new HttpTask(FinishCtrlActivity.this, HttpTask.HTTP_TASK_ACT_PLAN, MakeCtrlActivity.fiche.getId(), "", post);
                    task.execute(MainActivity.ACCESS_CODE);

                    try {
                        String result = task.get();

                        if( result != null && result.startsWith("1") ) {
                            MakeCtrlActivity.fiche.setIdPlanAct(result.substring(1));

                            planActDate = "";
                            planActTxt = "";
                        } else if( result != null ) {
                            Toast.makeText(FinishCtrlActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(FinishCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                        }

                        showWait(false);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    Storage storage = PrefDatabase.getInstance(FinishCtrlActivity.this).mStorageDao().getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId())).getValue();

                    if( storage != null ) {
                        storage.setPlan_end(Integer.parseInt(date));
                        storage.setPlan_content(txt);
                        storage.setPlan_validate(false);

                        AndyUtils.ProtectResidence(FinishCtrlActivity.this, storage);
                    }
                }

                Looper.loop();
            });

        }
    }
    private void addAlert() {
        String functionName = "addAlert::";

        if( (ActivityCompat.checkSelfPermission(FinishCtrlActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) ) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Looper.prepare();

                //Log.e(TAG, functionName+"::addAlert => "+planActDate+" => "+planActTxt);

                //ContentResolver cr = getContentResolver();

                int year = Integer.parseInt(planActDate.substring(6, 10));
                int month = Integer.parseInt(planActDate.substring(3, 5)) - 1;
                int day = Integer.parseInt(planActDate.substring(0, 2));

                java.util.Calendar DStart = java.util.Calendar.getInstance();
                DStart.set(year, month, day, 8, 0);

                //java.util.Calendar DEnd = java.util.Calendar.getInstance();
                //DEnd.set(year, month, day, 18, 0);

                String mess = MakeCtrlActivity.fiche.getRef() + " " + MakeCtrlActivity.fiche.getNom() + "\r\nEntrée: " + MakeCtrlActivity.fiche.getEntree() + "\r\n" + MakeCtrlActivity.fiche.getAdress();

                /*long idEvent = CalendarServices.addEventTo(FinishCtrlActivity.this, 1, DStart, DEnd, "Plan d'actions", txt, MakeCtrlActivity.fiche.getNom(), false);

                if( idEvent < 0 ) {
                    Toast.makeText(FinishCtrlActivity.this, "Echec de l'enregistrement du rappel!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FinishCtrlActivity.this, "Rappel enregistré.", Toast.LENGTH_SHORT).show();
                }*/

                /*ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.DTSTART, String.valueOf(DStart));
                values.put(CalendarContract.Events.ALL_DAY, 1);
                values.put(CalendarContract.Events.TITLE, "Plan d'actions");
                values.put(CalendarContract.Events.DESCRIPTION, txt);
                values.put(CalendarContract.Events.EVENT_LOCATION, MakeCtrlActivity.fiche.getNom());
                values.put(CalendarContract.Events.CALENDAR_ID, calendarId);

                Log.e(TAG, functionName+"::values => ok");*/

                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.DTSTART, String.valueOf(DStart))
                        .putExtra(CalendarContract.Events.ALL_DAY, 1)
                        .putExtra(CalendarContract.Events.TITLE, "Plan d'actions")
                        .putExtra(CalendarContract.Events.DESCRIPTION, planActTxt)
                        .putExtra(CalendarContract.Events.EVENT_LOCATION, mess);

                //Log.e(TAG, functionName+"intent => ok");

                /*Intent calendarIntent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);

                calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, DStart.getTimeInMillis());
                calendarIntent.putExtra(CalendarContract., DEnd.getTimeInMillis());
                calendarIntent.putExtra(CalendarContract.Events.TITLE, "Plan d'actions");
                calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, MakeCtrlActivity.fiche.getNom());
                calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, txt);*/

                try {
                    //startActivity(calendarIntent);
                    startActivity(Intent.createChooser(intent, "Choisir le calendrier"));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(FinishCtrlActivity.this, "Aucune application pour poser un rappel", Toast.LENGTH_SHORT).show();
                }

                Looper.loop();
            });
        } else {
            Toast.makeText(FinishCtrlActivity.this, "Pas d'autorisation pour poser un rappel !!!", Toast.LENGTH_LONG).show();
        }
    }
    private void signCtrl() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String image1 = "";
            String image2 = "";

            String functionName = "signCtrl::";

            if( !sig1URI.equals("") && !sig1URI.isEmpty() && (sig1URI.trim().length() > 0) ) {
                try {
                    Size size = new Size(640, 640);
                    Bitmap bitmap1 = AndyUtils.StringToBitMap(sig1URI);// AndyUtils.getBitmapFromGallery(FinishCtrlActivity.this, sig1URI, size);

                    if (bitmap1 != null) {
                        image1 = (System.currentTimeMillis() / 1000) + ".png";

                        waitUpload = true;

                        new UploadImage(FinishCtrlActivity.this, bitmap1, image1, UploadImage.UPLOAD_IMAGE_TYPE_SIGNATURE_CTRL);

                        if( !sig2URI.equals("") && !sig2URI.isEmpty() && (sig2URI.trim().length() > 0) ) {
                            Bitmap bitmap2 = AndyUtils.StringToBitMap(sig2URI);// AndyUtils.getBitmapFromGallery(FinishCtrlActivity.this, sig2URI, size);

                            if (bitmap2 != null) {
                                image2 = "_" + (System.currentTimeMillis() / 1000) + ".png";

                                waitUpload = true;

                                new UploadImage(FinishCtrlActivity.this, bitmap2, image2, UploadImage.UPLOAD_IMAGE_TYPE_SIGNATURE_AGT);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            while( waitUpload ) {
                (new Handler()).postDelayed(null, 1000);
            }

            if( resultUpload && AndyUtils.isNetworkAcceptable(FinishCtrlActivity.this) ) {
                String strGet = "dat=" + MakeCtrlActivity.fiche.getCtrl() + "&sig1=" + image1 + "&sig2=" + image2 + "&agt=" + sig2Agt;
                HttpTask task = new HttpTask(FinishCtrlActivity.this, HttpTask.HTTP_TASK_ACT_SIGNATURE, MakeCtrlActivity.fiche.getId(), strGet, "mbr=" + MainActivity.idMbr);

                task.execute(MainActivity.ACCESS_CODE);

                try {
                    String result = task.get();

                    if( result != null && result.equals("1") ) {
                        sig1URI = "";
                        sig2URI = "";

                        Toast.makeText(FinishCtrlActivity.this, "Contrôle signé", Toast.LENGTH_SHORT).show();

                        if( isFinishClicked ) {
                            closeCtrl();
                        }
                    } else if( result != null ) {
                        Toast.makeText(FinishCtrlActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FinishCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                    }

                    showWait(false);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                Storage storage = PrefDatabase.getInstance(FinishCtrlActivity.this).mStorageDao().getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId())).getValue();

                if( storage != null ) {
                    storage.setCtrl_sig1(sig1URI);
                    storage.setCtrl_sig2(sig2URI);
                    storage.setCtrl_sig(sig2Agt);

                    AndyUtils.ProtectResidence(FinishCtrlActivity.this, storage);
                }
            }

            Looper.loop();
        });
    }
    private void closeCtrl() {
        if( Integer.parseInt(noteCtrl) < Integer.parseInt(Objects.requireNonNull(Preferences.getString(MainActivity.PREF_KEY_LIM_RAPPORT, "100"))) ) {
            String strGet = "src=" + ((!proxi && contrat) ? "contra" : "standar");
            String strPost = "mbr=" + MainActivity.idMbr + "&typ=ctrl&dest=silence";

            HttpTask task = new HttpTask(FinishCtrlActivity.this, HttpTask.HTTP_TASK_ACT_SEND, MakeCtrlActivity.fiche.getId()+"a"+MakeCtrlActivity.fiche.getCtrl(), strGet, strPost);

            task.execute(MainActivity.ACCESS_CODE);
        }

        setResult(FINISH_ACTIVITY_RESULT_OK);
        finish();
    }

    private void showWait(Boolean b) {
        if( b ) {
            isBusy = true;
            mWaitImg.setVisibility(View.VISIBLE);
        } else {
            mWaitImg.setVisibility(View.INVISIBLE);
            isBusy = false;
        }
    }

}
