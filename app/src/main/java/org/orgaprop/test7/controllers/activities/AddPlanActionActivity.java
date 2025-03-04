package org.orgaprop.test7.controllers.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivityAddPlanActionBinding;
import org.orgaprop.test7.services.HttpTask;

import java.util.Calendar;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddPlanActionActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String idRsd;
    private String idPlanAction;

//********* STATIC VARIABLES

    private static final String TAG = "AddPlanActionActivity";

    public static final String ADD_PLAN_ACTION_ID = "id";
    public static final String ADD_PLAN_ACTION_DATE = "date";
    public static final String ADD_PLAN_ACTION_TEXT = "text";
    public static final String ADD_PLAN_ACTION_TYPE = "type";

//********* WIDGETS

    private DatePickerDialog mDatePickerDialog;
    private ActivityAddPlanActionBinding binding;

//********* CONSTRUCTOR

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        binding = ActivityAddPlanActionBinding.inflate(getLayoutInflater());
        
        setContentView(binding.getRoot());

        Intent intent = getIntent();

        idRsd = intent.getStringExtra(SelectActivity.SELECT_ACTIVITY_RSD);
        idPlanAction = intent.getStringExtra(ADD_PLAN_ACTION_ID);

        String txtDate = intent.getStringExtra(ADD_PLAN_ACTION_DATE);

        if( txtDate != null ) {
            final int year = Integer.parseInt(txtDate.substring(6, 10));
            final int month = Integer.parseInt(txtDate.substring(3, 5)) - 1;
            final int day = Integer.parseInt(txtDate.substring(0, 2));

            binding.addPlanActionDateTxt.setText(txtDate);
            binding.addPlanActionPlanTxt.setText(HtmlCompat.fromHtml(Objects.requireNonNull(intent.getStringExtra(ADD_PLAN_ACTION_TEXT)), HtmlCompat.FROM_HTML_MODE_LEGACY));

            binding.addPlanActionDateTxt.setOnClickListener(view -> {
                mDatePickerDialog = new DatePickerDialog(AddPlanActionActivity.this, (view1, year1, monthOfYear, dayOfMonth) -> {
                    String txtDay = (dayOfMonth < 10) ? "0" + dayOfMonth : String.valueOf(dayOfMonth);
                    String txtMonth = (monthOfYear < 9) ? "0" + (monthOfYear + 1) : String.valueOf(monthOfYear + 1);
                    String mess = txtDay + "/" + txtMonth + "/" + year1;
                    binding.addPlanActionDateTxt.setText(mess);
                }, year, month, day);

                mDatePickerDialog.show();
            });
        }
    }

//********* SURCHARGES
    
    

//********* PUBLIC FUNCTIONS

    public void addPlanActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "alert": addAlert(); break;
            case "valid": validPlanAct(); break;
            case "save": savePlanAct(); break;
            case "cancel": finishActivity(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void savePlanAct() {
        String date = binding.addPlanActionDateTxt.getText().toString();
        String txt = binding.addPlanActionPlanTxt.getText().toString();
        StringTokenizer tokenizer = new StringTokenizer(date, "/");
        Calendar thisDay = Calendar.getInstance();
        Calendar echeance = Calendar.getInstance();
        int day = Integer.parseInt(tokenizer.nextToken());
        int month = Integer.parseInt(tokenizer.nextToken());
        int year = Integer.parseInt(tokenizer.nextToken());

        echeance.set(year, month-1, day, 0, 0);

        thisDay.set(thisDay.get(Calendar.YEAR), thisDay.get(Calendar.MONTH), thisDay.get(Calendar.DATE), 0, 0);

        if( echeance.compareTo(thisDay) > 0 ) {
            try {
                CompletableFuture<String> futureResult = getStringCompletableFuture(date, txt);

                futureResult.thenAccept(result -> {
                    try {
                        JSONObject jsonResult = new JSONObject(result);

                        if( jsonResult.getBoolean("status") ) {
                            finish();
                        } else {
                            runOnUiThread(() -> {
                                try {
                                    Toast.makeText(AddPlanActionActivity.this, jsonResult.getString("message"), Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    runOnUiThread(() -> Toast.makeText(AddPlanActionActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(AddPlanActionActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
                    }
                }).exceptionally(ex -> {
                    runOnUiThread(() -> Toast.makeText(AddPlanActionActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show());
                    
                    return null; 
                });
            } catch (JSONException e) {
                runOnUiThread(() -> Toast.makeText(AddPlanActionActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private CompletableFuture<String> getStringCompletableFuture(String date, String txt, boolean levee) throws JSONException {
        JSONObject objPlan = new JSONObject();

        objPlan.put("id", idPlanAction);
        objPlan.put("echeance", date);
        objPlan.put("txt", txt);

        String strGet = "mod=" + HttpTask.HTTP_TASK_MOD_SET + "&rsd=" + idRsd + "&ctrl=" + SelectActivity.ficheResid.getObjProp().getObjDateCtrl().getValue();
        String strPost = "mbr=" + MainActivity.idMbr + "&mac=" + MainActivity.adrMac + "&data=" + objPlan;

        HttpTask task = new HttpTask(AddPlanActionActivity.this);

        return task.executeHttpTask(HttpTask.HTTP_TASK_ACT_PROP, HttpTask.HTTP_TASK_CBL_PLAN_ACTIONS, strGet, strPost);
    }

    private void addAlert() {
        if( (ActivityCompat.checkSelfPermission(AddPlanActionActivity.this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(AddPlanActionActivity.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) ) {
                    String date = mDateTxt.getText().toString();
                    String txt = mText.getText().toString();

                    Intent intent = new Intent();

                    intent.putExtra(FinishCtrlActivity.FINISH_ACTIVITY_RESULT, FinishCtrlActivity.FINISH_ACTIVITY_RESULT_REQUEST_PLAN_CAL);
                    intent.putExtra(ADD_PLAN_ACTION_DATE, date);
                    intent.putExtra(ADD_PLAN_ACTION_TEXT, txt);

                    //Log.e(TAG, functionName+"::intent");

                    setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(AddPlanActionActivity.this, getString(R.string.mess_bad_permission_calendar), Toast.LENGTH_SHORT).show();
                }
    }
    private void validPlanAct() {
        if( typCtrl.equals("") ) {
            if (!MakeCtrlActivity.fiche.getPlanAction("id").equals("new")) {
                Intent intent = new Intent();

                intent.putExtra(FinishCtrlActivity.FINISH_ACTIVITY_RESULT, FinishCtrlActivity.FINISH_ACTIVITY_RESULT_REQUEST_PLAN_VALID);

                setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_OK, intent);
                finish();
            }
        } else {
            HttpTask task = new HttpTask(AddPlanActionActivity.this, HttpTask.HTTP_TASK_ACT_VALID_PLAN, idRsd, "plan="+idPlanAction+"&typ=p", "mbr=" + MainActivity.idMbr);
            task.execute(MainActivity.ACCESS_CODE);

            try {
                String result = task.get();

                if( result != null && result.equals("1") ) {
                    finish();
                } else if( result != null ) {
                    Toast.makeText(AddPlanActionActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AddPlanActionActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    private void finishActivity() {
        setResult(FinishCtrlActivity.FINISH_ACTIVITY_RESULT_CANCEL);
        finish();
    }

}
