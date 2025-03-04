package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import org.orgaprop.test7.R;
import org.orgaprop.test7.models.CellCriterCtrlModel;
import org.orgaprop.test7.models.CellElmtCtrlModel;
import org.orgaprop.test7.models.NoteModel;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CtrlZoneActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private SharedPreferences preferences;
    private int position;

//********* STATIC VARIABLES

    public static final String TAG = "CtrlZoneActivity";

    public static final String CTRL_ZONE_ACTIVITY_TITLE = "title";
    public static final String CTRL_ZONE_ACTIVITY_ZONE = "zone";

    public static final int CTRL_ZONE_ACTIVITY_RESULT_REQUEST = 100;

    public static final int CTRL_ZONE_ACTIVITY_ABORD = 1;
    public static final int CTRL_ZONE_ACTIVITY_HALL = 2;
    public static final int CTRL_ZONE_ACTIVITY_ASCENSEUR = 3;
    public static final int CTRL_ZONE_ACTIVITY_ESCALIER = 4;
    public static final int CTRL_ZONE_ACTIVITY_PALIER = 5;
    public static final int CTRL_ZONE_ACTIVITY_OM = 6;
    public static final int CTRL_ZONE_ACTIVITY_VELO = 7;
    public static final int CTRL_ZONE_ACTIVITY_CAVE = 8;
    public static final int CTRL_ZONE_ACTIVITY_PARK_INT = 9;
    public static final int CTRL_ZONE_ACTIVITY_INT = 10;
    public static final int CTRL_ZONE_ACTIVITY_PARK_EXT = 11;
    public static final int CTRL_ZONE_ACTIVITY_EXT = 12;
    public static final int CTRL_ZONE_ACTIVITY_OFFICE = 13;
    public static final int CTRL_ZONE_ACTIVITY_REUNION = 14;
    public static final int CTRL_ZONE_ACTIVITY_WASHING = 15;

//********* WIDGETS

    @BindView(R.id.ctrl_zone_activity_title_zone_lbl) TextView mTitleZone;
    @BindView(R.id.ctrl_zone_activity_title_zone_img) ImageView mImageView;
    @BindView(R.id.ctrl_zone_activity_grill_list) LinearLayout mListCtrl;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_ctrl_zone);

        ButterKnife.bind(this);

        preferences = getSharedPreferences(MainActivity.PREF_NAME_APPLI, MODE_PRIVATE);

        Intent intent = getIntent();
        //String text = intent.getStringExtra(CTRL_ZONE_ACTIVITY_TITLE);

        position = intent.getIntExtra(CTRL_ZONE_ACTIVITY_ZONE, 0);

        if( position >= 0 ) {
            int idZone = Integer.parseInt(MakeCtrlActivity.fiche.getZone(position).getId());

            mTitleZone.setText(MakeCtrlActivity.fiche.getZone(position).getText());

            switch( idZone ) {
                case CTRL_ZONE_ACTIVITY_ABORD: mImageView.setImageResource(R.drawable.abords_acces_immeubles_2_blanc); break;
                case CTRL_ZONE_ACTIVITY_HALL: mImageView.setImageResource(R.drawable.hall_blanc); break;
                case CTRL_ZONE_ACTIVITY_ASCENSEUR: mImageView.setImageResource(R.drawable.ascenseur_blanc); break;
                case CTRL_ZONE_ACTIVITY_ESCALIER: mImageView.setImageResource(R.drawable.escalier_blanc); break;
                case CTRL_ZONE_ACTIVITY_PALIER: mImageView.setImageResource(R.drawable.paliers_coursives_blanc); break;
                case CTRL_ZONE_ACTIVITY_OM: mImageView.setImageResource(R.drawable.local_om_blanc); break;
                case CTRL_ZONE_ACTIVITY_VELO: mImageView.setImageResource(R.drawable.local_velo_blanc); break;
                case CTRL_ZONE_ACTIVITY_CAVE: mImageView.setImageResource(R.drawable.cave_blanc); break;
                case CTRL_ZONE_ACTIVITY_PARK_INT: mImageView.setImageResource(R.drawable.parking_sous_sol_blanc); break;
                case CTRL_ZONE_ACTIVITY_INT: mImageView.setImageResource(R.drawable.cour_interieure_blanc); break;
                case CTRL_ZONE_ACTIVITY_PARK_EXT: mImageView.setImageResource(R.drawable.parking_exterieur_blanc); break;
                case CTRL_ZONE_ACTIVITY_EXT: mImageView.setImageResource(R.drawable.espaces_exterieurs_blanc); break;
                case CTRL_ZONE_ACTIVITY_OFFICE: mImageView.setImageResource(R.drawable.icone_bureau_blanc); break;
                case CTRL_ZONE_ACTIVITY_REUNION: mImageView.setImageResource(R.drawable.salle_commune_blanc); break;
                case CTRL_ZONE_ACTIVITY_WASHING: mImageView.setImageResource(R.drawable.buanderie_blanc); break;
                default: mImageView.setImageResource(R.drawable.localisation_blanc); break;
            }

            makeView();
        }
    }

//********* SURCHARGES

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


    }

//********* PUBLIC FUNCTIONS

    public void ctrlZoneActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "finish": finishActivity(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void finishActivity() {
        finish();
    }

    private void makeView() {
        String functionName = "makeView::";

        //Log.e(TAG, functionName+"STRAT");
        //Log.e(TAG, functionName+"Zone => "+MakeCtrlActivity.fiche.getZone(position).getText());

        for( final CellElmtCtrlModel element : MakeCtrlActivity.fiche.getZone(position).getElements() ) {
            View viewElement = LayoutInflater.from(CtrlZoneActivity.this).inflate(R.layout.element_item, null);

            //Log.e(TAG, functionName+"element => "+element.getText());

            TextView titleElement = (TextView) viewElement.findViewById(R.id.element_item_text_txt);
            TextView noteElement = (TextView) viewElement.findViewById(R.id.element_item_note_txt);
            LinearLayout gridCriters = (LinearLayout) viewElement.findViewById(R.id.element_item_grill_lyt);

            NoteModel note = element.note();
            int calc = (note.max > 0) ? (int)((note.note * 100) / note.max) : -1;
            if( calc > 100 ) calc = 100;
            String strNote = (note.max > 0) ? calc+" %" : getResources().getString(R.string.txt_so);

            //Log.e(TAG, functionName+"strNote => "+strNote);

            titleElement.setText(element.getText());
            noteElement.setText(strNote);
            noteElement.setTag(element.getId());

            if( calc >= 0 ) {
                int max = Integer.parseInt(Objects.requireNonNull(preferences.getString(MainActivity.PREF_KEY_LIMIT_TOP, "-1")));
                int min = Integer.parseInt(Objects.requireNonNull(preferences.getString(MainActivity.PREF_KEY_LIMIT_DOWN, "-1")));

                if( max >= 0 && min >= 0 ) {
                    if (calc < min) {
                        noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_red));
                    } else {
                        if (calc > max) {
                            noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_green));
                        } else {
                            noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_orange));
                        }
                    }
                } else {
                    noteElement.setBackgroundColor(ContextCompat.getColor(this, R.color._dark_grey));
                }
            }

            for( final CellCriterCtrlModel criter : element.getCriters() ) {
                View viewCriter = LayoutInflater.from(CtrlZoneActivity.this).inflate(R.layout.criter_item, null);

                //Log.e(TAG, functionName+"criter => "+criter.getText());

                TextView titleCriter = (TextView) viewCriter.findViewById(R.id.criter_item_text_txt);
                final Button buttonCriterOk = (Button) viewCriter.findViewById(R.id.criter_item_ok_btn);
                final Button buttonCriterBad = (Button) viewCriter.findViewById(R.id.criter_item_bad_btn);
                final ImageButton buttonCriterCom = (ImageButton) viewCriter.findViewById(R.id.criter_item_com_btn);

                titleCriter.setText(criter.getText());
                buttonCriterOk.setTag("0");
                buttonCriterBad.setTag("0");
                buttonCriterCom.setEnabled(false);

                if( criter.getValue() > 0 ) {
                    buttonCriterOk.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_selected_green));
                    buttonCriterOk.setTag("1");
                }
                if( criter.getValue() < 0 ) {
                    buttonCriterBad.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_selected_red));
                    buttonCriterBad.setTag("1");
                    buttonCriterCom.setEnabled(true);
                }

                buttonCriterOk.setOnClickListener(view -> {
                    //Log.e(TAG, functionName+"click ok "+position+" / "+element.getPosition()+" / "+criter.getPosition());
                    //Log.e(TAG, functionName+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getText()+" => "+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).getText()+" => "+view.getTag().toString());

                    buttonCriterBad.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_desabled));

                    if( view.getTag().toString().equals("0") ) {
                        view.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_selected_green));
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setValue(1);
                        buttonCriterOk.setTag("1");
                        buttonCriterCom.setEnabled(true);
                    } else {
                        view.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_desabled));
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setValue(0);
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setComment("");
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setCapture("");
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setCaptureUri("");
                        buttonCriterOk.setTag("0");
                        buttonCriterCom.setEnabled(false);
                    }

                    updateView(element);
                });
                buttonCriterBad.setOnClickListener(view -> {
                    //Log.e(TAG, functionName+"click bad "+position+" / "+element.getPosition()+" / "+criter.getPosition());
                    //Log.e(TAG, functionName+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getText()+" => "+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).getText()+" => "+view.getTag().toString());

                    buttonCriterOk.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_desabled));

                    if( view.getTag().toString().equals("0") ) {
                        view.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_selected_red));
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setValue(-1);
                        buttonCriterBad.setTag("1");
                        buttonCriterCom.setEnabled(true);
                    } else {
                        view.setBackground(ContextCompat.getDrawable(CtrlZoneActivity.this, R.drawable.button_desabled));
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setValue(0);
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setComment("");
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setCapture("");
                        MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).setCaptureUri("");
                        buttonCriterBad.setTag("0");
                        buttonCriterCom.setEnabled(false);
                    }

                    updateView(element);
                });
                buttonCriterCom.setOnClickListener(view -> {
                    //Log.e(TAG, functionName+"click comment "+position+" / "+element.getPosition()+" / "+criter.getPosition());
                    //Log.e(TAG, functionName+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getText()+" "+MakeCtrlActivity.fiche.getZone(position).getElement(element.getPosition()).getCriter(criter.getPosition()).getText());

                    Intent intent = new Intent(CtrlZoneActivity.this, AddCommentActivity.class);

                    intent.putExtra(AddCommentActivity.ADD_COMMENT_ZONE, position);
                    intent.putExtra(AddCommentActivity.ADD_COMMENT_ELEMENT, element.getPosition());
                    intent.putExtra(AddCommentActivity.ADD_COMMENT_CRITER, criter.getPosition());

                    startActivityForResult(intent, CTRL_ZONE_ACTIVITY_RESULT_REQUEST);
                });

                gridCriters.addView(viewCriter);

            }

            mListCtrl.addView(viewElement);
        }

        //Log.e(TAG, functionName+"END");
    }
    private void updateView(CellElmtCtrlModel element) {
        String functionName="updateView::";
        TextView noteElement = findViewById(R.id.ctrl_zone_activity_grill_list).findViewWithTag(element.getId());
        NoteModel note = element.note();
        int calc = (note.max > 0) ? (int)((note.note * 100) / note.max) : -1;
        if( calc > 100 ) calc = 100;
        String mess = (calc < 0) ? "SO" : calc + " %";

        //Log.e(TAG, functionName+"element => "+element.getText());
        //Log.e(TAG, functionName+"note => "+mess);

        noteElement.setText(mess);

        if( calc >= 0 ) {
            int max = Integer.parseInt(Objects.requireNonNull(preferences.getString(MainActivity.PREF_KEY_LIMIT_TOP, "-1")));
            int min = Integer.parseInt(Objects.requireNonNull(preferences.getString(MainActivity.PREF_KEY_LIMIT_DOWN, "-1")));

            if( max >= 0 && min >= 0 ) {
                if (calc < min) {
                    noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_red));
                } else {
                    if (calc >= max) {
                        noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_green));
                    } else {
                        noteElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.ctrl_note_orange));
                    }
                }
            } else {
                noteElement.setBackgroundColor(ContextCompat.getColor(CtrlZoneActivity.this, R.color._dark_grey));
            }
        } else {
            noteElement.setBackgroundColor(ContextCompat.getColor(CtrlZoneActivity.this, R.color._dark_grey));
        }
    }

}
