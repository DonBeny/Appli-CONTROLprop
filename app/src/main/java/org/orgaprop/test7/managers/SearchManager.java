package org.orgaprop.test7.managers;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SearchManager {

	private final CompositeDisposable disposables = new CompositeDisposable();
	private final PublishSubject<String> searchSubject = PublishSubject.create();
	private final SelectActivityManager activityManager;
	private final ActivityStateManager stateManager;

	public SearchManager(SelectActivityManager activityManager, ActivityStateManager stateManager) {
		this.activityManager = activityManager;
		this.stateManager = stateManager;
	}

	public void setup() {
		disposables.add(
				searchSubject
						.debounce(SelectActivityConfig.SEARCH_DELAY_MS, TimeUnit.MILLISECONDS)
						.filter(text -> text.length() >= SelectActivityConfig.SEARCH_MIN_LENGTH)
						.distinctUntilChanged()
						.observeOn(AndroidSchedulers.mainThread())
						.subscribe(
								query -> {
									stateManager.setWaitDownload(true);
									activityManager.launchSearch(query);
								},
								error -> {
									stateManager.setWaitDownload(false);
									// Notifier l'erreur via callback
								}));
	}

	public void onSearchTextChanged(String text) {
		searchSubject.onNext(text);
	}

	public void dispose() {
		disposables.clear();
	}

}
