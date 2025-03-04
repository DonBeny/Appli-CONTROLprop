package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivityStartCtrlBinding;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StartCtrlActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String rsd;
    private String altCtrl;
    private String typeCtrl;
    private String proxi;
    private String contra;
    private int meteo;

    private boolean isStarted;

    private SharedPreferences Preferences;

//********* PUBLIC VARIABLES

    public static boolean ctrlInopine;
    public static boolean meteoPerturbe;
    public static boolean prodPresent;
    public static boolean affConforme;

    private static final String TAG = "StartCtrlActivity";

    public static final int START_CTRL_ACTIVITY_REQUEST_MAKE_CTRL_ACTIVITY = 100;
    public static final int START_CTRL_ACTIVITY_OK = 1;
    public static final int START_CTRL_ACTIVITY_CANCEL = 0;

    public static final String START_CTRL_KEY_CONF_CONTROL = "ctrl";
    public static final String START_CTRL_KEY_CONF_METEO = "meteo";
    public static final String START_CTRL_KEY_CONF_PRODUITS = "prod";
    public static final String START_CTRL_KEY_CONF_AFF_PARTIES_COMMUNES = "aff";

    public static final String START_CTRL_VAL_CONTROL_PROGRAMME = "prog";
    public static final String START_CTRL_VAL_CONTROL_INOPINE = "inopine";
    public static final String START_CTRL_VAL_METEO_NORMALE = "norm";
    public static final String START_CTRL_VAL_METEO_PERTURBEE = "perturb";
    public static final String START_CTRL_VAL_PRODUITS_CONFORME = "ok";
    public static final String START_CTRL_VAL_PRODUITS_NON_CONFORME = "no";
    public static final String START_CTRL_VAL_AFF_PARTIES_COMMUNES_CONFORME = "ok";
    public static final String START_CTRL_VAL_AFF_PARTIES_COMMUNES_NON_CONFORME = "no";

//********* WIDGETS

    private ActivityStartCtrlBinding binding;

//********* CONSTRUCTOR

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding = ActivityStartCtrlBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Intent intent = getIntent();

        rsd = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_RSD);
        proxi = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_PROXY);
        contra = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_CONTRA);
        altCtrl = intent.getStringExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_ALT_CTRL);
        typeCtrl = intent.getStringExtra(TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TYPE_CTRL);

        ctrlInopine = SelectActivity.ficheResid. SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(rsd)).getStrConfCtrl().startsWith("1");
        meteoPerturbe = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(rsd)).getStrConfCtrl().startsWith("1", 2);
        prodPresent = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(rsd)).getStrConfCtrl().startsWith("1", 4);
        affConforme = SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(rsd)).getStrConfCtrl().startsWith("1", 6);

        //Log.e(TAG, "onCreate::config => "+SelectActivity.nameRsds.get(SelectActivity.idRsds.indexOf(rsd)).getStrConfCtrl());
        //Log.e(TAG, "onCreate::ctrl => "+((ctrlInopine) ? "inopine" : "programme"));
        //Log.e(TAG, "onCreate::meteo => "+((meteoPerturbe) ? "perturbee" : "normal"));
        //Log.e(TAG, "onCreate::produits => "+((prodPresent) ? "" : "non")+" conforme");
        //Log.e(TAG, "onCreate::affichage => "+((affConforme) ? "" : "non")+" conforme");

        isStarted = false;

        Preferences = getSharedPreferences(MainActivity.PREF_NAME_APPLI, MODE_PRIVATE);

        mCheckBoxCtrlProg.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_CONTROL, START_CTRL_VAL_CONTROL_PROGRAMME));
        mCheckBoxCtrlInopine.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_CONTROL, START_CTRL_VAL_CONTROL_INOPINE));
        mCheckBoxMeteoNorm.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_METEO, START_CTRL_VAL_METEO_NORMALE));
        mCheckBoxMeteoPerturb.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_METEO, START_CTRL_VAL_METEO_PERTURBEE));
        mCheckBoxProdConform.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_PRODUITS, START_CTRL_VAL_PRODUITS_CONFORME));
        mCheckBoxProdNoConform.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_PRODUITS, START_CTRL_VAL_PRODUITS_NON_CONFORME));
        mCheckBoxAffConform.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_AFF_PARTIES_COMMUNES, START_CTRL_VAL_AFF_PARTIES_COMMUNES_CONFORME));
        mCheckBoxAffNoConform.setOnClickListener(view -> checkClick(START_CTRL_KEY_CONF_AFF_PARTIES_COMMUNES, START_CTRL_VAL_AFF_PARTIES_COMMUNES_NON_CONFORME));

        updateView();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        finish();

        showWait(false);
    }

//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == START_CTRL_ACTIVITY_REQUEST_MAKE_CTRL_ACTIVITY ) {
            if( resultCode == MakeCtrlActivity.MAKE_CTRL_RESULT_OK ) {
                finish();
            }
        }
    }

//********* PUBLIC FUNCTIONS

    public void startCtrlActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "prod": showInfoProd(); break;
            case "aff": showInfoAff(); break;
            case "go": showWait(true); lanceCtrl(); break;
            case "cancel": cancelCtrl(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void showInfoProd() {
        String mess = Preferences.getString(MainActivity.PREF_KEY_INFO_PROD, null);

        if( mess != null && mess.length() > 1 ) {
            Toast.makeText(StartCtrlActivity.this, mess, Toast.LENGTH_LONG).show();
        }
    }
    private void showInfoAff() {
        String mess = Preferences.getString(MainActivity.PREF_KEY_INFO_AFF, null);

        if( mess != null && mess.length() > 1 ) {
            Toast.makeText(StartCtrlActivity.this, mess, Toast.LENGTH_LONG).show();
        }
    }
    private void lanceCtrl() {
        if( !isStarted ) {
            String mess = "";

            isStarted = true;

            mess += (ctrlInopine) ? "1" : "0";
            mess += ";";
            mess += (meteoPerturbe) ? "1" : "0";
            mess += ";";
            mess += (prodPresent) ? "1" : "0";
            mess += ";";
            mess += (affConforme) ? "1" : "0";

            Intent intent = new Intent(StartCtrlActivity.this, MakeCtrlActivity.class);

            intent.putExtra(MakeCtrlActivity.MAKE_CTRL_ID_RSD, rsd);
            intent.putExtra(SelectActivity.SELECT_ACTIVITY_PROXY, proxi);
            intent.putExtra(SelectActivity.SELECT_ACTIVITY_CONTRA, contra);
            intent.putExtra(MakeCtrlActivity.MAKE_CTRL_ALT_CTRL, altCtrl);
            intent.putExtra(MakeCtrlActivity.MAKE_CTRL_TYPE_CTRL, typeCtrl);
            intent.putExtra(MakeCtrlActivity.MAKE_CTRL_CONF_CTRL, mess);
            intent.putExtra(MakeCtrlActivity.MAKE_CTRL_METEO, meteo);

            startActivityForResult(intent, START_CTRL_ACTIVITY_REQUEST_MAKE_CTRL_ACTIVITY);
        }
    }
    private void cancelCtrl() {
        finish();
    }

    private void checkClick(String config, String value) {
        if( config.equals(START_CTRL_KEY_CONF_CONTROL) ) {
            ctrlInopine = value.equals(START_CTRL_VAL_CONTROL_INOPINE);
        }
        if( config.equals(START_CTRL_KEY_CONF_METEO) ) {
            if( value.equals(START_CTRL_VAL_METEO_NORMALE) ) {
                meteoPerturbe = false;
                meteo = 0;
            }
            if( value.equals(START_CTRL_VAL_METEO_PERTURBEE) ) {
                meteoPerturbe = true;
                meteo = 1;
            }
        }
        if( config.equals(START_CTRL_KEY_CONF_PRODUITS) ) {
            prodPresent = value.equals(START_CTRL_VAL_PRODUITS_CONFORME);
        }
        if( config.equals(START_CTRL_KEY_CONF_AFF_PARTIES_COMMUNES) ) {
            affConforme = value.equals(START_CTRL_VAL_AFF_PARTIES_COMMUNES_CONFORME);
        }

        updateView();
    }
    private void updateView() {
        if( ctrlInopine ) {
            mCheckBoxCtrlProg.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxCtrlProg.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxCtrlInopine.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxCtrlInopine.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        } else {
            mCheckBoxCtrlInopine.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxCtrlInopine.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxCtrlProg.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxCtrlProg.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        }
        if( meteoPerturbe ) {
            mCheckBoxMeteoNorm.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxMeteoNorm.setTextColor(this.getResources().getColor(R.color._dark_grey));

            mCheckBoxMeteoPerturb.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxMeteoPerturb.setTextColor(this.getResources().getColor(R.color.main_ctrl_prop));
        } else {
            mCheckBoxMeteoPerturb.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxMeteoPerturb.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxMeteoNorm.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxMeteoNorm.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        }
        if( prodPresent ) {
            mCheckBoxProdNoConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxProdNoConform.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxProdConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxProdConform.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        } else {
            mCheckBoxProdConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxProdConform.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxProdNoConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxProdNoConform.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        }
        if( affConforme ) {
            mCheckBoxAffNoConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxAffNoConform.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxAffConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxAffConform.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        } else {
            mCheckBoxAffConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_desabled));
            mCheckBoxAffConform.setTextColor(ContextCompat.getColor(this, R.color._dark_grey));

            mCheckBoxAffNoConform.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            mCheckBoxAffNoConform.setTextColor(ContextCompat.getColor(this, R.color.main_ctrl_prop));
        }
    }

    private void showWait(Boolean b) {
        if( b ) {
            mWaitImg.setVisibility(View.VISIBLE);
        } else {
            mWaitImg.setVisibility(View.INVISIBLE);
        }
    }

}
