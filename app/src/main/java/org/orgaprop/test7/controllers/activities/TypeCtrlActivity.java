package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivityTypeCtrlBinding;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.utils.AndyUtils;

import java.util.concurrent.CompletableFuture;

public class TypeCtrlActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String rsd;
    private String altCtrl;
    private String typeCtrl;
    private String proxi;
    private String contra;

    private boolean isStarted;
    private boolean isSafely;

//********* STATIC VARIABLES

    public static final String TAG = "TypeCtrlActivity";

    public static final String TYPE_CTRL_ACTIVITY_ALT_CTRL = "altCtrl";
    public static final String TYPE_CTRL_ACTIVITY_TYPE_CTRL = "typeCtrl";

    public static final String TYPE_CTRL_ACTIVITY_ALT_CTRL_FULL = "full";
    public static final String TYPE_CTRL_ACTIVITY_ALT_CTRL_RANDOM = "rnd";

    public static final String TYPE_CTRL_ACTIVITY_TAG_ENTREE = "e";
    public static final String TYPE_CTRL_ACTIVITY_TAG_CTRL = "c";
    public static final String TYPE_CTRL_ACTIVITY_TAG_RANDOM = "cr";
    public static final String TYPE_CTRL_ACTIVITY_TAG_LEVEE = "p";
    public static final String TYPE_CTRL_ACTIVITY_TAG_SORTI = "s";

//********* WIDGETS

    private ActivityTypeCtrlBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding = ActivityTypeCtrlBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Intent intent = getIntent();

        rsd = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_RSD);
        proxi = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_PROXY);
        contra = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_CONTRA);

        typeCtrl = "";
        altCtrl = TYPE_CTRL_ACTIVITY_ALT_CTRL_FULL;

        isSafely = AndyUtils.isNetworkAvailable(TypeCtrlActivity.this);
        isStarted = false;
    }

//******** PUBLIC FUNCTIONS

    public void makeCtrl(View view) {
        if( !isStarted ) {
            isStarted = true;
            typeCtrl = view.getTag().toString();

            if( isSafely || ( !typeCtrl.equals(TYPE_CTRL_ACTIVITY_TAG_RANDOM) && !typeCtrl.equals(TYPE_CTRL_ACTIVITY_TAG_LEVEE) ) ) {
                if( typeCtrl.equals(TYPE_CTRL_ACTIVITY_TAG_LEVEE) ) {
                    String stringGet = "mod=" + HttpTask.HTTP_TASK_MOD_GET + "&rsd=" + rsd;
                    String stringPost = "mbr=" + MainActivity.idMbr + "&mac=" + MainActivity.adrMac;

                    HttpTask task = new HttpTask(TypeCtrlActivity.this);
                    CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_PROP, HttpTask.HTTP_TASK_CBL_PLAN_ACTIONS, stringGet, stringPost);

                    futureResult.thenAccept(result -> {
                       try {
                           JSONObject jsonResult = new JSONObject(result);

                           if( jsonResult.getBoolean("status") ) {
                               Intent intent = new Intent(TypeCtrlActivity.this, AddPlanActionActivity.class);

                               intent.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, rsd);
                               intent.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
                               intent.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
                               intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_ID, jsonResult.getJSONObject("data").getString("id"));
                               intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_DATE, jsonResult.getJSONObject("data").getString("limit"));
                               intent.putExtra(AddPlanActionActivity.ADD_PLAN_ACTION_TEXT, jsonResult.getJSONObject("data").getString("txt"));

                               startActivity(intent);
                           } else {
                               runOnUiThread(() -> {
                                   try {
                                       Toast.makeText(TypeCtrlActivity.this, jsonResult.getString("message"), Toast.LENGTH_SHORT).show();
                                   } catch (JSONException e) {
                                       runOnUiThread(() -> Toast.makeText(TypeCtrlActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
                                   }
                               });
                           }
                       } catch (JSONException e) {
                           runOnUiThread(() -> Toast.makeText(TypeCtrlActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
                       }
                    }).exceptionally(ex -> {
                        runOnUiThread(() -> Toast.makeText(TypeCtrlActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show());

                        return null;
                    });
                } else {
                    Intent intent = new Intent(TypeCtrlActivity.this, StartCtrlActivity.class);

                    if (typeCtrl.equals("cr")) {
                        typeCtrl = "c";
                        altCtrl = TYPE_CTRL_ACTIVITY_ALT_CTRL_RANDOM;
                    }

                    intent.putExtra(SelectActivity.SELECT_ACTIVITY_RSD, rsd);
                    intent.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
                    intent.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
                    intent.putExtra(TYPE_CTRL_ACTIVITY_ALT_CTRL, altCtrl);
                    intent.putExtra(TYPE_CTRL_ACTIVITY_TYPE_CTRL, typeCtrl);

                    startActivity(intent);
                }
            } else {
                Toast.makeText(TypeCtrlActivity.this, "Récupérez une connexion pour effectuer ce type de contrôle !", Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }
}
