/*
 * In derogation of the Scoreloop SDK - License Agreement concluded between
 * Licensor and Licensee, as defined therein, the following conditions shall
 * apply for the source code contained below, whereas apart from that the
 * Scoreloop SDK - License Agreement shall remain unaffected.
 * 
 * Copyright: Scoreloop AG, Germany (Licensor)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.scoreloop.client.android.ui.framework;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.phundroid.duck.R;
import com.scoreloop.client.android.ui.framework.ValueStore.Observer;

public abstract class BaseActivity extends Activity implements Observer, Runnable {

	public static enum RefreshMode {
		MERGE, SET
	}

	protected static final int	DIALOG_ERROR_NETWORK	= 0;
	private static boolean		LOG_ENABLED				= false;

	public static void showToast(final Context context, final String message) {
		showToast(context, message, null, Toast.LENGTH_SHORT);
	}

	public static void showToast(final Context context, final String message, final Drawable drawable, final int duration) {
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.sl_dialog_toast, null);
		((TextView) view.findViewById(R.id.message)).setText(message);
		if (drawable != null) {
			((ImageView) view.findViewById(R.id.icon)).setImageDrawable(drawable);
		}
		final Toast toast = new Toast(context.getApplicationContext());
		toast.setDuration(duration);
		toast.setView(view);
		toast.show();
	}

	private View				_contentView;
	private ValueStore			_currentScreenValues;
	private Handler				_handler;
	private boolean				_isPaused;
	private boolean				_needsRefresh;
	private Set<String>			_observedKeys			= null;
	private int					_refreshFlags;
	private ValueStore			_screenValuesSnapshot	= null;
	private final Set<Object>	_spinnerControllers		= new HashSet<Object>();
	private int					_spinnerSemaphore;
	private View				_spinnerView;

	public void addObservedKeys(final String... keys) {
		unobserveKeys();
		if (_observedKeys == null) {
			_observedKeys = new HashSet<String>();
		}
		Collections.addAll(_observedKeys, keys);
		if (!_isPaused) {
			observeKeys();
		}
	}

	private Dialog createErrorDialog(final int resId) {
		final Dialog dialog = new Dialog(getTopParent());
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		final View view = getLayoutInflater().inflate(R.layout.sl_dialog_custom, null);
		dialog.setContentView(view);
		dialog.setCanceledOnTouchOutside(true);
		((TextView) view.findViewById(R.id.message)).setText(getString(resId));
		return dialog;
	}

	protected void display(final ScreenDescription screenDescription) {
		ScreenManagerSingleton.get().display(screenDescription);
	}

	protected void displayPrevious() {
		ScreenManagerSingleton.get().displayPreviousDescription();
	}

	protected void finishDisplay() {
		ScreenManagerSingleton.get().finishDisplay();
	}

	public ValueStore getActivityArguments() {
		final String activityId = getIntent().getStringExtra(ActivityDescription.EXTRA_IDENTIFIER);
		return ScreenManagerSingleton.get().getActivityDescription(activityId).getArguments();
	}

	protected ScreenDescription getCurrentScreenDescription() {
		return ScreenManagerSingleton.get().getCurrentDescription();
	}

	protected Handler getHandler() {
		return _handler;
	}

	public ValueStore getScreenValues() {
		if (_currentScreenValues == null) {
			_currentScreenValues = getCurrentScreenDescription().getScreenValues();
		}
		return _currentScreenValues;
	}

	private FrameLayout getSpinnerParentFrameLayout() {
		ViewParent parent = _contentView.getParent();
		while (parent != null) {
			if (parent instanceof FrameLayout) {
				return (FrameLayout) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private Activity getTopParent() {
		Activity parent = this;
		if (parent.getParent() != null) {
			parent = parent.getParent();
		}
		return parent;
	}

	protected void hideSpinner() {
		if (_spinnerSemaphore == 0) {
			throw new IllegalStateException("spinner not shown you want to hide");
		}
		if (--_spinnerSemaphore == 0) {
			onSpinnerShow(false);
		}
	}

	protected void hideSpinnerFor(final Object controller) {
		if (_spinnerControllers.contains(controller)) {
			_spinnerControllers.remove(controller);
			hideSpinner();
		}
	}

	protected boolean isBackAllowed() {
		return true; // override in subclass if needed
	}

	protected boolean isPaused() {
		return _isPaused;
	}

	private void log(final String method) {
		if (!LOG_ENABLED) {
			return;
		}
		String address = "";
		final String ownString = toString();
		final int index = ownString.lastIndexOf("@");
		if (index != -1) {
			address = ownString.substring(index);
		}
		Log.d("ScoreloopUI", "              > " + getClass().getSimpleName() + "(" + address + ") " + method);
	}

	private void makeScreenValuesSnapshot() {
		if (_observedKeys == null) {
			return;
		}
		if (_screenValuesSnapshot == null) {
			_screenValuesSnapshot = new ValueStore();
		}
		_screenValuesSnapshot.copyFromOtherForKeys(_currentScreenValues, _observedKeys);
	}

	private void observeKeys() {
		if (_observedKeys == null) {
			return;
		}
		final ValueStore screenValues = getScreenValues();
		for (final String key : _observedKeys) {
			screenValues.addObserver(key, this);
		}
	}

	public void onBackPressed() {
		if (isBackAllowed()) {
			displayPrevious();
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_handler = new Handler();
		log("onCreate");
		_isPaused = true; // start with _isPaused flag set
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_ERROR_NETWORK:
			return createErrorDialog(R.string.sl_error_message_network);
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		log("onDestroy");
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
			if (isBackAllowed()) {
				displayPrevious();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onPause();
		log("onPause");
		_isPaused = true;
		unobserveKeys();
		makeScreenValuesSnapshot();
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		switch (id) {
		default:
			super.onPrepareDialog(id, dialog);
			break;
		}
	}

	public void onRefresh(final int flags) {
		// intentionally empty - override in subclass
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		log("onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		log("onResume");
		_isPaused = false;
		observeKeys();
		updateScreenValues();
		refreshIfNeeded();
	}

	protected void onSpinnerShow(final boolean show) {
		if (_spinnerView == null) {
			return;
		}
		if (show) {
			getSpinnerParentFrameLayout().addView(_spinnerView);
		} else {
			getSpinnerParentFrameLayout().removeView(_spinnerView);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		log("onStart");
	}

	@Override
	protected void onStop() {
		super.onStop();
		log("onStop");
	}

	public void onValueChanged(final ValueStore valueStore, final String key, final Object oldValue, final Object newValue) {
		// intentionally empty - override in subclass
	}

	public void onValueSetDirty(final ValueStore valueStore, final String key) {
		// intentionally empty - override in subclass
	}

	protected void refreshIfNeeded() {
		if (!_isPaused && _needsRefresh) {
			_needsRefresh = false;
			final int flags = _refreshFlags;
			_refreshFlags = 0;
			onRefresh(flags);
		}
	}

	public void run() {
		refreshIfNeeded();
	}

	protected void setContentView(final int resId, final boolean supportSpinner) {
		if (supportSpinner) {
			_spinnerView = getLayoutInflater().inflate(R.layout.sl_spinner_view, null);
		}
		_contentView = getLayoutInflater().inflate(resId, null);
		setContentView(_contentView);
	}

	protected void setNeedsRefresh() {
		setNeedsRefresh(0, RefreshMode.MERGE);
	}

	protected void setNeedsRefresh(final int flags, final RefreshMode mode) {
		_needsRefresh = true;
		if (mode == RefreshMode.MERGE) {
			_refreshFlags |= flags;
		} else {
			_refreshFlags = flags;
		}
		getHandler().post(this);
	}

	protected void showDialogForExceptionSafe(final Exception exception) {
		
		// override this in subclasses to take argument into account
		showDialogSafe(DIALOG_ERROR_NETWORK);
	}

	protected void showDialogSafe(final int res) {
		if (!_isPaused) {
			showDialog(res);
		}
	}
	
	protected void showDialogSafe(BaseDialog dialog) {
		if (!_isPaused) {
			dialog.show();
		}
	}
	
	protected void showSpinner() {
		if (_spinnerSemaphore++ == 0) {
			onSpinnerShow(true);
		}
	}

	protected void showSpinnerFor(final Object controller) {
		if (!_spinnerControllers.contains(controller)) {
			_spinnerControllers.add(controller);
			showSpinner();
		}
	}

	protected void showToast(final String message) {
		showToast(this, message);
	}

	private void unobserveKeys() {
		if (_observedKeys == null) {
			return;
		}
		final ValueStore screenValues = getScreenValues();
		for (final String key : _observedKeys) {
			screenValues.removeObserver(key, this);
		}
	}

	private void updateScreenValues() {
		_currentScreenValues = null; // this forces getScreenValues() to retrieve fresh valueStore
		final ValueStore newScreenValues = getScreenValues();

		if (_observedKeys != null) {
			newScreenValues.runObserverForKeys(_screenValuesSnapshot, _observedKeys, this);
		}
	}
}
