package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivityGetMailBinding;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.utils.AndyUtils;

import java.util.concurrent.CompletableFuture;

public class GetMailActivity extends AppCompatActivity {

	// ********* PRIVATE VARIABLES

	private String typeRequete;

	// ********* STATIC VARIABLES

	public static final String GET_MAIL_ACTIVITY_TYPE = "type";

	public static final class RequestType {
		public static final String ROBOT = HttpTask.HTTP_TASK_CBL_ROBOT;
		public static final String MAIL = HttpTask.HTTP_TASK_CBL_MAIL;
	}

	// ********* WIDGETS

	private ActivityGetMailBinding binding;

	// ********* CONSTRUCTORS

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		binding = ActivityGetMailBinding.inflate(getLayoutInflater());

		setContentView(binding.getRoot());

		EditText mEditText = binding.getMailActivityMailTxt;
		Intent intent = getIntent();

		typeRequete = intent.getStringExtra(GET_MAIL_ACTIVITY_TYPE);

		mEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				if (mEditText.getText().length() > 0) {
					sendRequest();
				}

				return true;
			}

			return false;
		});
	}

	// ********* PUBLIC FUNCTIONS

	public void getMailActivityActions(View v) {
		sendRequest();
	}

	// ********* PRIVATE FUNCTIONS

	private void sendRequest() {
		EditText mEditText = binding.getMailActivityMailTxt;

		if (!AndyUtils.isNetworkAvailable(GetMailActivity.this)) {
			return;
		}

		String mail = "mail=" + mEditText.getText().toString();
		String cbl = (typeRequete.equals(HttpTask.HTTP_TASK_CBL_ROBOT)) ? HttpTask.HTTP_TASK_CBL_ROBOT
				: HttpTask.HTTP_TASK_CBL_MAIL;

		HttpTask task = new HttpTask(GetMailActivity.this);
		CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, cbl, "", mail);

		futureResult.thenAccept(result -> {
			try {
				JSONObject jsonReponse = new JSONObject(result);

				if (jsonReponse.getBoolean("status")) {
					Toast.makeText(GetMailActivity.this, "Un email vous a être envoyé", Toast.LENGTH_SHORT).show();

					setResult(RESULT_OK);
					new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
				} else {
					Toast.makeText(GetMailActivity.this, jsonReponse.getString("message"), Toast.LENGTH_SHORT).show();

					setResult(RESULT_CANCELED);
					new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}).exceptionally(ex -> {
			runOnUiThread(() -> {
				Toast.makeText(GetMailActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT)
						.show();

				setResult(RESULT_CANCELED);
				new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
			});

			return null;
		});
	}

	/**
	 * Valide le format de l'email
	 * 
	 * @param email L'email à valider
	 * @return true si l'email est valide
	 */
	private boolean isValidEmail(String email) {
		String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
		return email != null && email.matches(emailPattern);
	}

	/**
	 * Affiche/Cache l'indicateur de chargement
	 * 
	 * @param show true pour afficher, false pour cacher
	 */
	private void showLoading(boolean show) {
		if (binding.progressBar != null) {
			binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
		}
		if (binding.getMailActivityMailTxt != null) {
			binding.getMailActivityMailTxt.setEnabled(!show);
		}
	}

	/**
	 * Gère la fermeture de l'activité avec un délai
	 */
	private void finishWithDelay() {
		new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		binding = null; // Libération de la mémoire
	}

}
