package org.orgaprop.test7.utils;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

public class ParseContent {

//********* PUBLIC VARIABLES



//********* PRIVATE VARIABLES

    private Activity activity;

    private final String KEY_SUCCESS = "status";
    private final String KEY_MSG = "message";
    private final String KEY_DATA = "data";

//********* CONSTRUCTORS

    public ParseContent(Activity activity) {
        this.activity = activity;
    }

//********* PUBLIC FUNCTIONS

    public boolean isSuccess(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);

            return jsonObject.optString(KEY_SUCCESS).equals("true");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }
    public String getErrorCode(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);

            return jsonObject.getString(KEY_MSG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "No data";
    }
    public String getMessage(String reponse) {
        String msg = "";

        try{
            JSONObject jsonObject = new JSONObject(reponse);

            jsonObject.toString().replace("\\\\","");

            if( jsonObject.getString(KEY_SUCCESS).equals("true") ) {
                msg = jsonObject.getString(KEY_MSG);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return msg;
    }
    public String getURL(String response) {
        String url = "";

        try {
            JSONObject jsonObject = new JSONObject(response);

            jsonObject.toString().replace("\\\\","");

            if( jsonObject.getString(KEY_SUCCESS).equals("true") ) {
                url = jsonObject.getString(KEY_DATA);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return url;
    }

//********* PRIVATE FUNCTIONS

}
