package org.orgaprop.test7.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Base64;
import android.widget.Toast;

import org.orgaprop.test7.controllers.activities.FinishCtrlActivity;
import org.orgaprop.test7.controllers.activities.MakeCtrlActivity;
import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UploadImage {

//******** PRIVATE VARIABLES

    private String imageNameFieldOnServer = "image_name";
    private String imagePathFieldOnServer = "image_path";
    private String imageUploadPathOnServer = "https://www.orgaprop.org/cs/app/imgToServer/capture_img_upload_to_server.php";

    private ParseContent parseContent;

    private Bitmap bitmap;
    private String imageName;
    private Activity activity;

    private boolean check = true;

//********* PUBLIC VARIABLES

    public final String convertImage;
    public final String typeUpload;

    public static final String TAG = "UploadImage";

    public static final String UPLOAD_IMAGE_TYPE_SIGNATURE_CTRL = "sig1";
    public static final String UPLOAD_IMAGE_TYPE_SIGNATURE_AGT = "sig2";
    public static final String UPLOAD_IMAGE_TYPE_CAPTURE = "capture";
    public static final String UPLOAD_IMAGE_TYPE_SEND = "send";

//********* CONSTRUCTORS

    public UploadImage(Activity activity, Bitmap bitmap, String imageName, String typeUpload) {
        this.activity = activity;
        this.bitmap = bitmap;
        this.imageName = imageName;
        this.typeUpload = typeUpload;

        parseContent = new ParseContent(this.activity);

        ByteArrayOutputStream byteArrayOutputStreamObj = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStreamObj);
        byte[] byteArrayVar = byteArrayOutputStreamObj.toByteArray();

        convertImage = Base64.encodeToString(byteArrayVar, Base64.DEFAULT);

        executeImageUpload().thenAccept(result -> {
            if (!parseContent.isSuccess(result)) {
                String errorMsg = parseContent.getErrorCode(result);
                handleFailure(errorMsg);
            } else {
                handleSuccess(parseContent.getMessage(result));
            }
        }).exceptionally(e -> {
            e.printStackTrace();

            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

            return null;
        });
    }


    //********* PUBLIC CLASSES

    public class ImageProcessClass {

        public String ImageHttpRequest(String requestURL, HashMap<String, String> PData) {
            StringBuilder stringBuilder = new StringBuilder();

            try {
                URL url = new URL(requestURL);
                HttpURLConnection httpURLConnectionObj = (HttpURLConnection) url.openConnection();

                httpURLConnectionObj.setReadTimeout(19000);
                httpURLConnectionObj.setConnectTimeout(19000);
                httpURLConnectionObj.setRequestMethod("POST");
                httpURLConnectionObj.setDoInput(true);
                httpURLConnectionObj.setDoOutput(true);

                try (OutputStream outputStream = httpURLConnectionObj.getOutputStream();
                     BufferedWriter bufferedWriterObj = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                    bufferedWriterObj.write(bufferedWriterDataFunction(PData));
                    bufferedWriterObj.flush();
                }

                int responseCode = httpURLConnectionObj.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader bufferedReaderObj = new BufferedReader(new InputStreamReader(httpURLConnectionObj.getInputStream()))) {
                        String line;
                        while ((line = bufferedReaderObj.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                    }
                }
            } catch( IOException e ) {
                e.printStackTrace();
            }

            return stringBuilder.toString();
        }

    }

//********* PRIVATE FUNCTIONS

    private CompletableFuture<String> executeImageUpload() {
        return CompletableFuture.supplyAsync(() -> {
            ImageProcessClass imageProcessClass = new ImageProcessClass();
            HashMap<String, String> hashMapParams = new HashMap<>();

            hashMapParams.put(imageNameFieldOnServer, imageName);
            hashMapParams.put(imagePathFieldOnServer, convertImage);

            return imageProcessClass.ImageHttpRequest(imageUploadPathOnServer, hashMapParams);
        });
    }

    private void handleFailure(String errorMsg) {
        Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show();

        if( typeUpload.equals(UPLOAD_IMAGE_TYPE_CAPTURE) ) {
            MakeCtrlActivity.listCapture.add(imageName);
            MakeCtrlActivity.listBitmap.add(bitmap);

            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Echec de l'envoi de la prise de vue !", Toast.LENGTH_SHORT).show();
            });
        } else {
            FinishCtrlActivity.resultUpload = false;
            FinishCtrlActivity.waitUpload = false;
            Storage storage = PrefDatabase.getInstance(activity)
                    .mStorageDao()
                    .getStorageRsd(Integer.parseInt(MakeCtrlActivity.fiche.getId()))
                    .getValue();

            if (storage != null) {
                storage.setCtrl_sig(AndyUtils.bitmapToString(bitmap));
            }

            activity.runOnUiThread(() -> {
                if( typeUpload.equals(UPLOAD_IMAGE_TYPE_SIGNATURE_CTRL) ) {
                    Toast.makeText(activity, "Echec de l'enregistrement de la signature du contrôleur !", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Echec de l'enregistrement de la signature du contradicteur !", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleSuccess(String message) {
        if( typeUpload.equals(UPLOAD_IMAGE_TYPE_SIGNATURE_CTRL) || typeUpload.equals(UPLOAD_IMAGE_TYPE_SIGNATURE_AGT) ) {
            FinishCtrlActivity.resultUpload = true;
            FinishCtrlActivity.waitUpload = false;

            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Signature enregistrée.", Toast.LENGTH_SHORT).show();
            });
        } else {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Prise de vue enregistrée.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String bufferedWriterDataFunction(HashMap<String, String> hashMapParams) throws UnsupportedEncodingException {
        StringBuilder stringBuilderObj = new StringBuilder();
        boolean check = true;

        for (Map.Entry<String, String> entry : hashMapParams.entrySet()) {
            if (!check) {
                stringBuilderObj.append("&");
            } else {
                check = false;
            }

            stringBuilderObj.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            stringBuilderObj.append("=");
            stringBuilderObj.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return stringBuilderObj.toString();
    }

}
