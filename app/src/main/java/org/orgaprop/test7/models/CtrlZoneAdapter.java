package org.orgaprop.test7.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import org.orgaprop.test7.R;
import org.orgaprop.test7.controllers.activities.MakeCtrlActivity;

import java.util.List;

public class CtrlZoneAdapter extends ArrayAdapter<CellElmtCtrlModel> {

//********* PRIVATE VARIABLES

    private Context context;

//********* STATIC VARIABLES

    private static final String TAG = "CtrlZoneAdapter";

//********* CONSTRUCTORS

    public CtrlZoneAdapter(Context context, List<CellElmtCtrlModel> list) {
        super(context, 0, list);

        this.context = context;
    }

//********* SURCHARGES

    @Override
    public int getViewTypeCount() {
        return getCount();
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getCount() {
        return MakeCtrlActivity.fiche.getZones().get(MakeCtrlActivity.position).getElements().size();
    }
    @Override
    public CellElmtCtrlModel getItem(int position) {
        return MakeCtrlActivity.fiche.getZones().get(MakeCtrlActivity.position).getElement(position);
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }

//********* VIEW

    @Override
    public View getView(final int position, View contentView, ViewGroup parent) {

        final ElementHolder holderElement;

        if( contentView == null ) {
            holderElement = new ElementHolder();

            contentView = LayoutInflater.from(context).inflate(R.layout.element_item, parent, false);

            holderElement.mTxtElmt = (TextView) contentView.findViewById(R.id.element_item_text_txt);
            holderElement.mNoteElmt = (TextView) contentView.findViewById(R.id.element_item_note_txt);
            holderElement.mGrillCriters = (LinearLayout) contentView.findViewById(R.id.element_item_grill_lyt);

            contentView.setTag(holderElement);

            CellElmtCtrlModel item = getItem(position);

            assert item != null;

            String m = "S O";
            NoteModel note = item.note();

            if( note.max > 0 ) {
                int calc = (int) ((note.note * 100) / note.max);

                m = String.valueOf(calc);
            }

            holderElement.mTxtElmt.setText(item.getText());
            holderElement.mNoteElmt.setText(m);

            List<CellCriterCtrlModel> listCriters = item.getCriters();
            int posCriter = 0;

            for( final CellCriterCtrlModel criter : listCriters ) {
                posCriter++;

                View criterView = LayoutInflater.from(context).inflate(R.layout.criter_item, parent, false);
                final CriterHolder holderCriter = new CriterHolder();

                holderCriter.mTxtCriter = (TextView) criterView.findViewById(R.id.criter_item_text_txt);
                holderCriter.mBtnOk = (Button) criterView.findViewById(R.id.criter_item_ok_btn);
                holderCriter.mBtnBad = (Button) criterView.findViewById(R.id.criter_item_bad_btn);

                holderCriter.mTxtCriter.setText(criter.getText());
                holderCriter.mTag = MakeCtrlActivity.fiche.getZone(MakeCtrlActivity.position).getId() + "_" + item.getId() + "_" + criter.getId();
                holderCriter.mValue = criter.getValue();
                holderCriter.mComment = criter.getComment();
                holderCriter.mCapture = criter.getCapture();
                holderCriter.position = posCriter;

                if( criter.getValue() > 0 ) {
                    holderCriter.mBtnOk.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_selected_green));
                    holderCriter.mBtnOk.setTextColor(context.getResources().getColor(R.color._white));
                }
                if( criter.getValue() < 0 ) {
                    holderCriter.mBtnBad.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_selected_red));
                    holderCriter.mBtnBad.setTextColor(context.getResources().getColor(R.color._white));
                }

                holderCriter.mBtnOk.setOnClickListener(view -> {
                    if( holderCriter.mValue < 1 ) {
                        if( holderCriter.mValue < 0 ) {
                            holderCriter.mBtnBad.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_desabled));
                            holderCriter.mBtnBad.setTextColor(context.getResources().getColor(R.color._black));
                        }

                        holderCriter.mBtnOk.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_selected_green));
                        holderCriter.mBtnOk.setTextColor(context.getResources().getColor(R.color._white));

                        holderCriter.mValue = 1;
                        MakeCtrlActivity.fiche.getZone(MakeCtrlActivity.position).getElement(position).getCriter(holderCriter.position).setValue(1);
                    }
                });
                holderCriter.mBtnBad.setOnClickListener(view -> {
                    if( holderCriter.mValue >= 0 ) {
                        if( holderCriter.mValue > 0 ) {
                            holderCriter.mBtnOk.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_desabled));
                            holderCriter.mBtnOk.setTextColor(context.getResources().getColor(R.color._black));
                        }

                        holderCriter.mBtnBad.setBackground(AppCompatResources.getDrawable(context, R.drawable.button_selected_red));
                        holderCriter.mBtnBad.setTextColor(context.getResources().getColor(R.color._white));

                        holderCriter.mValue = -1;
                        MakeCtrlActivity.fiche.getZone(MakeCtrlActivity.position).getElement(position).getCriter(holderCriter.position).setValue(-1);
                    }
                });

                holderElement.mGrillCriters.addView(criterView);
            }
        } else {
            holderElement = (ElementHolder) contentView.getTag();
        }

        return contentView;

    }

//********* PRIVATE CLASSES

    private static class ElementHolder {
        TextView mTxtElmt;
        TextView mNoteElmt;
        LinearLayout mGrillCriters;
    }
    private static class CriterHolder {
        TextView mTxtCriter;
        Button mBtnOk;
        Button mBtnBad;
        String mTag;
        int mValue;
        String mComment;
        String mCapture;
        int position;
    }

}
