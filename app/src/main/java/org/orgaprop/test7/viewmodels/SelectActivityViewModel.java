package org.orgaprop.test7.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.orgaprop.test7.models.SelectionState;

public class SelectActivityViewModel extends ViewModel {

	private final MutableLiveData<SelectionState> selectionState = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
	private final MutableLiveData<Integer> error = new MutableLiveData<>();

	private final CompositeDisposable disposables = new CompositeDisposable();

	public LiveData<SelectionState> getSelectionState() {
		return selectionState;
	}

	public LiveData<Boolean> isLoading() {
		return isLoading;
	}

	public LiveData<Integer> getError() {
		return error;
	}

	public void setSelectionState(SelectionState state) {
		selectionState.setValue(state);
	}

	public void setLoading(boolean loading) {
		isLoading.setValue(loading);
	}

	public void setError(Integer errorResId) {
		error.setValue(errorResId);
	}

	@Override
	protected void onCleared() {
		disposables.clear();
		super.onCleared();
	}

}
