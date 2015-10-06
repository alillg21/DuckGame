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

package com.scoreloop.client.android.ui.component.user;

import java.util.List;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.phundroid.duck.R;
import com.scoreloop.client.android.core.controller.MessageController;
import com.scoreloop.client.android.core.controller.RequestController;
import com.scoreloop.client.android.core.model.User;
import com.scoreloop.client.android.ui.component.agent.ManageBuddiesTask;
import com.scoreloop.client.android.ui.component.agent.ManageBuddiesTask.ManageBuddiesContinuation;
import com.scoreloop.client.android.ui.component.base.ComponentHeaderActivity;
import com.scoreloop.client.android.ui.component.base.Constant;
import com.scoreloop.client.android.ui.framework.ValueStore;
import com.scoreloop.client.android.ui.util.ImageDownloader;

public class UserHeaderActivity extends ComponentHeaderActivity implements OnClickListener {

	public enum ControlMode {
		BLANK, BUDDY, PROFILE
	}

	private static final int	MENU_FLAG_INAPPROPRIATE	= 0x128;
	private static final int	MENU_REMOVE_BUDDY		= 0x100;

	private boolean				_canRemoveBuddy;
	private MessageController	_messageController;
	private ControlMode			_mode;

	private void disableAddRemoveBuddiesControl() {
		hideControlIcon();
		_canRemoveBuddy = false;
	}

	private String formatInteger(final ValueStore store, final String key) {
		final Integer value = store.getValue(key);
		return value != null ? value.toString() : "";
	}

	private ImageView hideControlIcon() {
		final ImageView icon = (ImageView) findViewById(R.id.sl_control_icon);
		icon.setImageDrawable(null);
		icon.setOnClickListener(null);
		icon.setEnabled(false);
		final View view = findViewById(R.id.sl_header_layout);
		view.setEnabled(true);
		view.setOnClickListener(null);
		return icon;
	}

	@Override
	public void onClick(final View view) {
		final User user = getUser();
		if (_mode == ControlMode.PROFILE) {
			display(getFactory().createProfileSettingsScreenDescription(user));
		} else if (_mode == ControlMode.BUDDY) {
			disableAddRemoveBuddiesControl();
			ManageBuddiesTask.addBuddy(user, getManager(), new ManageBuddiesContinuation() {
				public void withAddedOrRemovedBuddies(final int count) {
					if (!isPaused()) {
						showToast(String.format(getString(R.string.sl_format_added_as_friend), user.getDisplayName()));
					}
				}
			});
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.sl_header_user);

		addObservedContentKeys(Constant.USER_NAME, Constant.USER_IMAGE_URL, Constant.NUMBER_GAMES,
				Constant.NUMBER_BUDDIES, Constant.NUMBER_GLOBAL_ACHIEVEMENTS);
		addObservedKeys(ValueStore.concatenateKeys(Constant.SESSION_USER_VALUES, Constant.USER_BUDDIES));

		_mode = getActivityArguments().getValue(Constant.MODE, ControlMode.BLANK);
		getImageView().setImageDrawable(getResources().getDrawable(R.drawable.sl_header_icon_user));

		switch (_mode) {
		case PROFILE:
			showControlIcon(R.drawable.sl_button_profile, true);
			break;

		default:
			disableAddRemoveBuddiesControl();
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(Menu.NONE, MENU_REMOVE_BUDDY, Menu.NONE, R.string.sl_remove_friend).setIcon(R.drawable.sl_icon_remove_friend);
		if (!isSessionUser()) {
			menu.add(Menu.NONE, MENU_FLAG_INAPPROPRIATE, Menu.NONE, R.string.sl_abuse_report_title).setIcon(
					R.drawable.sl_icon_flag_inappropriate);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REMOVE_BUDDY:
			disableAddRemoveBuddiesControl();
			final User user = getUser();
			ManageBuddiesTask.removeBuddy(user, getManager(), new ManageBuddiesContinuation() {
				public void withAddedOrRemovedBuddies(final int count) {
					if (!isPaused()) {
						showToast(String.format(getString(R.string.sl_format_removed_as_friend), user.getDisplayName()));
					}
				}
			});
			return true;

		case MENU_FLAG_INAPPROPRIATE:
			postAbuseReport();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem item = menu.findItem(MENU_REMOVE_BUDDY);
		if (item != null) {
			item.setVisible(_canRemoveBuddy);
			return _canRemoveBuddy;
		}
		return false;
	}

	@Override
	public void onValueChanged(final ValueStore valueStore, final String key, final Object oldValue, final Object newValue) {
		updateUI();
	}

	@Override
	public void onValueSetDirty(final ValueStore valueStore, final String key) {
		retrieveContentValueFor(key, Constant.USER_NAME, ValueStore.RetrievalMode.NOT_DIRTY, null);
		retrieveContentValueFor(key, Constant.USER_IMAGE_URL, ValueStore.RetrievalMode.NOT_DIRTY, null);
		retrieveContentValueFor(key, Constant.NUMBER_GAMES, ValueStore.RetrievalMode.NOT_DIRTY, null);
		retrieveContentValueFor(key, Constant.NUMBER_BUDDIES, ValueStore.RetrievalMode.NOT_DIRTY, null);
		retrieveContentValueFor(key, Constant.NUMBER_GLOBAL_ACHIEVEMENTS, ValueStore.RetrievalMode.NOT_DIRTY, null);
		if (_mode == ControlMode.BUDDY &&  Constant.USER_BUDDIES.equals(key)) {
			getSessionUserValues().retrieveValue(Constant.USER_BUDDIES, ValueStore.RetrievalMode.NOT_DIRTY, null);
		}
	}

	private void postAbuseReport() {
		_messageController = new MessageController(getRequestControllerObserver());

		_messageController.setTarget(getUser());
		_messageController.setMessageType(MessageController.TYPE_ABUSE_REPORT);
		_messageController.setText("Inappropriate user in ScoreloopUI");
		_messageController.addReceiverWithUsers(MessageController.RECEIVER_SYSTEM);

		if (_messageController.isSubmitAllowed()) {
			_messageController.submitMessage();
		}
	}

	@Override
	public void requestControllerDidReceiveResponseSafe(final RequestController requestController) {
		if (requestController == _messageController) {
			showToast(getString(R.string.sl_abuse_report_sent));
		}
	}

	private void setNumberBuddies(final String text) {
		final TextView textView = (TextView) findViewById(R.id.sl_header_number_friends);
		textView.setText(text);
	}

	private void setNumberGames(final String text) {
		final TextView textView = (TextView) findViewById(R.id.sl_header_number_games);
		textView.setText(text);
	}

	private void setNumberGlobalAchievements(final String text) {
		final TextView textView = (TextView) findViewById(R.id.sl_header_number_achievements);
		textView.setText(text);
	}

	private void showControlIcon(final int resId, final boolean isHeaderClickable) {
		final ImageView icon = hideControlIcon();
		icon.setImageResource(resId);
		icon.setEnabled(true);
		icon.setOnClickListener(this);
		if (isHeaderClickable) {
			final View view = findViewById(R.id.sl_header_layout);
			view.setEnabled(true);
			view.setOnClickListener(this);
		}
	}

	private void updateAddRemoveBuddiesControl() {
		if (_mode != ControlMode.BUDDY) {
			return;
		}
		final List<User> buddyUsers = getSessionUserValues().getValue(Constant.USER_BUDDIES);
		if (buddyUsers == null) {
			return;
		}
		if (!buddyUsers.contains(getUser())) {
			showControlIcon(R.drawable.sl_button_add_friend, false);
			_canRemoveBuddy = false;
		} else {
			hideControlIcon();
			_canRemoveBuddy = true;
		}
	}

	private void updateUI() {
		final ValueStore store = getContentValues();
		String imageUrl = store.<String> getValue(Constant.USER_IMAGE_URL);
		if(imageUrl != null) {
			ImageDownloader.downloadImage(imageUrl,
					getResources().getDrawable(R.drawable.sl_header_icon_user), getImageView());
		}
		else {
			getImageView().setImageResource(R.drawable.sl_header_icon_user);
		}
		setTitle(store.<String> getValue(Constant.USER_NAME));
		setNumberGames(formatInteger(store, Constant.NUMBER_GAMES));
		setNumberBuddies(formatInteger(store, Constant.NUMBER_BUDDIES));
		setNumberGlobalAchievements(formatInteger(store, Constant.NUMBER_GLOBAL_ACHIEVEMENTS));
		
		updateAddRemoveBuddiesControl();
	}
}
