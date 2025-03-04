package org.orgaprop.test7.controllers.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.databinding.ActivitySynchronizeBinding;
import org.orgaprop.test7.models.Storage;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.services.Storages;

import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SynchronizeActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private Intent intent;

//********* PUBLIC VARIABLES



//********* WIDGETS

    private ActivitySynchronizeBinding binding;

//********* SURCHARGES

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySynchronizeBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        intent = getIntent();

        showWait(false);
    }

//********* PUBLIC FUNCTIONS

    public void synchronizeActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "start": showWait(true); makeSave(); break;
            case "end": cancel(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void makeSave() {
        TextView mTextView = binding.synchronizeActivityTxt;

        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            Cursor cursor = PrefDatabase.getInstance(SynchronizeActivity.this).mStorageDao().getAllStorageWithCursor();
            JSONObject postBuilder = new JSONObject();

            if( cursor != null && cursor.moveToFirst() ) {
                try {
                    JSONObject item = makeJsonFromCursor(cursor);

                    postBuilder.put(item.getString(Storage.STORAGE_PARAM_RESID), item);

                    if( !cursor.isLast() ) {
                        while (cursor.moveToNext()) {
                            item = makeJsonFromCursor(cursor);

                            postBuilder.put(item.getString(Storage.STORAGE_PARAM_RESID), item);
                        }
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(SynchronizeActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show());
                } finally {
                    String stringPost = "mbr=" + MainActivity.idMbr + "&data=" + postBuilder;

                    HttpTask task = new HttpTask(SynchronizeActivity.this);
                    CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_SYNCHRO, "prop", "", stringPost);

                    futureResult.thenAccept(result -> {
                        if( ( result != null ) && result.equals("1") ) {
                            PrefDatabase.getInstance(SynchronizeActivity.this).mStorageDao().deleteAllStorage();

                            runOnUiThread(() -> {
                                showWait(false);
                                setResult(RESULT_OK, intent);
                                finish();
                            });
                        } else if( result != null ) {
                            StringTokenizer tokenizer = new StringTokenizer(result, "£");
                            StringBuilder r = new StringBuilder();

                            while( tokenizer.hasMoreTokens() ) {
                                if(!r.toString().isEmpty()) r.append("\n");
                                r.append(tokenizer.nextToken());
                            }

                            runOnUiThread(() -> {
                                mTextView.setText(r);

                                showWait(false);
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show());

                        }
                    }).exceptionally(ex -> {
                        runOnUiThread(() -> Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show());

                        return null;
                    });
                }
            }

            Looper.loop();
        });
    }
    private void cancel() {
        if( MainActivity.debugg ) {
            Executors.newSingleThreadExecutor().execute(() -> {
                PrefDatabase.getInstance(SynchronizeActivity.this).mStorageDao().deleteAllStorage();
            });
        }

        runOnUiThread(() -> {
            setResult(RESULT_CANCELED, intent);
            finish();
        });

        finish();
    }

    private void showWait(boolean b) {
        Button mButtonCancel = binding.synchronizeActivityNoBtn;
        Button mButtonStart = binding.synchronizeActivityStartBtn;
        TextView mTextView = binding.synchronizeActivityTxt;
        pl.droidsonroids.gif.GifImageView mWaitImg = binding.synchronizeActivityWait;

        SynchronizeActivity.this.runOnUiThread(() -> {
            if( b ) {
                mTextView.setVisibility(View.INVISIBLE);
                mButtonStart.setVisibility(View.INVISIBLE);
                mButtonCancel.setVisibility(View.INVISIBLE);
                mWaitImg.setVisibility(View.VISIBLE);
            } else {
                mWaitImg.setVisibility(View.INVISIBLE);
                mTextView.setVisibility(View.VISIBLE);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonCancel.setVisibility(View.VISIBLE);
            }
        });
    }

    private JSONObject makeJsonFromCursor(Cursor cursor) {
        JSONObject result = new JSONObject();
        JSONObject ctrl = new JSONObject();
        JSONObject plan = new JSONObject();
        JSONObject send = new JSONObject();

        try {
            result.put(Storage.STORAGE_PARAM_ID, cursor.getInt(PrefDatabase.STORAGE_COL_ID_NUM));
            result.put(Storage.STORAGE_PARAM_RESID, cursor.getInt(PrefDatabase.STORAGE_COL_RESID_NUM));
            result.put(Storage.STORAGE_PARAM_DATE, cursor.getInt(PrefDatabase.STORAGE_COL_DATE_NUM));
            result.put(Storage.STORAGE_PARAM_TYPE, cursor.getString(PrefDatabase.STORAGE_COL_TYPE_CTRL_NUM));
            result.put(Storage.STORAGE_PARAM_CONFIG, cursor.getString(PrefDatabase.STORAGE_COL_CONFIG_NUM));

            ctrl.put(Storage.STORAGE_PARAM_CTRL_TYPE, cursor.getString(PrefDatabase.STORAGE_COL_CTRL_TYPE_NUM));
            ctrl.put(Storage.STORAGE_PARAM_CTRL_GRILLE, cursor.getString(PrefDatabase.STORAGE_COL_CTRL_CTRL_NUM));
            ctrl.put(Storage.STORAGE_PARAM_CTRL_SIG1, cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG1_NUM));
            ctrl.put(Storage.STORAGE_PARAM_CTRL_SIG2, cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG2_NUM));
            ctrl.put(Storage.STORAGE_PARAM_CTRL_AGT, cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG_NUM));
            result.put(Storage.STORAGE_PARAM_CTRL, ctrl);

            plan.put(Storage.STORAGE_PARAM_PLAN_ECHEANCE, cursor.getInt(PrefDatabase.STORAGE_COL_PLAN_END_NUM));
            plan.put(Storage.STORAGE_PARAM_PLAN_CONTENT, cursor.getString(PrefDatabase.STORAGE_COL_PLAN_CONTENT_NUM));
            plan.put(Storage.STORAGE_PARAM_PLAN_VALIDATE, cursor.getInt(PrefDatabase.STORAGE_COL_PLAN_VALIDATE_NUM));
            result.put(Storage.STORAGE_PARAM_PLAN_ACTIONS, plan);

            send.put(Storage.STORAGE_PARAM_SEND_DEST, cursor.getString(PrefDatabase.STORAGE_COL_SEND_DEST_NUM));
            send.put(Storage.STORAGE_PARAM_SEND_PLAN_ID, cursor.getInt(PrefDatabase.STORAGE_COL_SEND_ID_PLAN_NUM));
            send.put(Storage.STORAGE_PARAM_SEND_CTRL_DATE, cursor.getInt(PrefDatabase.STORAGE_COL_SEND_DATE_CTRL_NUM));
            send.put(Storage.STORAGE_PARAM_SEND_CTRL_TYPE, cursor.getString(PrefDatabase.STORAGE_COL_SEND_TYPE_CTRL_NUM));
            send.put(Storage.STORAGE_PARAM_SEND_SRC, cursor.getString(PrefDatabase.STORAGE_COL_SEND_SRC_NUM));
            result.put(Storage.STORAGE_PARAM_SEND, send);
        } catch (JSONException e) {
            runOnUiThread(() -> Toast.makeText(SynchronizeActivity.this, "Erreur de traitement du JSON", Toast.LENGTH_SHORT).show());
        }

        return result;
    }

}