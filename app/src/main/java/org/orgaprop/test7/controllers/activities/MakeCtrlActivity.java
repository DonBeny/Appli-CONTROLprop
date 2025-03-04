package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.CellCriterCtrlModel;
import org.orgaprop.test7.models.CellElmtCtrlModel;
import org.orgaprop.test7.models.CellZoneCtrlModel;
import org.orgaprop.test7.models.FicheResidModel;
import org.orgaprop.test7.models.ListResidModel;
import org.orgaprop.test7.models.NoteModel;
import org.orgaprop.test7.models.Storage;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.services.Storages;
import org.orgaprop.test7.utils.AndyUtils;
import org.orgaprop.test7.utils.UploadImage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MakeCtrlActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "MakeCtrlActivity";

    private String idRsd;
    private String typeCtrl;
    private String altCtrl;
    private String confCtrl;
    private String notCtrl;
    private String proxi;
    private String contra;

    private String idPlan = null;
    private String datePlan = null;
    private String textPlan = null;

    private ArrayList<String> agts;
    private JSONObject grill = new JSONObject();
    private JSONObject old = new JSONObject();
    private boolean isStorage = false;

    private boolean first;
    private boolean isSafe;
    private boolean isFinish;

//********* PUBLIC VARIABLES

    public static final String MAKE_CTRL_ID_RSD = "rsd";
    public static final String MAKE_CTRL_ALT_CTRL = "altCtrl";
    public static final String MAKE_CTRL_TYPE_CTRL = "typeCtrl";
    public static final String MAKE_CTRL_CONF_CTRL = "conf";
    public static final String MAKE_CTRL_METEO = "meteo";
    public static final String MAKE_CTRL_NOTE_CTRL = "note";

    public static final int MAKE_CTRL_ACTIVITY_PLAN_ACTION = 100;
    public static final int MAKE_CTRL_ACTIVITY_REQUEST_FINISH_CTRL_ACTIVITY = 200;
    public static final int MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY = 300;
    public static final int MAKE_CTRL_ACTIVITY_REQUEST_SEND_MAIL_ACTIVITY = 400;
    public static final int MAKE_CTRL_RESULT_OK = 1;
    public static final int MAKE_CTRL_RESULT_CANCEL = 0;

    public static int position;
    public static int meteo;
    public static FicheResidModel fiche;
    public static ArrayList<String> listCapture;
    public static ArrayList<Bitmap> listBitmap;

//********* WIDGETS

    @BindView(R.id.make_ctrl_activity_ref_rsd_txt) TextView mRefRsd;
    @BindView(R.id.make_ctrl_activity_name_rsd_txt) TextView mNameRsd;
    @BindView(R.id.make_ctrl_activity_entry_rsd_txt) TextView mEntryRsd;
    @BindView(R.id.make_ctrl_activity_adr_rsd_txt) TextView mAdrRsd;
    @BindView(R.id.make_ctrl_activity_ecdr_txt) TextView mNameEcdr;
    @BindView(R.id.make_ctrl_activity_adm_txt) TextView mNameGrd;
    @BindView(R.id.make_ctrl_activity_agt_txt) Spinner mNameAgt;
    @BindView(R.id.make_ctrl_activity_note_ctrl_txt) TextView mNoteCtrl;
    @BindView(R.id.make_ctrl_activity_list_zone_lyt) LinearLayout mGrill;
    @BindView(R.id.make_ctrl_activity_wait_img) pl.droidsonroids.gif.GifImageView mWaitImg;
    @BindView(R.id.make_ctrl_activity_scroll_lyt) ScrollView mScrollView;
    @BindView(R.id.make_ctrl_activity_wait_grill) pl.droidsonroids.gif.GifImageView mWaitGrill;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_make_ctrl);

        ButterKnife.bind(this);

        String functionName = "onCreate::";

        position = 0;
        fiche = new FicheResidModel();
        first = true;
        isSafe = true;
        isFinish = false;
        listCapture = new ArrayList<>();
        listBitmap = new ArrayList<>();
        agts = new ArrayList<>();
        notCtrl = "";

        Intent intent = getIntent();

        idRsd = intent.getStringExtra(MAKE_CTRL_ID_RSD);
        proxi = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_PROXY);
        contra = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_CONTRA);
        altCtrl = intent.getStringExtra(MAKE_CTRL_ALT_CTRL);
        typeCtrl = intent.getStringExtra(MAKE_CTRL_TYPE_CTRL);
        confCtrl = intent.getStringExtra(MAKE_CTRL_CONF_CTRL);
        meteo = intent.getIntExtra(MAKE_CTRL_METEO, 0);

        //Log.e(TAG, functionName+"typeCtrl => "+typeCtrl);
        //Log.e(TAG, functionName+"confCtrl => "+confCtrl);

        if( typeCtrl.equals("p") ) {
            idPlan = intent.getStringExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID);
            datePlan = intent.getStringExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE);
            textPlan = intent.getStringExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT);

            //Log.e(TAG, functionName+"idPlan => "+idPlan);
            //Log.e(TAG, functionName+"datePlan => "+datePlan);
            //Log.e(TAG, functionName+"textPlan => "+textPlan);
        }

        //Log.e(TAG, functionName+"idRsd => '"+idRsd+"'");
        //Log.e(TAG, functionName+"proxi => '"+proxi+"'");
        //Log.e(TAG, functionName+"contra => '"+contra+"'");
        //Log.e(TAG, functionName+"altCtrl => '"+altCtrl+"'");
        //Log.e(TAG, functionName+"typeCtrl => '"+typeCtrl+"'");
        //Log.e(TAG, functionName+"confCVtrl => '"+confCtrl+"'");
        //Log.e(TAG, functionName+"meteo => '"+meteo+"'");

        mNameAgt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                String agt = agts.get(position);

                if( !agt.equals("0") ) {
                    if( agt.equals("-1") ) agt = "0";

                    HttpTask task = new HttpTask(MakeCtrlActivity.this, HttpTask.HTTP_TASK_ACT_NEW_AGT, idRsd, "debugg=true&agt="+agt, "mbr=" + MainActivity.idMbr);
                    task.execute(MainActivity.ACCESS_CODE);

                    try {
                        String result = task.get();

                        if( result != null && result.equals("1") ) {
                            Toast.makeText(MakeCtrlActivity.this, "Agent mis a jour", Toast.LENGTH_SHORT).show();
                        } else if( result != null ) {
                            Toast.makeText(MakeCtrlActivity.this, result, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MakeCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //chargeResid();
        makeResid();
    }
    @Override
    protected void onResume() {
        super.onResume();

        showWait(false);
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();

        if( first ) {
            first = false;

            //MakeCtrlActivity.this.runOnUiThread(this::updateZones);
        } else {
            if( typeCtrl.equals("p") ) {
                finish();
            } else {
                if( !isFinish ) {
                    saveCtrl();
                } else {
                    isFinish = false;
                }

                //MakeCtrlActivity.this.runOnUiThread(this::updateZones);
            }
        }

        showWait(false);
    }

//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAKE_CTRL_ACTIVITY_REQUEST_FINISH_CTRL_ACTIVITY) {
            if (resultCode == FinishCtrlActivity.FINISH_ACTIVITY_RESULT_OK) {
                setResult(MAKE_CTRL_RESULT_OK);
                finish();
            } else {
                isFinish = false;
            }
        }
        if( requestCode == MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY ) {
            Intent intent = new Intent(MakeCtrlActivity.this, SendMailActivity.class);

            intent.putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_PLAN, idPlan);
            intent.putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, typeCtrl);

            startActivityForResult(intent, MAKE_CTRL_ACTIVITY_REQUEST_SEND_MAIL_ACTIVITY);
        }
        if( requestCode == MAKE_CTRL_ACTIVITY_REQUEST_SEND_MAIL_ACTIVITY ) {
            finishActivity();
        }
    }
    @Override
    public void onBackPressed() {
        saveCtrl();

        if( isSafe ) {
            setResult(MAKE_CTRL_RESULT_CANCEL);
            finish();
        }
    }

//********* PUBLIC FUNCTIONS

    public void makeCtrlActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "sendMail": startActivitySendMail(); break;
            case "valid": showWait(true); finishCtrl(); break;
            case "cancel": finishActivity(); break;
            case "comment": startActivityComment(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void finishCtrl() {
        String functionName = "finishCtrl::";

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"notCtrl => "+notCtrl);

        if( notCtrl.equals("") || notCtrl.equals("-1") ) {
            //Log.e(TAG, functionName+"END");

            finish();
        } else {
            saveCtrl();

            //
            if( !notCtrl.equals("!") ) {
                isFinish = true;
                Intent intent = new Intent(MakeCtrlActivity.this, FinishCtrlActivity.class);

                //Log.e(TAG, functionName+"Finish activity");

                intent.putExtra(MAKE_CTRL_NOTE_CTRL, notCtrl);
                intent.putExtra(FinishCtrlActivity.FINISH_ACTIVITY_TYPE_CTRL, typeCtrl);
                intent.putExtra(FinishCtrlActivity.FINISH_ACTIVITY_PROXI, (proxi.equals("true")));
                intent.putExtra(FinishCtrlActivity.FINISH_ACTIVITY_CONTRAT, (contra.equals("true")));

                startActivityForResult(intent, MAKE_CTRL_ACTIVITY_REQUEST_FINISH_CTRL_ACTIVITY);
            } else {
                //Log.e(TAG, functionName+"END");

                Toast.makeText(MakeCtrlActivity.this, R.string.mess_connexion_for_synchro, Toast.LENGTH_LONG).show();

                finish();
            }
        }
    }
    private void finishActivity() {
        saveCtrl();

        //if( isSafe ) {
        setResult(MAKE_CTRL_RESULT_CANCEL);
        finish();
        //}
    }

    private void startActivitySendMail() {
        Intent intent = new Intent(MakeCtrlActivity.this, SendMailActivity.class);

        intent.putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_PROBLEM_TECH);

        startActivity(intent);
    }
    private void startActivityComment() {
        Intent intent = new Intent(MakeCtrlActivity.this, CommentActivity.class);

        intent.putExtra(CommentActivity.COMMENT_ACTIVITY_TEXT_COMMENT, fiche.getCommentRsd());

        startActivity(intent);
    }

    private void chargeResid() {
        String functionName = "chargeResid::";

        mScrollView.setVisibility(View.INVISIBLE);
        mWaitGrill.setVisibility(View.VISIBLE);

        //Log.e(TAG, functionName+"START");

        /*Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            if( !AndyUtils.isNetworkAcceptable(MakeCtrlActivity.this) ) {
                //Toast.makeText(MakeCtrlActivity.this, getResources().getString(R.string.conextion_lost), Toast.LENGTH_LONG).show();
                Storages storages = new Storages(MakeCtrlActivity.this);
                Storage storage = storages.getStorage(idRsd);

                if( storage.getId() > 0 ) {
                    Log.e(TAG, functionName+"make resid from storage");

                    MakeCtrlActivity.this.runOnUiThread(() -> makeResid(storage));
                } else {
                    Log.e(TAG, functionName+"make resid from SelectActivity");

                    MakeCtrlActivity.this.runOnUiThread(() -> makeResid(SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd))));
                }
            } else {
                Log.e(TAG, functionName+"make resid from HttpTask");

                HttpTask task = new HttpTask(MakeCtrlActivity.this, HttpTask.HTTP_TASK_ACT_FICH, idRsd, "proxi="+proxi+"&contra="+contra, "mbr=" + MainActivity.idMbr);
                task.execute(MainActivity.ACCESS_CODE);

                try {
                    String result = task.get();

                    Log.e(TAG, functionName+"result => "+result);

                    if( result != null && result.startsWith("1") ) {
                        Log.e(TAG, functionName+"make resid from http => '"+result.substring(1)+"'");

                        MakeCtrlActivity.this.runOnUiThread(() -> makeResid(result.substring(1)));
                    } else if( result != null ) {
                        Toast.makeText(MakeCtrlActivity.this, result, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MakeCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                    }
                } catch( InterruptedException | ExecutionException e ) {
                    e.printStackTrace();
                }
            }

            Looper.loop();
        });*/
    }

    private void makeResid() {
        ListResidModel data = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd));

        mScrollView.setVisibility(View.GONE);
        mWaitGrill.setVisibility(View.VISIBLE);

        try {
            String ref_agt = proxi.equals("true") ? data.getRef_agent() : data.getRef_contra();

            fiche.setId(String.valueOf(data.getId()))
                    .setRef(data.getRef())
                    .setNom(data.getName())
                    .setEntree(data.getEntry())
                    .setAdress(data.getAdress())
                    .setEncadrant(data.getRef_sector())
                    .setGardien(data.getRef_admin())
                    .setAgent(ref_agt)
                    .setPlanAction(data.getPlanActions().getString("id"), data.getPlanActions().getString("date"), data.getPlanActions().getString("txt"));

            String entryRsd = "Entrée " + fiche.getEntree();

            mRefRsd.setText(fiche.getRef());
            mNameRsd.setText(fiche.getNom());
            mEntryRsd.setText(entryRsd);
            mAdrRsd.setText(fiche.getAdress());
            mNameEcdr.setText(fiche.getEncadrant());
            mNameGrd.setText(fiche.getGardien());
            notCtrl = data.getNote();

            ArrayList<String> constructor = new ArrayList<>();

            agts.add("0");
            constructor.add(fiche.getAgent());

            for( String agt : SelectActivity.idAgts ) {
                if( !agt.equals(fiche.getAgent()) ) {
                    agts.add(agt);
                    constructor.add(SelectActivity.nameAgts.get(SelectActivity.idAgts.indexOf(agt)));
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MakeCtrlActivity.this, android.R.layout.simple_spinner_item, constructor);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mNameAgt.setAdapter(adapter);

            if( typeCtrl.equals("p") ) {
                Intent intent2 = new Intent(MakeCtrlActivity.this, AddPlanActionActivity.class);

                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, idRsd);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_ALT_CTRL, altCtrl);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TYPE_CTRL, typeCtrl);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, idPlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, datePlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, textPlan);

                startActivityForResult(intent2, MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY);
            } else {
                chargeView();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void makeResid(String donn) {
        String functionName = "makeResid1::";
        StringTokenizer token = new StringTokenizer(donn, "§");

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"donn => '"+donn+"'");
        //Log.e(TAG, functionName+"token => '"+token+"'");

        fiche.setId(idRsd)
                .setRef(token.nextToken())
                .setNom(token.nextToken())
                .setEntree(token.nextToken())
                .setAdress(token.nextToken())
                .setCommentRsd(token.nextToken())
                .setEncadrant(token.nextToken())
                .setGardien(token.nextToken())
                .setAgent(token.nextToken())
                .setPlanAction(token.nextToken(), token.nextToken(), token.nextToken());

        String entryRsd = "Entrée " + fiche.getEntree();

        //Log.e(TAG, functionName+"ref rsd => "+fiche.getRef());
        //Log.e(TAG, functionName+"nom rsd => "+fiche.getNom());
        //Log.e(TAG, functionName+"entree rsd => "+fiche.getEntree());
        //Log.e(TAG, functionName+"adr rsd => "+fiche.getAdress());
        //Log.e(TAG, functionName+"comment rsd => "+fiche.getCommentRsd());
        //Log.e(TAG, functionName+"ecdr rsd => "+fiche.getEncadrant());
        //Log.e(TAG, functionName+"grd rsd => "+fiche.getGardien());
        //Log.e(TAG, functionName+"agt rsd => "+fiche.getAgent());
        //Log.e(TAG, functionName+"plan rsd => "+fiche.getPlanAction("id")+" -- "+fiche.getPlanAction("date")+" -- "+fiche.getPlanAction("text"));

        mRefRsd.setText(fiche.getRef());
        mNameRsd.setText(fiche.getNom());
        mEntryRsd.setText(entryRsd);
        mAdrRsd.setText(fiche.getAdress());
        mNameEcdr.setText(fiche.getEncadrant());
        mNameGrd.setText(fiche.getGardien());

        String mess = token.nextToken();

        //Log.e(TAG, functionName+"mess => '"+mess+"'");

        StringTokenizer tokenizer = new StringTokenizer(mess, "£");
        ArrayList<String> constructor = new ArrayList<>();

        agts.add("0");
        constructor.add(fiche.getAgent());

        while( tokenizer.hasMoreTokens() ) {
            String item = tokenizer.nextToken();

            //Log.e(TAG, functionName+"item => '"+item+"'");

            agts.add(item.substring(0, item.indexOf("=")));
            constructor.add(item.substring(item.indexOf("=")+1));
        }

        //Log.e(TAG, functionName+"constructor => '"+constructor.toString()+"'");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MakeCtrlActivity.this, android.R.layout.simple_spinner_item, constructor);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNameAgt.setAdapter(adapter);

        if( typeCtrl.equals("p") ) {
            Intent intent2 = new Intent(MakeCtrlActivity.this, AddPlanActionActivity.class);

            //Log.e(TAG, functionName+"start AddPlanActivity");

            intent2.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, idRsd);
            intent2.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
            intent2.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
            intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_ALT_CTRL, altCtrl);
            intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TYPE_CTRL, typeCtrl);
            intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, idPlan);
            intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, datePlan);
            intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, textPlan);

            startActivityForResult(intent2, MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY);
        } else {
            chargeView();
        }
    }
    private void makeResid(ListResidModel data) {
        String functionName = "makeResid2::";

        //Log.e(TAG, functionName+"START");

        try {
            fiche.setId(String.valueOf(data.getId()))
                    .setRef(data.getRef())
                    .setNom(data.getName())
                    .setEntree(data.getEntry())
                    .setAdress(data.getAdress())
                    .setEncadrant(data.getRef_sector())
                    .setGardien(data.getRef_admin())
                    .setAgent(data.getRef_agent())
                    .setPlanAction(data.getPlanActions().getString("id"), data.getPlanActions().getString("date"), data.getPlanActions().getString("txt"));

            //Log.e(TAG, functionName+"ref rsd => "+fiche.getRef());
            //Log.e(TAG, functionName+"nom rsd => "+fiche.getNom());
            //Log.e(TAG, functionName+"entree rsd => "+fiche.getEntree());
            //Log.e(TAG, functionName+"adr rsd => "+fiche.getAdress());
            //Log.e(TAG, functionName+"comment rsd => "+fiche.getCommentRsd());
            //Log.e(TAG, functionName+"ecdr rsd => "+fiche.getEncadrant());
            //Log.e(TAG, functionName+"grd rsd => "+fiche.getGardien());
            //Log.e(TAG, functionName+"agt rsd => "+fiche.getAgent());
            //Log.e(TAG, functionName+"plan rsd => "+fiche.getPlanAction("id")+" -- "+fiche.getPlanAction("date")+" -- "+fiche.getPlanAction("text"));

            String entryRsd = "Entrée " + fiche.getEntree();

            mRefRsd.setText(fiche.getRef());
            mNameRsd.setText(fiche.getNom());
            mEntryRsd.setText(entryRsd);
            mAdrRsd.setText(fiche.getAdress());
            mNameEcdr.setText(fiche.getEncadrant());
            mNameGrd.setText(fiche.getGardien());

            ArrayList<String> constructor = new ArrayList<>();

            agts.add("0");
            constructor.add(fiche.getAgent());

            for( String agt : SelectActivity.idAgts ) {
                if( !agt.equals(fiche.getAgent()) ) {
                    agts.add(agt);
                    constructor.add(SelectActivity.nameAgts.get(SelectActivity.idAgts.indexOf(agt)));
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MakeCtrlActivity.this, android.R.layout.simple_spinner_item, constructor);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mNameAgt.setAdapter(adapter);

            if( typeCtrl.equals("p") ) {
                Intent intent2 = new Intent(MakeCtrlActivity.this, AddPlanActionActivity.class);

                //Log.e(TAG, functionName+"start AddPlanActivity");

                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, idRsd);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_ALT_CTRL, altCtrl);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TYPE_CTRL, typeCtrl);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, idPlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, datePlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, textPlan);

                startActivityForResult(intent2, MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY);
            } else {
                chargeView();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void makeResid(Storage storage) {
        String functionName = "makeResid3::";

        //Log.e(TAG, functionName+"START");

        try{
            ListResidModel data = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd));

            fiche.setId(String.valueOf(storage.getResid()))
                    .setRef(data.getRef())
                    .setNom(data.getName())
                    .setEntree(data.getEntry())
                    .setAdress(data.getAdress())
                    .setEncadrant(data.getRef_sector())
                    .setGardien(data.getRef_admin())
                    .setAgent(data.getRef_agent())
                    .setPlanAction(data.getPlanActions().getString("id"), data.getPlanActions().getString("date"), storage.getPlan_content());

            //Log.e(TAG, functionName+"ref rsd => "+fiche.getRef());
            //Log.e(TAG, functionName+"nom rsd => "+fiche.getNom());
            //Log.e(TAG, functionName+"entree rsd => "+fiche.getEntree());
            //Log.e(TAG, functionName+"adr rsd => "+fiche.getAdress());
            //Log.e(TAG, functionName+"comment rsd => "+fiche.getCommentRsd());
            //Log.e(TAG, functionName+"ecdr rsd => "+fiche.getEncadrant());
            //Log.e(TAG, functionName+"grd rsd => "+fiche.getGardien());
            //Log.e(TAG, functionName+"agt rsd => "+fiche.getAgent());
            //Log.e(TAG, functionName+"plan rsd => "+fiche.getPlanAction("id")+" -- "+fiche.getPlanAction("date")+" -- "+fiche.getPlanAction("text"));

            String entryRsd = "Entrée " + fiche.getEntree();

            mRefRsd.setText(fiche.getRef());
            mNameRsd.setText(fiche.getNom());
            mEntryRsd.setText(entryRsd);
            mAdrRsd.setText(fiche.getAdress());
            mNameEcdr.setText(fiche.getEncadrant());
            mNameGrd.setText(fiche.getGardien());

            ArrayList<String> constructor = new ArrayList<>();

            agts.add("0");
            constructor.add(fiche.getAgent());

            for( String agt : SelectActivity.idAgts ) {
                if( !agt.equals(fiche.getAgent()) ) {
                    agts.add(agt);
                    constructor.add(SelectActivity.nameAgts.get(SelectActivity.idAgts.indexOf(agt)));
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MakeCtrlActivity.this, android.R.layout.simple_spinner_item, constructor);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mNameAgt.setAdapter(adapter);

            if( typeCtrl.equals("p") ) {
                Intent intent2 = new Intent(MakeCtrlActivity.this, AddPlanActionActivity.class);

                //Log.e(TAG, functionName+"start AddPlanActivity");

                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, idRsd);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
                intent2.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_ALT_CTRL, altCtrl);
                intent2.putExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TYPE_CTRL, typeCtrl);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, idPlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, datePlan);
                intent2.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, textPlan);

                startActivityForResult(intent2, MAKE_CTRL_ACTIVITY_REQUEST_PLAN_ACT_ACTIVITY);
            } else {
                chargeView();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void chargeView() {
        String functionName = "chargeView::";

        //Log.e(TAG, functionName+"START");

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            try{
                int pos = 0;
                JSONObject obj_proxi = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getProxi();
                Iterator<String> keys_proxi = obj_proxi.keys();
                JSONObject obj_contra = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getContra();
                Iterator<String> keys_contra = obj_contra.keys();

                //Log.e(TAG, functionName+"proxi : "+proxi);
                //Log.e(TAG, functionName+"contra : "+contra);

                if( !AndyUtils.isNetworkAcceptable(MakeCtrlActivity.this) ) {
                    Storages storages = new Storages(MakeCtrlActivity.this);
                    Storage storage = storages.getStorage(idRsd);

                    notCtrl = "!";
                    isSafe = false;

                    if( storage.getId() > 0 ) {
                        //Log.e(TAG, functionName+"use storage without network");

                        isStorage = true;
                        old = new JSONObject(storage.getCtrl_ctrl());
                    } else {
                        //Log.e(TAG, functionName+"use SelectActivity without network");

                        isStorage = false;
                        old = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getOldCtrl();
                    }
                } else {
                    //Log.e(TAG, functionName+"use SelectActivity grille with network");

                    isStorage = false;
                    old = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getOldCtrl();
                }

                if( proxi.equals("1") || proxi.equals("true") ) {
                    //Log.e(TAG, functionName+"obj_proxi => "+obj_proxi);

                    while( keys_proxi.hasNext() ) {
                        String kz = obj_proxi.getString(keys_proxi.next());

                        //Log.e(TAG, functionName+"add "+kz+" to grill");

                        grill.put(String.valueOf(pos), kz);
                        pos++;
                    }
                }
                if( contra.equals("1") || contra.equals("true") ) {
                    //Log.e(TAG, functionName+"obj_contra => "+obj_contra);

                    while( keys_contra.hasNext() ) {
                        String kz = obj_contra.getString(keys_contra.next());

                        //Log.e(TAG, functionName+"add "+kz+" to grill");

                        grill.put(String.valueOf(pos), kz);
                        pos++;
                    }
                }

                //Log.e(TAG, functionName+"old => "+old);
                //Log.e(TAG, functionName+"grill => "+grill);
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                makeModel();
            }

            /*if( !AndyUtils.isNetworkAcceptable(MakeCtrlActivity.this) ) {
                Storages storages = new Storages(MakeCtrlActivity.this);
                Storage storage = storages.getStorage(idRsd);

                notCtrl = "!";

                if( storage.getId() > 0 ) {
                    Log.e(TAG, functionName+"makeModel from storage");

                    makeModel(storage);
                } else {
                    Log.e(TAG, functionName+"makeModel from SelectActivity");
                    Log.e(TAG, functionName+"proxi => "+proxi);
                    Log.e(TAG, functionName+"contra => "+contra);

                    ListResidModel src = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd));

                    if( (proxi.equals("1") || proxi.equals("true")) && (contra.equals("1") || contra.equals("true")) ) {
                        int pos = 0;
                        JSONObject obj = new JSONObject();
                        JSONObject obj_proxi = src.getProxi();
                        Iterator<String> keys_proxi = obj_proxi.keys();
                        JSONObject obj_contra = src.getContra();
                        Iterator<String> keys_contra = obj_contra.keys();

                        try {
                            while( keys_proxi.hasNext() ) {
                                String kz = obj_proxi.getString(keys_proxi.next());

                                try {
                                    obj.put(String.valueOf(pos), kz);
                                    pos++;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            while( keys_contra.hasNext() ) {
                                String kz = obj_contra.getString(keys_contra.next());

                                try {
                                    obj.put(String.valueOf(pos), kz);
                                    pos++;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.e(TAG, functionName+"with proxi & contra");

                        makeModel(obj);
                    } else {
                        if( proxi.equals("1") || proxi.equals("true") ) {
                            Log.e(TAG, functionName+"with proxi");

                            makeModel(src.getProxi());
                        }
                        if( contra.equals("1") || contra.equals("true") ) {
                            Log.e(TAG, functionName+"with contra");

                            makeModel(src.getContra());
                        }
                    }
                }
            } else {
                Log.e(TAG, functionName+"use SelectActivity grille with network");

                ListResidModel src = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd));

                if( (proxi.equals("1") || proxi.equals("true")) && (contra.equals("1") || contra.equals("true")) ) {
                   int pos = 0;
                    JSONObject obj = new JSONObject();
                    JSONObject obj_proxi = src.getProxi();
                    Iterator<String> keys_proxi = obj_proxi.keys();
                    JSONObject obj_contra = src.getContra();
                    Iterator<String> keys_contra = obj_contra.keys();

                    try {
                        while( keys_proxi.hasNext() ) {
                            String kz = obj_proxi.getString(keys_proxi.next());

                            try {
                                obj.put(String.valueOf(pos), kz);
                                pos++;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        while( keys_contra.hasNext() ) {
                            String kz = obj_contra.getString(keys_contra.next());

                            try {
                                obj.put(String.valueOf(pos), kz);
                                pos++;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.e(TAG, functionName+"with proxi & contra");

                    makeModel(obj);
                } else {
                    if( proxi.equals("1") || proxi.equals("true") ) {
                        Log.e(TAG, functionName+"with proxi");

                        makeModel(src.getProxi());
                    }
                    if( contra.equals("1") || contra.equals("true") ) {
                        Log.e(TAG, functionName+"with contra");

                        makeModel(src.getContra());
                    }
                }
            }*/

            Looper.loop();
        });
    }

    private void makeModel() {
        String functionName = "makeModel::";
        int positionZone = 0;

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"structure => "+MainActivity.structure);
        //Log.e(TAG, functionName+"grill => "+grill);
        //Log.e(TAG, functionName+"old => "+old);

        try{
            Iterator<String> keys_zones = MainActivity.structure.keys();

            fiche.setCtrl(SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getDate().toString());

            while( keys_zones.hasNext() ) {
                String zone = keys_zones.next();
                boolean inData = false;
                Iterator<String> keys_grill = grill.keys();

                //Log.e(TAG, functionName+"key_zon => "+zone);

                while( keys_grill.hasNext() ) {
                    String key_test = keys_grill.next();

                    if( grill.getString(key_test).equals(zone) ) {
                        inData = true;
                    }
                }

                if( inData ) {
                    CellZoneCtrlModel modelZone = new CellZoneCtrlModel(positionZone);
                    JSONObject structure_zon = MainActivity.structure.getJSONObject(zone);
                    JSONObject structure_elm_zon = structure_zon.getJSONObject("e");
                    Iterator<String> keys_elements = structure_elm_zon.keys();
                    int positionElement = 0;

                    //Log.e(TAG, functionName+"inData");
                    //Log.e(TAG, functionName+"structure_zon => "+structure_zon);
                    //Log.e(TAG, functionName+"structure_elm_zon => "+structure_elm_zon);

                    modelZone.setId(zone);
                    modelZone.setCoef(Integer.parseInt(structure_zon.getString("coef")));
                    modelZone.setText(structure_zon.getString("txt"));

                    while( keys_elements.hasNext() ) {
                        String elm = keys_elements.next();
                        CellElmtCtrlModel modelElement = new CellElmtCtrlModel(positionElement);
                        JSONObject structure_elm = structure_elm_zon.getJSONObject(elm);
                        JSONObject structure_cri_elm = structure_elm.getJSONObject("c");
                        Iterator<String> keys_cri = structure_cri_elm.keys();
                        int positionCritter = 0;

                        //Log.e(TAG, functionName+"elm => "+elm);
                        //Log.e(TAG, functionName+"structure_elm => "+structure_elm);
                        //Log.e(TAG, functionName+"structure_cri_elm => "+structure_cri_elm);

                        modelElement.setId(elm);
                        modelElement.setCoef(Integer.parseInt(structure_elm.getString("coef")));
                        modelElement.setText(structure_elm.getString("txt"));

                        while( keys_cri.hasNext() ) {
                            String cri = keys_cri.next();
                            CellCriterCtrlModel modelCritter = new CellCriterCtrlModel(positionCritter);
                            JSONObject structure_cri = structure_cri_elm.getJSONObject(cri);

                            //Log.e(TAG, functionName+"cri => "+cri);
                            //Log.e(TAG, functionName+"structure_cri => "+structure_cri);

                            modelCritter.setId(cri);
                            modelCritter.setText(structure_cri.getString("txt"));
                            modelCritter.setCoef(Integer.parseInt(structure_cri.getString("coef")));

                            if( old.has(zone) && old.getJSONObject(zone).has(elm) && old.getJSONObject(zone).getJSONObject(elm).has(cri) ) {
                                //Log.e(TAG, functionName + "old => " + old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri));

                                if( isStorage ) {
                                    modelCritter.setValue(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getInt("value"));
                                    modelCritter.setComment(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getString("comment"));
                                    modelCritter.setCapture(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getJSONObject("capture").getString("name"));
                                    modelCritter.setCaptureUri(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getJSONObject("capture").getString("img"));
                                } else {
                                    modelCritter.setValue(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getInt("not"));
                                    modelCritter.setComment(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getJSONObject("com").getString("txt"));
                                    modelCritter.setCapture(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getJSONObject("com").getString("img"));
                                    modelCritter.setCaptureUri(old.getJSONObject(zone).getJSONObject(elm).getJSONObject(cri).getJSONObject("com").getString("scr"));
                                }
                            } else {
                                modelCritter.setValue(0);
                                modelCritter.setComment("");
                                modelCritter.setCapture("");
                                modelCritter.setCaptureUri("");
                            }

                            //Log.e(TAG, functionName+"modelCritter id => "+modelCritter.getId());
                            //Log.e(TAG, functionName+"modelCritter txt => "+modelCritter.getText());
                            //Log.e(TAG, functionName+"modelCritter coef => "+modelCritter.getCoef());
                            //Log.e(TAG, functionName+"modelCritter value => "+modelCritter.getValue());
                            //Log.e(TAG, functionName+"modelCritter comment => "+modelCritter.getComment());
                            //Log.e(TAG, functionName+"modelCritter capture => "+modelCritter.getCapture());

                            modelElement.addCriter(modelCritter);
                            positionCritter++;
                        }

                        modelZone.addElement(modelElement);
                        positionElement++;
                    }

                    fiche.addZone(modelZone);
                    positionZone++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            makeView();
        }
    }
    private void makeModel(String list) {
        String functionName = "makeModel1::";
        int positionZone = 0;
        int positionElement = 0;
        int positionCriter = 0;

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"list => "+list);

        StringTokenizer token = new StringTokenizer(list, "§");

        //Log.e(TAG, functionName+"token => "+token);

        CellZoneCtrlModel modelZone = new CellZoneCtrlModel(positionZone);
        CellElmtCtrlModel modelElement = new CellElmtCtrlModel(positionElement);

        try{
            String strToken = token.nextToken().trim();

            //Log.e(TAG, functionName+"ctrl => "+strToken);
            fiche.setCtrl(strToken);

            strToken = token.nextToken().trim();
            //Log.e(TAG, functionName+"note => "+strToken);
            notCtrl = strToken;

            while( token.hasMoreTokens() ) {
                strToken = token.nextToken().trim();

                switch( strToken.substring(0,1) ) {
                    case "1":
                        //Log.e(TAG, functionName+"case 1");

                        if( !modelElement.getId().equals("new") ) {
                            //Log.e(TAG, functionName+"addElement "+modelElement+" to "+modelZone);

                            modelZone.addElement(modelElement);

                            positionElement = 0;
                            positionCriter = 0;

                            modelElement = new CellElmtCtrlModel(positionElement);
                        }
                        if( !modelZone.getId().equals("new") ) {
                            //Log.e(TAG, functionName+"addZone "+modelZone);

                            fiche.addZone(modelZone);
                            positionZone++;

                            modelZone = new CellZoneCtrlModel(positionZone);
                        }

                        //Log.e(TAG, functionName+"new zone");

                        modelZone.setId(strToken.substring(1));
                        modelZone.setCoef(Integer.parseInt(token.nextToken().trim()));
                        modelZone.setText(token.nextToken().trim());

                        //Log.e(TAG, functionName+"done => "+modelZone);
                        break;
                    case "2":
                        //Log.e(TAG, functionName+"case 2");

                        if( !modelElement.getId().equals("new") ) {
                            //Log.e(TAG, functionName+"addElement "+modelElement+" to "+modelZone);

                            modelZone.addElement(modelElement);

                            positionElement++;
                            positionCriter = 0;

                            modelElement = new CellElmtCtrlModel(positionElement);
                        }

                        //Log.e(TAG, functionName+"new element");

                        modelElement.setId(strToken.substring(1));
                        modelElement.setCoef(Integer.parseInt(token.nextToken().trim()));
                        modelElement.setText(token.nextToken().trim());

                        //Log.e(TAG, functionName+"done => "+modelElement);
                        break;
                    case "3":
                        //Log.e(TAG, functionName+"case 3");

                        CellCriterCtrlModel modelCriter = new CellCriterCtrlModel(positionCriter);

                        //Log.e(TAG, functionName+"new critter");

                        modelCriter.setId(strToken.substring(1));
                        modelCriter.setValue(Integer.parseInt(token.nextToken().trim()));
                        modelCriter.setCoef(Integer.parseInt(token.nextToken().trim()));
                        modelCriter.setComment(token.nextToken().trim());

                        if( modelCriter.getComment().equals("vide") ) {
                            modelCriter.setComment("");
                        }

                        modelCriter.setCapture(token.nextToken().trim());

                        if( modelCriter.getCapture().equals("vide") ) {
                            modelCriter.setCapture("");
                        }

                        modelCriter.setText(token.nextToken().trim());

                        //Log.e(TAG, functionName+"done => "+modelCriter);
                        //Log.e(TAG, functionName+"add criter to "+modelElement);

                        modelElement.addCriter(modelCriter);
                        positionCriter++;
                        break;
                }
            }

            if( !modelElement.getId().equals("new") ) {
                //Log.e(TAG, functionName+"addElement "+modelElement+" to "+modelZone);

                modelZone.addElement(modelElement);
            }
            if( !modelZone.getId().equals("new") ) {
                //Log.e(TAG, functionName+"add zone "+modelZone);

                fiche.addZone(modelZone);
            }

            makeView();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    private void makeModel(JSONObject data) {
        String functionName = "makeModel2::";
        int positionZone = 0;

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"data => "+data);
        //Log.e(TAG, functionName+"structure => "+MainActivity.structure);

        try {
            //Iterator<String> zon_keys = data.keys();
            Iterator<String> zon_keys = MainActivity.structure.keys();
            JSONObject old = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getOldCtrl();

            fiche.setCtrl(SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd)).getDate().toString());
            //notCtrl = "!";

            while( zon_keys.hasNext() ) {
                String zon = zon_keys.next();
                boolean inData = false;
                Iterator<String> data_keys = data.keys();

                //Log.e(TAG, functionName+"key_zon => "+zon);

                while( data_keys.hasNext() ) {
                    String key_test = data_keys.next();

                    //Log.e(TAG, functionName+"test "+key_test+" => "+data.getString(key_test)+" => "+((data.getString(key_test).equals(zon)) ? "true" : "false"));

                    if( data.getString(key_test).equals(zon) ) {
                        inData = true;
                    }
                }

                if( inData ) {
                    CellZoneCtrlModel modelZone = new CellZoneCtrlModel(positionZone);
                    //String zon = key_zon;// data.getString(key_zon);

                    //Log.e(TAG, functionName + "zon => " + zon);

                    JSONObject data_zon = MainActivity.structure.getJSONObject(zon);// data.getJSONObject(zon);
                    JSONObject data_zon_elm = data_zon.getJSONObject("e");
                    Iterator<String> elm_keys = data_zon_elm.keys();
                    int positionElement = 0;

                    //Log.e(TAG, functionName + "data_zon => " + data_zon);
                    //Log.e(TAG, functionName + "data_zon_elm => " + data_zon_elm);

                    modelZone.setId(zon);
                    modelZone.setCoef(Integer.parseInt(data_zon.getString("coef")));
                    //modelZone.setCoef(Integer.parseInt(MainActivity.structure.getJSONObject(zon).getString("coef")));
                    modelZone.setText(data_zon.getString("txt"));
                    //modelZone.setText(MainActivity.structure.getJSONObject(zon).getString("txt"));

                    while (elm_keys.hasNext()) {
                        CellElmtCtrlModel modelElement = new CellElmtCtrlModel(positionElement);
                        String elm = elm_keys.next();

                        //Log.e(TAG, functionName + "elm => " + elm);

                        if (data_zon_elm.has(elm)) {
                            JSONObject data_elm = data_zon_elm.getJSONObject(elm);
                            JSONObject data_elm_cri = data_elm.getJSONObject("c");
                            //JSONObject data_structure_elm = MainActivity.structure.getJSONObject("e").getJSONObject(elm);
                            Iterator<String> cri_keys = data_elm_cri.keys();
                            int positionCriter = 0;

                            //Log.e(TAG, functionName + "data_elm => " + data_elm);
                            //Log.e(TAG, functionName + "data_elm_cri => " + data_elm_cri);

                            modelElement.setId(elm);
                            //modelElement.setCoef(Integer.parseInt(data_elm.getString("coef")));
                            //modelElement.setCoef(Integer.parseInt(data_structure_elm.getString("coef")));
                            modelElement.setCoef(Integer.parseInt(data_elm.getString("coef")));
                            //modelElement.setText(data_structure_elm.getString("txt"));
                            modelElement.setText(data_elm.getString("txt"));

                            while (cri_keys.hasNext()) {
                                CellCriterCtrlModel modelCritter = new CellCriterCtrlModel(positionCriter);
                                String cri = cri_keys.next();

                                //Log.e(TAG, functionName + "cri => " + cri);

                                if (data_elm_cri.has(cri)) {
                                    JSONObject data_cri = data_elm_cri.getJSONObject(cri);
                                    //JSONObject data_structure_cri = data_structure_elm.getJSONObject("c").getJSONObject(cri);

                                    //Log.e(TAG, functionName + "data_cri => " + data_cri);

                                    modelCritter.setId(cri);
                                    //modelCritter.setText(data_structure_cri.getString("txt"));
                                    modelCritter.setText(data_cri.getString("txt"));
                                    //modelCritter.setValue(Integer.parseInt(data_cri.getString("not")));
                                    //modelCritter.setCoef(Integer.parseInt(data_structure_cri.getString("coef")));
                                    modelCritter.setCoef(Integer.parseInt(data_cri.getString("coef")));

                                    if (old.has(zon) && old.getJSONObject(zon).has(elm) && old.getJSONObject(zon).getJSONObject(elm).has(cri)) {
                                        //Log.e(TAG, functionName + "old => " + old.getJSONObject(zon).getJSONObject(elm).getJSONObject(cri));

                                        modelCritter.setValue(old.getJSONObject(zon).getJSONObject(elm).getJSONObject(cri).getInt("not"));
                                        modelCritter.setComment(old.getJSONObject(zon).getJSONObject(elm).getJSONObject(cri).getJSONObject("com").getString("txt"));
                                        modelCritter.setCapture(old.getJSONObject(zon).getJSONObject(elm).getJSONObject(cri).getJSONObject("com").getString("img"));
                                        modelCritter.setCaptureUri("");
                                    } else {
                                        modelCritter.setValue(0);
                                        modelCritter.setComment("");
                                        modelCritter.setCapture("");
                                        modelCritter.setCaptureUri("");
                                    }

                                    //Log.e(TAG, functionName + "modelCritter id => " + modelCritter.getId());
                                    //Log.e(TAG, functionName + "modelCritter txt => " + modelCritter.getText());
                                    //Log.e(TAG, functionName + "modelCritter coef => " + modelCritter.getCoef());
                                    //Log.e(TAG, functionName + "modelCritter value => " + modelCritter.getValue());
                                    //Log.e(TAG, functionName + "modelCritter comment => " + modelCritter.getComment());
                                    //Log.e(TAG, functionName + "modelCritter capture => " + modelCritter.getCapture());

                                    modelElement.addCriter(modelCritter);
                                    positionCriter++;
                                } else {
                                    //Log.e(TAG, functionName + "not in structure");
                                }
                            }

                            modelZone.addElement(modelElement);
                            positionElement++;
                        } else {
                            //Log.e(TAG, functionName + "not in structure");
                        }
                    }

                    fiche.addZone(modelZone);
                    positionZone++;
                }
            }

            makeView();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void makeModel(Storage storage) {
        String functionName = "makeModel3::";
        int positionZone = 0;

        //Log.e(TAG, functionName+"START");

        fiche.setCtrl(String.valueOf(storage.getDate()));
        //notCtrl = "!";

        try{
            ListResidModel data_rsd = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(idRsd));
            JSONObject data_structure = new JSONObject();

            if (proxi.equals("1") && contra.equals("1")) {
                JSONObject obj = data_rsd.getProxi();
                Iterator<String> keys = data_rsd.getContra().keys();

                while (keys.hasNext()) {
                    String kz = keys.next();

                    try {
                        obj.put(kz, data_rsd.getContra().get(kz));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                data_structure = obj;
            } else {
                if (proxi.equals("1")) {
                    data_structure = data_rsd.getProxi();
                }
                if (contra.equals("1")) {
                    data_structure = data_rsd.getContra();
                }
            }

            //Log.e(TAG, functionName+"data_structure => "+data_structure);

            JSONObject data_ctrl = new JSONObject(storage.getCtrl_ctrl());
            Iterator<String> zon_keys = data_structure.keys();

            //Log.e(TAG, functionName+"data_ctrl => "+data_ctrl);

            while( zon_keys.hasNext() ) {
                CellZoneCtrlModel modelZone = new CellZoneCtrlModel(positionZone);
                String zon = zon_keys.next();
                JSONObject data_struct_zon = data_structure.getJSONObject(zon);
                JSONObject data_struct_zon_elm = data_struct_zon.getJSONObject("e");
                Iterator<String> elm_keys = data_struct_zon_elm.keys();
                JSONObject data_ctrl_zon = data_ctrl.getJSONObject(zon);
                int positionElement = 0;

                modelZone.setId(zon);
                modelZone.setCoef(Integer.parseInt(data_struct_zon.getString("coef")));
                modelZone.setText(data_struct_zon.getString("txt"));

                //Log.e(TAG, functionName+"zone => "+zon);
                //Log.e(TAG, functionName+"txt => "+modelZone.getText());
                //Log.e(TAG, functionName+"coef => "+modelZone.getCoef());

                while( elm_keys.hasNext() ) {
                    CellElmtCtrlModel modelElement = new CellElmtCtrlModel(positionElement);
                    String elm = elm_keys.next();
                    JSONObject data_struct_elm = data_struct_zon.getJSONObject(elm);
                    JSONObject data_struct_elm_cri = data_struct_elm.getJSONObject("c");
                    Iterator<String> cri_keys = data_struct_elm_cri.keys();
                    JSONObject data_ctrl_elm = data_ctrl_zon.getJSONObject(elm);
                    int positionCriter = 0;

                    modelElement.setId(elm);
                    modelElement.setCoef(Integer.parseInt(data_struct_elm.getString("coef")));
                    modelElement.setText(data_struct_elm.getString("txt"));

                    //Log.e(TAG, functionName+"element => "+elm);
                    //Log.e(TAG, functionName+"txt => "+modelElement.getText());
                    //Log.e(TAG, functionName+"coef => "+modelElement.getCoef());

                    while( cri_keys.hasNext() ) {
                        CellCriterCtrlModel modelCriter = new CellCriterCtrlModel(positionCriter);
                        String cri = cri_keys.next();
                        JSONObject data_struct_cri = data_struct_elm.getJSONObject(cri);
                        JSONObject data_ctrl_cri = data_ctrl_elm.getJSONObject(cri);

                        modelCriter.setId(cri);
                        modelCriter.setText(data_struct_cri.getString("txt"));
                        modelCriter.setValue(Integer.parseInt(data_ctrl_cri.getString("value")));
                        modelCriter.setCoef(Integer.parseInt(data_struct_cri.getString("coef")));

                        //Log.e(TAG, functionName+"critter => "+cri);
                        //Log.e(TAG, functionName+"txt => "+modelCriter.getText());
                        //Log.e(TAG, functionName+"coef => "+modelCriter.getCoef());
                        //Log.e(TAG, functionName+"value => "+modelCriter.getValue());
                        //Log.e(TAG, functionName+"comment => "+data_ctrl_cri.getString("comment"));

                        if( !data_ctrl_cri.getString("comment").equals("vide") ) {
                            modelCriter.setComment(data_ctrl_cri.getString("comment"));
                        }

                        modelElement.addCriter(modelCriter);
                        positionCriter++;
                    }

                    modelZone.addElement(modelElement);
                    positionElement++;
                }

                fiche.addZone(modelZone);
                positionZone++;
            }

            makeView();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void makeView() {
        String functionName = "makeView::";
        int columnIndex = 0;
        int rowIndex = 0;
        int rowCount = fiche.getZones().size() / 3;

        ArrayList<View> listLine = new ArrayList<>();

        //Log.e(TAG, functionName+"START");
        //Log.e(TAG, functionName+"rowCount => "+rowCount);

        for( int i = 0; i < rowCount+1; i++ ) {
            listLine.add(LayoutInflater.from(MakeCtrlActivity.this).inflate(R.layout.line_zones_layout, null));
        }

        for( CellZoneCtrlModel item : fiche.getZones() ) {
            View viewZone = new View(MakeCtrlActivity.this);
            ImageView imageView = new ImageView(MakeCtrlActivity.this);
            TextView textViewZone = new TextView(MakeCtrlActivity.this);
            TextView textViewNote = new TextView(MakeCtrlActivity.this);

            if( columnIndex == 3 ) {
                int finalRowIndex = rowIndex;

                MakeCtrlActivity.this.runOnUiThread(() -> {
                    mGrill.addView(listLine.get(finalRowIndex));
                });

                columnIndex = 0;
                rowIndex++;
            }

            //Log.e(TAG, functionName+"rowIndex => "+rowIndex);
            //Log.e(TAG, functionName+"columnIndex => "+columnIndex);

            switch( columnIndex ) {
                case 0:
                    viewZone = listLine.get(rowIndex).findViewById(R.id.item_zone_1);
                    imageView = listLine.get(rowIndex).findViewById(R.id.zone_1_item_zone_img);
                    textViewNote = listLine.get(rowIndex).findViewById(R.id.zone_1_item_zone_note);
                    textViewZone = listLine.get(rowIndex).findViewById(R.id.zone_1_item_zone_text);
                    item.setView(R.id.zone_1_item_zone_note);
                    break;
                case 1:
                    viewZone = listLine.get(rowIndex).findViewById(R.id.item_zone_2);
                    imageView = listLine.get(rowIndex).findViewById(R.id.zone_2_item_zone_img);
                    textViewNote = listLine.get(rowIndex).findViewById(R.id.zone_2_item_zone_note);
                    textViewZone = listLine.get(rowIndex).findViewById(R.id.zone_2_item_zone_text);
                    item.setView(R.id.zone_2_item_zone_note);
                    break;
                case 2:
                    viewZone = listLine.get(rowIndex).findViewById(R.id.item_zone_3);
                    imageView = listLine.get(rowIndex).findViewById(R.id.zone_3_item_zone_img);
                    textViewNote = listLine.get(rowIndex).findViewById(R.id.zone_3_item_zone_note);
                    textViewZone = listLine.get(rowIndex).findViewById(R.id.zone_3_item_zone_text);
                    item.setView(R.id.zone_3_item_zone_note);
                    break;
            }
            switch( item.getId() ) {
                case "1": imageView.setImageResource(R.drawable.abords_acces_immeubles_vert); break;
                case "2": imageView.setImageResource(R.drawable.hall_vert); break;
                case "3":
                case "16": imageView.setImageResource(R.drawable.ascenseur_vert); break;
                case "4": imageView.setImageResource(R.drawable.escalier_vert); break;
                case "5": imageView.setImageResource(R.drawable.paliers_coursives_vert); break;
                case "17":
                case "6": imageView.setImageResource(R.drawable.local_om_vert); break;
                case "7": imageView.setImageResource(R.drawable.local_velo_vert); break;
                case "8": imageView.setImageResource(R.drawable.cave_vert); break;
                case "9": imageView.setImageResource(R.drawable.parking_sous_sol_vert); break;
                case "10": imageView.setImageResource(R.drawable.cour_interieure_vert); break;
                case "11": imageView.setImageResource(R.drawable.parking_exterieur_vert); break;
                case "12": imageView.setImageResource(R.drawable.espaces_exterieurs_vert); break;
                case "13": imageView.setImageResource(R.drawable.icone_bureau_vert); break;
                case "14": imageView.setImageResource(R.drawable.salle_commune_vert); break;
                case "15": imageView.setImageResource(R.drawable.buanderie_vert); break;
                default: imageView.setImageResource(R.drawable.localisation_vert); break;
            }

            textViewNote.setText(R.string.lbl_so);
            textViewZone.setText(item.getText());
            viewZone.setTag(item.getPosition());

            //Log.e(TAG, functionName+"text => '"+textViewZone.getText().toString()+"'");
            ///Log.e(TAG, functionName+"tag => '"+viewZone.getTag().toString()+"'");

            viewZone.setOnClickListener(view -> {
                Intent intent = new Intent(MakeCtrlActivity.this, CtrlZoneActivity.class);

                intent.putExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_ZONE, Integer.parseInt(view.getTag().toString()));

                startActivity(intent);
            });

            columnIndex++;
        }

        if( columnIndex == 0 ) {
            Toast.makeText(MakeCtrlActivity.this, "Aucune zone à contrôler avec ces paramêtres.", Toast.LENGTH_LONG).show();
        } else {
            if( columnIndex < 3 ) {
                for( int i = columnIndex + 1; i < 4; i++ ) {
                    switch( i ) {
                        case 2:
                            listLine.get(rowIndex).findViewById(R.id.item_zone_2).setVisibility(View.INVISIBLE);
                            break;
                        case 3:
                            listLine.get(rowIndex).findViewById(R.id.item_zone_3).setVisibility(View.INVISIBLE);
                            break;
                    }
                }
            }

            int finalRowIndex1 = rowIndex;
            MakeCtrlActivity.this.runOnUiThread(() -> {
                mWaitGrill.setVisibility(View.INVISIBLE);
                mScrollView.setVisibility(View.VISIBLE);
                mGrill.setVisibility(View.VISIBLE);
                mGrill.addView(listLine.get(finalRowIndex1));
                updateZones();
            });
        }
    }

    private void updateZones() {
        String functionName = "updateZones::";
        NoteModel noteCtrl = new NoteModel();
        SharedPreferences Preferences = getSharedPreferences(MainActivity.PREF_NAME_APPLI, MODE_PRIVATE);
        int limTop = Integer.parseInt(Objects.requireNonNull(Preferences.getString(MainActivity.PREF_KEY_LIMIT_TOP, "-1")));
        int limDown = Integer.parseInt(Objects.requireNonNull(Preferences.getString(MainActivity.PREF_KEY_LIMIT_DOWN, "-1")));

        //Log.e(TAG, functionName+"START => "+((isSafe) ? "true" : "false"));

        if( isSafe ) {
            //Log.e(TAG, functionName+"calc notes => "+notCtrl);

            for (CellZoneCtrlModel item : fiche.getZones()) {
                View view = mGrill.findViewWithTag(item.getPosition());

                TextView cbl = (TextView) view.findViewById(item.getView());
                NoteModel note = item.note();
                String mess = "";
                int calc = -1;

                //Log.e(TAG, functionName+"cbl => '"+item+"'");

                if( !notCtrl.equals("!") ) {
                    if (note.max > 0) {
                        calc = (int) ((note.note * 100) / note.max);

                        if (calc > 100) calc = 100;

                        mess = calc + " %";

                        noteCtrl.note += note.note;
                        noteCtrl.max += note.max;
                    } else {
                        mess = "SO";
                    }
                } else if( isSafe ) {
                    mess = getResources().getString(R.string.lbl_so);
                } else {
                    mess = "!";
                }

                cbl.setText(mess);

                //Log.e(TAG, functionName+"note => '"+cbl.getText().toString()+"'");

                if( !notCtrl.equals("!") && (calc >= 0) && (limDown >= 0) && (limTop >= 0)) {
                    if (calc < limDown) {
                        cbl.setTextColor(ContextCompat.getColor(MakeCtrlActivity.this, R.color._red));
                    } else if( calc >= limTop ) {
                        cbl.setTextColor(ContextCompat.getColor(MakeCtrlActivity.this, R.color._light_green));
                    } else {
                        cbl.setTextColor(ContextCompat.getColor(MakeCtrlActivity.this, R.color._orange));
                    }
                } else {
                    cbl.setTextColor(ContextCompat.getColor(MakeCtrlActivity.this, R.color._dark_grey));
                }
            }

            if (noteCtrl.max > 0) {
                //int calc = (int) ((noteCtrl.note * 100) / noteCtrl.max);

                //if (calc > 100) calc = 100;

                String mess = (notCtrl.equals("!")) ? "!" : notCtrl + " %";
                mNoteCtrl.setText(mess);

                if( !notCtrl.equals("!") ) {
                    int calc = Integer.parseInt(notCtrl);
                    //notCtrl = String.valueOf(calc);

                    //Log.e(TAG, functionName+"note ctrl => '"+mess+"'");

                    if ((calc >= 0) && (limDown >= 0) && (limTop >= 0)) {
                        if (calc < limDown) {
                            mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_red));
                        } else {
                            if (calc >= limTop) {
                                mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_green));
                            } else {
                                mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_orange));
                            }
                        }
                    }
                } else {
                    mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_grey));
                }
            } else {
                //Log.e(TAG, functionName+"note ctrl => 'S O'");

                mNoteCtrl.setText(getResources().getString(R.string.lbl_so));
                mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_grey));
            }
        } else {
            //Log.e(TAG, functionName+"no network");

            for (CellZoneCtrlModel item : fiche.getZones()) {
                View view = mGrill.findViewWithTag(item.getPosition());
                TextView cbl = (TextView) view.findViewById(item.getView());

                cbl.setText("!");
                cbl.setTextColor(ContextCompat.getColor(MakeCtrlActivity.this, R.color._dark_grey));
            }

            mNoteCtrl.setText("!");
            mNoteCtrl.setBackground(ContextCompat.getDrawable(MakeCtrlActivity.this, R.drawable.ctrl_note_grey));
        }
    }

    private void secureCtrl() {
        String functionName = "secureCtrl::";

        //Log.e(TAG, functionName+"START");

        isSafe = false;
        notCtrl = "!";

        Storage storage = new Storage();
        //JSONObject captures = new JSONObject();
        StringBuilder ctrl_type = new StringBuilder();
        JSONObject grill = new JSONObject();
        //JSONObject comment = new JSONObject();

        /*if( listCapture.size() > 0 ) {
            int i = 0;

            for( String capture : listCapture ) {
                try {
                    captures.put(capture, AndyUtils.bitMapToString(listBitmap.get(i)));
                    i++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }*/
        if( proxi.equals("true") ) {
            ctrl_type.append("proxi");
        }
        ctrl_type.append(";");
        if( contra.equals("true") ) {
            ctrl_type.append("contra");
        }

        try {
            for( CellZoneCtrlModel zone : fiche.getZones() ) {
                boolean bZone = false;
                //StringBuilder zoneBuilder = new StringBuilder();
                JSONObject zoneBuilder = new JSONObject();

                for( CellElmtCtrlModel element : zone.getElements() ) {
                    boolean bElement = false;
                    //StringBuilder elementBuilder = new StringBuilder();
                    JSONObject elementBuilder = new JSONObject();

                    for( CellCriterCtrlModel criter : element.getCriters() ) {
                        //String vCriter = criter.getId() + "=" + criter.getValue();

                        JSONObject vCriter = new JSONObject();
                        JSONObject capture = new JSONObject();

                        vCriter.put("id", criter.getId());
                        vCriter.put("value", criter.getValue());
                        vCriter.put("comment", criter.getComment());

                        capture.put("name", criter.getCapture());
                        capture.put("img", criter.getCaptureUri());
                        vCriter.put("capture", capture);

                        elementBuilder.put(criter.getId(), vCriter);

                        /*if( elementBuilder.length() > 0 ) {
                            elementBuilder.append(",");
                        }
                        elementBuilder.append(vCriter);

                        if( criter.hasComment() || criter.hasCapture() ) {
                            String vComment = zone.getId() + "_" + element.getId() + "_" + criter.getId() + "#02" + hashComment(criter.getComment()) + "£" + criter.getCapture();

                            if( comment.length() > 0 ) {
                                comment.append("#01");
                            }

                            comment.append(vComment);
                        }

                        bElement = true;*/
                    }

                    zoneBuilder.put(element.getId(), elementBuilder);
                    /*if( bElement ) {
                        bZone = true;
                        String vElement = element.getId() + ":" + elementBuilder;

                        if( zoneBuilder.length() > 0 ) {
                            zoneBuilder.append(";");
                        }

                        zoneBuilder.append(vElement);
                    }*/
                }

                grill.put(zone.getId(), zoneBuilder);
                /*if( bZone ) {
                    String vZone = zone.getId() + "_" + zoneBuilder;

                    if( grill.length() > 0 ) {
                        grill.append("£");
                    }

                    grill.append(vZone);
                }*/
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        storage.setResid(Integer.parseInt(idRsd));
        storage.setDate(Integer.parseInt(fiche.getCtrl()));
        storage.setConfig(confCtrl);
        storage.setTypeCtrl(typeCtrl);
        storage.setCtrl_type(ctrl_type.toString());
        storage.setCtrl_ctrl(grill.toString());

        //Log.e(TAG, functionName+"storage => "+storage);

        AndyUtils.ProtectResidence(MakeCtrlActivity.this, storage);
    }
    private void saveCtrl() {
        String functionName = "saveCtrl::";

        //Log.e(TAG, functionName+"START");

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            if( AndyUtils.isNetworkAcceptable(MakeCtrlActivity.this) ) {
                String idCtrl = fiche.getId() + "a" + fiche.getCtrl();
                String confCtrl = typeCtrl + "£" + MakeCtrlActivity.this.confCtrl;
                StringBuilder grillBuilder = new StringBuilder();
                StringBuilder commentBuilder = new StringBuilder();

                //Log.e(TAG, functionName+"idCtrl => "+idCtrl);
                //Log.e(TAG, functionName+"confCtrl => "+confCtrl);

                showWait(true);

                for( CellZoneCtrlModel zone : fiche.getZones() ) {
                    boolean bZone = false;
                    StringBuilder zoneBuilder = new StringBuilder();

                    for( CellElmtCtrlModel element : zone.getElements() ) {
                        boolean bElement = false;
                        StringBuilder elementBuilder = new StringBuilder();

                        for( CellCriterCtrlModel criter : element.getCriters() ) {
                            String vCriter = criter.getId() + "=" + criter.getValue();

                            if( elementBuilder.length() > 0 ) {
                                elementBuilder.append(",");
                            }
                            elementBuilder.append(vCriter);

                            if( criter.hasComment() || criter.hasCapture() ) {
                                String vComment = zone.getId() + "_" + element.getId() + "_" + criter.getId() + "#02" + hashComment(criter.getComment()) + "£" + criter.getCapture();
                                Bitmap img = AndyUtils.StringToBitMap(criter.getCaptureUri());

                                if( img != null ) {
                                    new UploadImage(MakeCtrlActivity.this, img, criter.getCapture(), UploadImage.UPLOAD_IMAGE_TYPE_CAPTURE);
                                }

                                if( commentBuilder.length() > 0 ) {
                                    commentBuilder.append("#01");
                                }

                                commentBuilder.append(vComment);
                            }

                            bElement = true;
                        }

                        if( bElement ) {
                            bZone = true;
                            String vElement = element.getId() + ":" + elementBuilder;

                            if( zoneBuilder.length() > 0 ) {
                                zoneBuilder.append(";");
                            }

                            zoneBuilder.append(vElement);
                        }
                    }

                    if( bZone ) {
                        String vZone = zone.getId() + "_" + zoneBuilder;

                        if( grillBuilder.length() > 0 ) {
                            grillBuilder.append("£");
                        }

                        grillBuilder.append(vZone);
                    }
                }

                if( grillBuilder.length() > 0 ) {
                    String postBuilder = "&conf=" + confCtrl;

                    postBuilder += "&grill=" + grillBuilder;
                    postBuilder += "&comment=" + commentBuilder;
                    postBuilder += "&note=" + mNoteCtrl.getText().toString();

                    //Log.e(TAG, functionName+"postBuilder => "+postBuilder);

                    HttpTask task = new HttpTask(MakeCtrlActivity.this, HttpTask.HTTP_TASK_ACT_SAVE, idCtrl, "", "mbr=" + MainActivity.idMbr + postBuilder);
                    task.execute(MainActivity.ACCESS_CODE);

                    try {
                        String result = task.get();

                        //Log.e(TAG, functionName+"result => "+result);

                        if( result != null && result.charAt(0) == '1' ) {
                            isSafe = true;

                            notCtrl = result.substring(1);

                            if( listCapture.size() > 0 ) {
                                int i = 0;
                                ArrayList<String> listName = new ArrayList<>(listCapture);
                                ArrayList<Bitmap> listImg = new ArrayList<>(listBitmap);

                                listCapture.clear();
                                listBitmap.clear();

                                for( String img : listName ) {
                                    new UploadImage(MakeCtrlActivity.this, listImg.get(i), img, UploadImage.UPLOAD_IMAGE_TYPE_CAPTURE);
                                    i++;
                                }
                            }

                            Storages storages = new Storages(MakeCtrlActivity.this);
                            Storage storage = storages.getStorage(fiche.getId());

                            if( storage.getId() > 0 ) {
                                PrefDatabase.getInstance(MakeCtrlActivity.this).mStorageDao().deleteStorageById(storage.getId());
                            }
                            if( !storages.getAllStorages().isEmpty() ) {
                                Intent intent = new Intent(MakeCtrlActivity.this, SynchronizeActivity.class);

                                startActivity(intent);
                            }

                            MakeCtrlActivity.this.runOnUiThread(this::updateZones);
                        } else {
                            secureCtrl();

                            MakeCtrlActivity.this.runOnUiThread(this::updateZones);
                        }

                        showWait(false);
                    } catch( InterruptedException | ExecutionException e ) {
                        e.printStackTrace();

                        secureCtrl();
                        showWait(false);

                        MakeCtrlActivity.this.runOnUiThread(this::updateZones);
                    }
                }
            } else {
                //Toast.makeText(MakeCtrlActivity.this, getResources().getString(R.string.conextion_lost), Toast.LENGTH_LONG).show();
                secureCtrl();

                MakeCtrlActivity.this.runOnUiThread(this::updateZones);
            }

            Looper.loop();
        });
    }

    private String hashComment(String comment) {
        String result = comment;

        result = result.replace(";", "$$#59;");
        result = result.replace("\"", "$$#34;");
        result = result.replace("'", "$$#39;");
        result = result.replace(":", "$$#58;");
        result = result.replace("_", "$$#95;");
        result = result.replace("=", "$$#61;");
        result = result.replace("£", "$$#163;");
        result = result.replace("§", "$$#167;");
        result = result.replace("<", "$$#60;");
        result = result.replace(">", "$$#62;");
        result = result.replace(",", "$$#130;");

        return result;
    }

    private void showWait(Boolean b) {
        if( b ) {
            MakeCtrlActivity.this.runOnUiThread(() -> mWaitImg.setVisibility(View.VISIBLE));
        } else {
            MakeCtrlActivity.this.runOnUiThread(() -> mWaitImg.setVisibility(View.INVISIBLE));
        }
    }

}
