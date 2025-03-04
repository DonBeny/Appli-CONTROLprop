package org.orgaprop.test7.utils;

import android.content.Context;
import android.content.Intent;

import org.orgaprop.test7.controllers.activities.SelectListActivity;
import org.orgaprop.test7.controllers.activities.TypeCtrlActivity;
import org.orgaprop.test7.models.SelectItem;

import java.util.ArrayList;
import java.util.List;

public class IntentFactory {

	public static Intent createSearchIntent(Context context, String searchText) {
		Intent intent = new Intent(context, SelectListActivity.class);
		intent.putExtra(SelectListActivity.SELECT_LIST_TYPE, SelectListActivity.SELECT_LIST_TYPE_SEARCH);
		intent.putExtra(SelectListActivity.SELECT_LIST_TXT, searchText.trim());
		return intent;
	}

	public static Intent createSelectIntent(Context context, String type, List<?> cachedList) {
		Intent intent = new Intent(context, SelectListActivity.class);
		intent.putExtra(SelectListActivity.SELECT_LIST_TYPE, type);
		intent.putExtra(SelectListActivity.SELECT_LIST_LIST, new ArrayList<>(cachedList));
		return intent;
	}

	public static Intent createSelectWithParentIntent(Context context, String type, int parentId) {
		Intent intent = new Intent(context, SelectListActivity.class);
		intent.putExtra(SelectListActivity.SELECT_LIST_TYPE, type);
		intent.putExtra(SelectListActivity.SELECT_LIST_ID, parentId);
		return intent;
	}

	public static Intent createTypeCtrlIntent(Context context, SelectItem ficheResid, boolean isProxi, boolean isContra) {
		Intent intent = new Intent(context, TypeCtrlActivity.class);
		intent.putExtra("rsd", ficheResid.getId());
		intent.putExtra("proxi", String.valueOf(isProxi));
		intent.putExtra("contra", String.valueOf(isContra));
		return intent;
	}

}
