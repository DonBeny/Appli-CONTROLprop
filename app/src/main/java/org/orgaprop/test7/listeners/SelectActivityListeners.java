package org.orgaprop.test7.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import org.orgaprop.test7.R;
import org.orgaprop.test7.databinding.ActivitySelectBinding;
import org.orgaprop.test7.managers.Managers;

/**
 * Gestionnaire des listeners de SelectActivity
 */
public class SelectActivityListeners {

	private final ActivitySelectBinding binding;
	private final Managers managers;

	public SelectActivityListeners(ActivitySelectBinding binding, Managers managers) {
		this.binding = binding;
		this.managers = managers;
	}

	public void setup() {
		setupSearchInputListeners();
		setupCheckBoxListeners();
	}

	private void setupSearchInputListeners() {
		binding.selectActivitySearchInput.addTextChangedListener(createSearchTextWatcher());
		binding.selectActivitySearchInput.setOnEditorActionListener(createEditorActionListener());
		binding.selectActivitySearchInput.setOnFocusChangeListener(createFocusChangeListener());
	}

	private void setupCheckBoxListeners() {
		binding.selectActivityProxiChk.setOnCheckedChangeListener(createProxyCheckListener());
		binding.selectActivityContraChk.setOnCheckedChangeListener(createContraCheckListener());
	}

	private TextWatcher createSearchTextWatcher() {
		return new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				managers.getSearchManager().onSearchTextChanged(s.toString());
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		};
	}

	private View.OnEditorActionListener createEditorActionListener() {
		return (textView, actionId, keyEvent) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				managers.getSearchManager().startActivitySearch();
				managers.getKeyboardManager().hideKeyboard(textView);
				return true;
			}
			return false;
		};
	}

	private View.OnFocusChangeListener createFocusChangeListener() {
		return (v, hasFocus) -> {
			if (hasFocus)
				managers.getKeyboardManager().showKeyboard();
		};
	}

	private CompoundButton.OnCheckedChangeListener createProxyCheckListener() {
		return (buttonView, isChecked) -> {
			managers.getValidationManager().validateSelectionType(
					isChecked || binding.selectActivityContraChk.isChecked());
		};
	}

	private CompoundButton.OnCheckedChangeListener createContraCheckListener() {
		return (buttonView, isChecked) -> {
			managers.getValidationManager().validateSelectionType(
					isChecked || binding.selectActivityProxiChk.isChecked());
		};
	}
}
