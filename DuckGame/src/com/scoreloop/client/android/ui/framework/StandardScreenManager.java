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

import java.util.ArrayList;
import java.util.List;

import android.os.Debug;

public class StandardScreenManager implements ScreenManager {

	protected static class StackEntry {
		public static int getDepth(final StackEntry entry) {
			return entry == null ? 0 : 1 + getDepth(entry._next);
		}

		private final StackEntry		_next;
		private ScreenActivityProtocol	_screenActivity;

		private final ScreenDescription	_screenDescription;

		private StackEntry(final StackEntry next, final ScreenDescription description, final ScreenActivityProtocol activity) {
			_next = next;
			_screenDescription = description;
			_screenActivity = activity;
		}

		ActivityDescription getActivityDescription(final String id) {
			ActivityDescription description = _screenDescription.getActivityDescription(id);
			if ((description == null) && (_next != null)) {
				description = _next.getActivityDescription(id);
			}
			return description;
		}

		ScreenActivityProtocol getScreenActivity() {
			return _screenActivity;
		}

		ScreenDescription getScreenDescription() {
			return _screenDescription;
		}
	}

	private static final boolean	TRACE_FRAMEWORK			= false;

	private boolean					_frameworkHooksEnabled	= true;
	private Delegate				_delegate;
	private StackEntry				_stack					= null;
	private ScreenDescription		_storedDescription		= null;

	protected void applyCurrentDescription(final ScreenDescription referenceDescription, final int anim) {
		final ScreenDescription screenDescription = getCurrentDescription();
		final ScreenActivityProtocol screenActivity = getCurrentActivity();

		// apply header
		final ActivityDescription headerDescription = screenDescription.getHeaderDescription();
		if (headerDescription == null) {
			throw new IllegalStateException("we don't currently support screens without a header");
		}
		if (referenceDescription != null) {
			headerDescription.setWantsClearTop(wantsNewActivity(headerDescription, referenceDescription.getHeaderDescription()));
		}
		screenActivity.startHeader(headerDescription, anim);

		// apply bodies
		final List<ActivityDescription> bodyDescriptions = screenDescription.getBodyDescriptions();
		final int bodyCount = bodyDescriptions.size();

		// determine if we want a new activity because current model differs from old one
		if (referenceDescription != null) {
			final List<ActivityDescription> referenceBodyDescriptions = referenceDescription.getBodyDescriptions();
			final int count = Math.min(bodyCount, referenceBodyDescriptions.size());
			for (int i = 0; i < count; ++i) {
				final ActivityDescription bodyDescription = bodyDescriptions.get(i);
				bodyDescription.setWantsClearTop(wantsNewActivity(bodyDescription, referenceBodyDescriptions.get(i)));
			}
		}

		if (bodyCount == 0) {
			screenActivity.startEmptyBody();
		} else if (bodyCount == 1) {
			screenActivity.startBody(bodyDescriptions.get(0), anim);
		} else if (bodyCount > 1) {
			storeDescription(screenDescription);
			screenActivity.startTabBody(screenDescription, anim);
		}

		// apply shortcuts
		screenActivity.setShortcuts(screenDescription);
	}

	public void display(final ScreenDescription description) {
		if (description == null) {
			return;
		}
		final ScreenDescription previousDescription = getCurrentDescription();
		if ((previousDescription != null) && wantsNewScreen(description, previousDescription)) {
			storeDescription(description);
			startNewScreen();
		} else {
			pushDescriptionAndActivity(description, getCurrentActivity());
			applyCurrentDescription(previousDescription, ActivityHelper.ANIM_NEXT);
		}
	}

	public void displayInScreen(final ScreenDescription description, final ScreenActivityProtocol screenActivity) {
		pushDescriptionAndActivity(description, screenActivity);
		applyCurrentDescription(getPreviousDescription(), ActivityHelper.ANIM_NEXT);
	}

	public void displayPreviousDescription() {
		final StackEntry previousEntry = popEntry();
		final StackEntry currentEntry = peekEntry();

		if ((currentEntry == null) || (currentEntry.getScreenActivity() != previousEntry.getScreenActivity())) {
			previousEntry.getScreenActivity().getActivity().finish();
			return;
		}
		applyCurrentDescription(previousEntry.getScreenDescription(), ActivityHelper.ANIM_PREVIOUS);
	}

	public void displayReferencedStackEntryInScreen(final int stackEntryReference, final ScreenActivityProtocol newScreenActivity) {

		// NOTE: currently we only support resumption of top-stack-entry
		if (StackEntry.getDepth(_stack) != stackEntryReference) {
			throw new IllegalStateException("wrong stackEntryReference");
		}

		// walk stack and replace occurences of oldScreenActivity with newScreenActivity
		final ScreenActivityProtocol oldScreenActivity = _stack.getScreenActivity();
		for (StackEntry entry = _stack; entry != null; entry = entry._next) {
			if (entry.getScreenActivity() == oldScreenActivity) {
				entry._screenActivity = newScreenActivity;
			}
		}

		applyCurrentDescription(null, ActivityHelper.ANIM_NONE);
	}

	public void displayStoredDescriptionInScreen(final ScreenActivityProtocol screenActivity) {
		if (_storedDescription != null) {
			final ScreenDescription description = _storedDescription;
			_storedDescription = null;
			displayInScreen(description, screenActivity);
		}
	}

	public void displayStoredDescriptionInTabs(final TabsActivityProtocol tabs) {
		if (_storedDescription != null) {
			final ScreenDescription description = _storedDescription;
			_storedDescription = null;
			tabs.startDescription(description);
		}
	}

	public void displayWithEmptyStack(final ScreenDescription description) {

		// get description before unwinding stack
		final ScreenDescription previousDescription = getCurrentDescription();

		// finish first n-1 screen-activities
		final List<ScreenActivityProtocol> screenActivities = getScreenActivities();
		final int count = screenActivities.size() - 1;
		for (int i = 0; i < count; ++i) {
			screenActivities.get(i).getActivity().finish();
		}

		// empty stack (but without leaving framework)
		setFrameworkHooksEnabled(false);
		setStack(null);

		// and start afresh with last scren-activity - but without any animations
		pushDescriptionAndActivity(description, screenActivities.get(count));
		applyCurrentDescription(previousDescription, ActivityHelper.ANIM_NONE);
		setFrameworkHooksEnabled(true);
	}

	public void finishDisplay() {

		// finish all screen-activities
		for (final ScreenActivityProtocol screenActivity : getScreenActivities()) {
			screenActivity.getActivity().finish();
		}

		// empty stack
		setStack(null);
	}

	public ActivityDescription getActivityDescription(final String id) {
		return _stack.getActivityDescription(id);
	}

	protected ScreenActivityProtocol getCurrentActivity() {
		return peekEntry().getScreenActivity();
	}

	public ScreenDescription getCurrentDescription() {
		return peekEntry().getScreenDescription();
	}

	public int getCurrentStackEntryReference() {
		return StackEntry.getDepth(_stack);
	}

	protected ScreenDescription getPreviousDescription() {
		if (_stack == null) {
			return null;
		}
		final StackEntry previousEntry = _stack._next;
		return previousEntry != null ? previousEntry.getScreenDescription() : null;
	}

	private List<ScreenActivityProtocol> getScreenActivities() {

		// walk stack and collect a list of all non-duplicate screen-activities
		final List<ScreenActivityProtocol> screenActivities = new ArrayList<ScreenActivityProtocol>();
		ScreenActivityProtocol lastAddedActivity = null;
		for (StackEntry entry = _stack; entry != null; entry = entry._next) {
			final ScreenActivityProtocol activity = entry.getScreenActivity();
			if ((lastAddedActivity == null) || (activity != lastAddedActivity)) {
				screenActivities.add(activity);
				lastAddedActivity = activity;
			}
		}
		return screenActivities;
	}

	private void onFrameworkEntered() {
		if (TRACE_FRAMEWORK) {
			Debug.startMethodTracing("ui-framework");
		}
		if (_delegate != null) {
			_delegate.screenManagerWillEnterFramework(this);
		}
	}

	private void onFrameworkLeft() {
		if (TRACE_FRAMEWORK) {
			Debug.stopMethodTracing();
		}
		if (_delegate != null) {
			_delegate.screenManagerDidLeaveFramework(this);
		}
	}

	private StackEntry peekEntry() {
		return _stack;
	}

	private StackEntry popEntry() {
		if (_stack == null) {
			return null;
		}
		final StackEntry previous = _stack;
		setStack(_stack._next);
		return previous;
	}

	private void pushDescriptionAndActivity(final ScreenDescription model, final ScreenActivityProtocol screen) {
		setStack(new StackEntry(_stack, model, screen));
	}

	private void setFrameworkHooksEnabled(final boolean enabled) {
		_frameworkHooksEnabled = enabled;
	}

	public void setDelegate(final Delegate policy) {
		_delegate = policy;
	}

	// debug only

	private void setStack(final StackEntry entry) {
		final StackEntry oldStack = _stack;
		_stack = entry;
		if (!_frameworkHooksEnabled) {
			return;
		}
		if ((oldStack == null) && (entry != null)) {
			onFrameworkEntered();
		} else if ((oldStack != null) && (entry == null)) {
			onFrameworkLeft();
		}
	}

	private void startNewScreen() {
		final ScreenActivityProtocol oldScreen = getCurrentActivity();
		oldScreen.startNewScreen();
	}

	private void storeDescription(final ScreenDescription model) {
		_storedDescription = model;
	}

	private boolean wantsNewActivity(final ActivityDescription description, final ActivityDescription referenceDescription) {
		// NOTE: currently we don't support recycled activities
		return true;
	}

	private boolean wantsNewScreen(final ScreenDescription description, final ScreenDescription referenceDescription) {
		if (_delegate != null) {
			return _delegate.screenManagerWantsNewScreen(null, description, referenceDescription);
		}
		return false;
	}
}

/*
 * Design Notes on Activity-Lifecycle:
 * 
 *	To force the creation of new activities we set the FLAG_ACTIVITY_CLEAR_TOP flag on the intents to be started.
 * 
 * When an activity has the clear-top flag set, we want a new activity to be created only the
 * first time the tab gets selected and not everytime thereafter the activity is tabbed. We
 * therefore have the enableWantsClearTop flag which is disabled once a tab was shown and enabled
 * when the tab activity gets a new model.
 */
