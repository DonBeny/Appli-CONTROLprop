package org.orgaprop.test7.controllers.activities;

import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import org.orgaprop.test7.constants.SelectActivityConstants;
import org.orgaprop.test7.databinding.ActivitySelectBinding;
import org.orgaprop.test7.managers.Managers;
import org.orgaprop.test7.contracts.ActivityResultContract;
import org.orgaprop.test7.listeners.SelectActivityListeners;
import org.orgaprop.test7.models.SelectionState;
import org.orgaprop.test7.utils.UiUtils;
import org.orgaprop.test7.viewmodels.SelectActivityViewModel;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Activité principale de sélection
 */
public class SelectActivity extends BaseActivity implements UiCallbacks {
	private static final String TAG = SelectActivity.class.getSimpleName();
	private final CompositeDisposable disposables = new CompositeDisposable();

	private ActivitySelectBinding binding;
	private SelectActivityViewModel viewModel;
	private Managers managers;
	private SelectActivityListeners listeners;
	private ActivityResultContract resultContract;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeComponents();
		setupComponents();
	}

	@Override
	protected void initializeComponents() {
		initializeBinding();
		initializeViewModel();
		initializeManagers();
	}

	private void initializeBinding() {
		binding = ActivitySelectBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		setRequestedOrientation(SelectActivityConstants.Config.SCREEN_ORIENTATION);
	}

	private void initializeViewModel() {
		viewModel = new ViewModelProvider(this).get(SelectActivityViewModel.class);
	}

	private void initializeManagers() {
		managers = new Managers(this, binding, viewModel);
		listeners = new SelectActivityListeners(binding, managers);
		resultContract = new ActivityResultContract(this, managers.getResultHandler());
	}

	@Override
	protected void setupComponents() {
		setupObservers();
		listeners.setup();
	}

	private void setupObservers() {
		viewModel.getSelectionState().observe(this, this::onSelectionUpdated);
		viewModel.isLoading().observe(this, this::onLoadingStateChanged);
		viewModel.getError().observe(this, this::onError);
	}

	@Override
	protected void onResume() {
		super.onResume();
		managers.getStateManager().onResume();
		onLoadingStateChanged(false);
	}

	@Override
	protected void onDestroy() {
		managers.dispose();
		disposables.clear();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (SelectActivityConfig.ENABLE_BACK_BUTTON) {
			super.onBackPressed();
		}
	}

	@Override
	public void onLoadingStateChanged(boolean isLoading) {
		showWait(isLoading);
	}

	@Override
	public void onError(@StringRes int messageId) {
		UiUtils.showToast(this, messageId);
	}

	@Override
	public void onSelectionUpdated(SelectionState state) {
		binding.selectActivityAgcSpinner.setText(state.getAgenceText());
		binding.selectActivityGrpSpinner.setText(state.getGroupementText());
		binding.selectActivityRsdSpinner.setText(state.getResidenceText());
	}

	@Override
	protected void showWait(boolean show) {
		UiUtils.toggleWaitingState(
				binding.selectActivityWaitImg,
				binding.selectActivityMainLayout,
				show);
	}
}
