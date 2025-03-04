package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivitySelectListBinding;
import org.orgaprop.test7.models.ObjProp;
import org.orgaprop.test7.models.SelectItem;
import org.orgaprop.test7.models.SelectListAdapter;
import org.orgaprop.test7.models.prop.ObjCriter;
import org.orgaprop.test7.models.prop.ObjElement;
import org.orgaprop.test7.models.prop.ObjGrille;
import org.orgaprop.test7.models.prop.ObjZone;
import org.orgaprop.test7.services.HttpTask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SelectListActivity extends AppCompatActivity {

    public static final String SELECT_LIST_TYPE = "SELECT_LIST_TYPE";
    public static final String SELECT_LIST_ID = "SELECT_LIST_ID";
    public static final String SELECT_LIST_TXT = "SELECT_LIST_TXT";
    public static final String SELECT_LIST_LIST = "SELECT_LIST_LIST";
    public static final String SELECT_LIST_COMMENT = "SELECT_LIST_COMMENT";

    public static final String SELECT_LIST_TYPE_AGC = "agc";
    public static final String SELECT_LIST_TYPE_GRP = "grp";
    public static final String SELECT_LIST_TYPE_RSD = "rsd";
    public static final String SELECT_LIST_TYPE_SEARCH = "search";

    private ActivitySelectListBinding binding;
    private SelectListAdapter adapter;
    private List<SelectItem> items = new ArrayList<>();

    private String type;
    private int parentId = 0;
    private String mess = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type = getIntent().getStringExtra(SELECT_LIST_TYPE);
        parentId = getIntent().getIntExtra(SELECT_LIST_ID, 0);
        mess = getIntent().getStringExtra(SELECT_LIST_TXT);

        setupRecyclerView();

        if( type.equals(SELECT_LIST_TYPE_AGC) ) {
            recupAgences();
        } else {
            fetchData();
        }
    }

    private void setupRecyclerView() {
        adapter = new SelectListAdapter(items, type, this::onItemSelected);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void recupAgences() {
        try {
            items.clear();

            for( Iterator<String> it = MainActivity.mapAgences.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject jsonItem = MainActivity.mapAgences.getJSONObject(key);

                items.add(new SelectItem(jsonItem.getInt("id"), jsonItem.getString("txt")));
            }

            runOnUiThread(() -> adapter.notifyDataSetChanged());
        } catch (JSONException e) {
            Toast.makeText(SelectListActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchData() {
        String postString = "mbr=" + MainActivity.idMbr + "&search=" + mess;

        HttpTask task = new HttpTask(SelectListActivity.this);
        CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_LIST, type, "val="+parentId, postString);

        futureResult.thenAccept(result -> {
            try {
                JSONObject jsonResponse = new JSONObject(result);

                if( jsonResponse.getBoolean("status") ) {
                    JSONObject dataObject = jsonResponse.getJSONObject("data");

                    items.clear();

                    for( Iterator<String> it = dataObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        JSONObject jsonItem = dataObject.getJSONObject(key);

                        if( type.equals(SELECT_LIST_TYPE_GRP) ) {
                            items.add(new SelectItem(jsonItem.getInt("id"), jsonItem.getString("txt")));
                        } else {
                            SelectItem item = new SelectItem(
                                    jsonItem.getInt("id"),
                                    jsonItem.getInt("agency"),
                                    jsonItem.getInt("group"),
                                    jsonItem.getString("ref"),
                                    jsonItem.getString("name"),
                                    jsonItem.getString("entry"),
                                    jsonItem.getJSONObject("adr").getString("rue"),
                                    jsonItem.getJSONObject("adr").getString("cp"),
                                    jsonItem.getJSONObject("adr").getString("city"),
                                    jsonItem.getString("last"),
                                    jsonItem.getBoolean("delay"),
                                    jsonItem.getString("comment")
                                    );

                            item.getObjProp().getObjZones().setProxi(jsonItem.getJSONObject("prop").getJSONObject("zones").getJSONArray("proxi"));
                            item.getObjProp().getObjZones().setContrat(jsonItem.getJSONObject("prop").getJSONObject("zones").getJSONArray("contrat"));
                            item.getObjProp().getObjConfig().setVisite(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("conf").getBoolean("visite"));
                            item.getObjProp().getObjConfig().setMeteo(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("conf").getBoolean("meteo"));
                            item.getObjProp().getObjConfig().setAffichage(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("conf").getBoolean("affichage"));
                            item.getObjProp().getObjConfig().setProduits(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("conf").getBoolean("produits"));
                            item.getObjProp().getObjDateCtrl().setValue(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("data").getInt("val"));
                            item.getObjProp().getObjDateCtrl().setTxt(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("data").getString("txt"));
                            item.getObjProp().setNote(jsonItem.getJSONObject("prop").getJSONObject("ctrl").getInt("note"));

                            JSONObject jsonGrille = jsonItem.getJSONObject("prop").getJSONObject("ctrl").getJSONObject("grille");
                            ObjGrille grille = item.getObjProp().getGrille();

                            for( Iterator<String> itZone = jsonGrille.keys(); itZone.hasNext(); ) {
                                String keyZone = itZone.next();
                                JSONObject zone = jsonGrille.getJSONObject(keyZone);
                                ObjZone objZone = new ObjZone();

                                objZone.setId(Integer.parseInt(keyZone));
                                objZone.setNote(-1);

                                for( Iterator<String> itElement = zone.keys(); itElement.hasNext(); ) {
                                    String keyElement = itElement.next();
                                    JSONObject element = zone.getJSONObject(keyElement);
                                    ObjElement objElement = new ObjElement();

                                    objElement.setId(Integer.parseInt(keyElement));
                                    objElement.setNote(-1);

                                    for( Iterator<String> itCriter = element.keys(); itCriter.hasNext(); ) {
                                        String keyCrtiter = itCriter.next();
                                        JSONObject criter = element.getJSONObject(keyCrtiter);
                                        ObjCriter objCriter = new ObjCriter();

                                        objCriter.setId(Integer.parseInt(keyCrtiter));
                                        objCriter.setNote(criter.getInt("note"));

                                        if( criter.has("com") ) {
                                            objCriter.getComment().setTxt(criter.getJSONObject("com").getString("txt"));
                                            objCriter.getComment().setImg(criter.getJSONObject("com").getString("img"));
                                        }

                                        objElement.addCriter(objCriter);
                                    }

                                    objZone.addElement(objElement);
                                }

                                grille.addZone(objZone);
                            }

                            item.getObjProp().setGrille(grille);

                            if( type.equals(SELECT_LIST_TYPE_SEARCH) ) {
                                JSONObject jsonComment = new JSONObject(Objects.requireNonNull(jsonItem.getString("comment")));

                                item.setNameAgency(jsonComment.getJSONObject("agency").getString("txt"));
                                item.setNameGroup(jsonComment.getJSONObject("groupe").getString("txt"));
                                item.setComment("");
                            }

                            items.add(item);
                        }
                    }

                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } else {
                    runOnUiThread(() -> {
                        try {
                            Toast.makeText(SelectListActivity.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(SelectListActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(ex -> {
            runOnUiThread(() -> Toast.makeText(SelectListActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show());

            return null;
        });
    }

    private void onItemSelected(SelectItem item) {
        Intent resultIntent = new Intent();

        resultIntent.putExtra(SELECT_LIST_TYPE, type);
        resultIntent.putExtra(SELECT_LIST_ID, item.getId());
        resultIntent.putExtra(SELECT_LIST_TXT, item.getName());

        if( type.equals(SELECT_LIST_TYPE_SEARCH) ) {
            resultIntent.putExtra(SELECT_LIST_COMMENT, item.getComment());
        } else {
            resultIntent.putExtra(SELECT_LIST_LIST, (Serializable) items);

            if( type.equals(SELECT_LIST_TYPE_RSD) ) {
                resultIntent.putExtra(SELECT_LIST_COMMENT, item);
            }
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }

}