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

package com.scoreloop.client.android.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.phundroid.duck.R;
import com.scoreloop.client.android.core.controller.ChallengeController;
import com.scoreloop.client.android.core.controller.ChallengeControllerObserver;
import com.scoreloop.client.android.core.controller.RequestController;
import com.scoreloop.client.android.core.controller.ScoreController;
import com.scoreloop.client.android.core.model.Achievement;
import com.scoreloop.client.android.core.model.AwardList;
import com.scoreloop.client.android.core.model.Challenge;
import com.scoreloop.client.android.core.model.Client;
import com.scoreloop.client.android.core.model.Game;
import com.scoreloop.client.android.core.model.Score;
import com.scoreloop.client.android.core.model.SearchList;
import com.scoreloop.client.android.core.model.Session;
import com.scoreloop.client.android.core.model.User;
import com.scoreloop.client.android.ui.component.achievement.AchievementHeaderActivity;
import com.scoreloop.client.android.ui.component.achievement.AchievementListActivity;
import com.scoreloop.client.android.ui.component.achievement.AchievementsEngine;
import com.scoreloop.client.android.ui.component.agent.NewsAgent;
import com.scoreloop.client.android.ui.component.agent.NumberAchievementsAgent;
import com.scoreloop.client.android.ui.component.agent.UserAgent;
import com.scoreloop.client.android.ui.component.agent.UserBuddiesAgent;
import com.scoreloop.client.android.ui.component.agent.UserDetailsAgent;
import com.scoreloop.client.android.ui.component.base.Configuration;
import com.scoreloop.client.android.ui.component.base.Constant;
import com.scoreloop.client.android.ui.component.base.Factory;
import com.scoreloop.client.android.ui.component.base.Manager;
import com.scoreloop.client.android.ui.component.base.Configuration.Feature;
import com.scoreloop.client.android.ui.component.challenge.ChallengeAcceptListActivity;
import com.scoreloop.client.android.ui.component.challenge.ChallengeCreateListActivity;
import com.scoreloop.client.android.ui.component.challenge.ChallengeHeaderActivity;
import com.scoreloop.client.android.ui.component.challenge.ChallengeListActivity;
import com.scoreloop.client.android.ui.component.challenge.ChallengePaymentActivity;
import com.scoreloop.client.android.ui.component.entry.EntryListActivity;
import com.scoreloop.client.android.ui.component.game.GameDetailHeaderActivity;
import com.scoreloop.client.android.ui.component.game.GameDetailListActivity;
import com.scoreloop.client.android.ui.component.game.GameListActivity;
import com.scoreloop.client.android.ui.component.market.MarketHeaderActivity;
import com.scoreloop.client.android.ui.component.market.MarketListActivity;
import com.scoreloop.client.android.ui.component.news.NewsHeaderActivity;
import com.scoreloop.client.android.ui.component.news.NewsListActivity;
import com.scoreloop.client.android.ui.component.profile.ProfileSettingsListActivity;
import com.scoreloop.client.android.ui.component.profile.ProfileSettingsPictureListActivity;
import com.scoreloop.client.android.ui.component.score.ScoreHeaderActivity;
import com.scoreloop.client.android.ui.component.score.ScoreListActivity;
import com.scoreloop.client.android.ui.component.user.UserAddBuddyListActivity;
import com.scoreloop.client.android.ui.component.user.UserDetailListActivity;
import com.scoreloop.client.android.ui.component.user.UserHeaderActivity;
import com.scoreloop.client.android.ui.component.user.UserListActivity;
import com.scoreloop.client.android.ui.framework.BaseActivity;
import com.scoreloop.client.android.ui.framework.ScreenDescription;
import com.scoreloop.client.android.ui.framework.ScreenManager;
import com.scoreloop.client.android.ui.framework.ScreenManagerSingleton;
import com.scoreloop.client.android.ui.framework.StandardScreenManager;
import com.scoreloop.client.android.ui.framework.ValueStore;
import com.scoreloop.client.android.ui.framework.ScreenDescription.ShortcutObserver;
import com.scoreloop.client.android.ui.framework.ScreenManager.Delegate;
import com.scoreloop.client.android.ui.framework.ValueStore.ValueSource;
import com.scoreloop.client.android.ui.framework.ValueStore.ValueSourceFactory;

class StandardScoreloopManager implements ScoreloopManager, Manager, Factory, ShortcutObserver, Delegate, ValueSourceFactory,
		ChallengeControllerObserver, Runnable {

	class Checker {

		private static final String			SCORELOOP_UI	= "ScoreloopUI";

		private int							_counter;
		private final Map<String, Object>	_infos;
		private final String				_kind;
		private final List<String>			_missing		= new ArrayList<String>();
		private boolean						_shouldBail;

		Checker(final String kind, final Map<String, Object> infos) {
			_kind = kind;
			_infos = infos;
		}

		void add(final String name, final Feature feature) {
			if (!_configuration.isFeatureEnabled(feature)) {
				return;
			}
			add(name);
		}

		public void add(final String name, final Object... keyValuePairs) {
			++_counter;
			if (!_infos.containsKey(name)) {
				_missing.add(format(name, keyValuePairs));
			}
			for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
				final String key = (String) keyValuePairs[i];
				final Object expectedValue = keyValuePairs[i + 1];

				@SuppressWarnings("unchecked")
				final Map<String, Object> details = (Map<String, Object>) _infos.get(name);
				if (details != null) {
					final Object actualValue = details.get(key);
					if ((actualValue != null) && actualValue.equals(expectedValue)) {
						continue;
					}
				}
				_missing.add(format(name, keyValuePairs));
				return;
			}
		}

		void check() {
			if (_shouldBail) {
				throw new VerifyException();
			}
		}

		private String format(final String name, final Object[] keyValuePairs) {
			final StringBuilder buffer = new StringBuilder();
			buffer.append("android:name=\"");
			buffer.append(name);
			buffer.append("\"");
			for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
				final String key = (String) keyValuePairs[i];
				final Object value = keyValuePairs[i + 1];
				buffer.append(" android:");
				buffer.append(key);
				buffer.append("=\"");
				if (key.equals("theme")) { // add other keys for which the value should be formatted as resource-name
					buffer.append(getContext().getResources().getResourceName((Integer) value));
				} else {
					buffer.append(value.toString());
				}
				buffer.append("\"");
			}
			return buffer.toString();
		}

		private void informDeveloper(final String detail) {
			Log.e(SCORELOOP_UI, "=====================================================================================");
			Log.e(SCORELOOP_UI, "Manifest file verification error. Please resolve any issues first!");
			Log.e(SCORELOOP_UI, detail);
			for (final String entry : _missing) {
				Log.e(SCORELOOP_UI, "<" + _kind + " " + entry + "/>");
			}
		}

		void reportOptional() {
			if (_counter == _missing.size()) {
				informDeveloper("At least one of following entries is mssing in your AndroidManifest.xml file:");
				_shouldBail = true;
			}
		}

		void reportRequired() {
			if (_missing.size() > 0) {
				informDeveloper("All the following entries are missing in your AndroidManifest.xml file:");
				_shouldBail = true;
			}
		}
	}

	private static class UserGameKey {
		private final String	_gameId;
		private final String	_userId;

		private UserGameKey(final String userId, final String gameId) {
			_userId = userId;
			_gameId = gameId;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final UserGameKey other = (UserGameKey) obj;
			if (_gameId == null) {
				if (other._gameId != null) {
					return false;
				}
			} else if (!_gameId.equals(other._gameId)) {
				return false;
			}
			if (_userId == null) {
				if (other._userId != null) {
					return false;
				}
			} else if (!_userId.equals(other._userId)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_gameId == null) ? 0 : _gameId.hashCode());
			result = prime * result + ((_userId == null) ? 0 : _userId.hashCode());
			return result;
		}
	}

	private static class VerifyException extends RuntimeException {
		private static final long	serialVersionUID	= 1L;

		VerifyException() {
			super(("Manifest Verification Error! See logcat output!"));
		}
	}

	private static final int	CACHE_COLLECTION_THRESHOLD	= 3;
	private static final String	PREFERENCES_ENTRY_USER_NAME	= "userName";
	private static final String	PREFERENCES_NAME			= "com.scoreloop.ui.login";

	private static boolean containsKey(final String[] keys, final String key) {
		for (final String string : keys) {
			if (string.equalsIgnoreCase(key)) {
				return true;
			}
		}
		return false;
	}

	static StandardScoreloopManager getFactory(final ScoreloopManager scoreloopManager) {
		return (StandardScoreloopManager) scoreloopManager;
	}

	private AchievementsEngine									_achievementsEngine;
	private int													_cacheCollectionCounter;
	private ChallengeController									_challengeController;
	private boolean												_challengeOngoing;
	private final Client										_client;
	private final Configuration									_configuration;
	private final Context										_context;
	private final Handler										_handler			= new Handler();
	private Challenge											_lastChallenge;
	private Score												_lastScore;
	private int													_lastStatus;
	private OnCanStartGamePlayObserver							_onCanStartGamePlayObserver;
	private OnScoreSubmitObserver								_onScoreSubmitObserver;
	private OnStartGamePlayRequestObserver						_onStartGamePlayRequestObserver;
	private ScoreController										_scoreController;
	private ValueStore											_sessionUserValueStore;
	private final Map<UserGameKey, WeakReference<ValueStore>>	_valueStoreCache	= new HashMap<UserGameKey, WeakReference<ValueStore>>();

	StandardScoreloopManager(final Context context) {
		_context = context;
		_client = new Client(_context, null);

		_configuration = new Configuration(_context, getSession());
		verifyManifest();

		final ScreenManager screenManager = new StandardScreenManager();
		screenManager.setDelegate(this);
		ScreenManagerSingleton.init(screenManager);

		Constant.setup();
	}

	public void achieveAward(final String awardId, final boolean showToast, final boolean submitNow) {
		final Achievement achievement = getAchievement(awardId); // checks made here
		if (!achievement.isAchieved()) {
			achievement.setAchieved();
			if (showToast) {
				showToastForAchievement(achievement);
			}
			if (submitNow) {
				submitAchievements(null);
			}
		}
	}

	public boolean canStartGamePlay() {
		if (_onCanStartGamePlayObserver == null) {
			throw new IllegalStateException(
					"trying to check if gameplay can be started, but the callback is not set - did you call ScoreloopManagerSingleton.get().setOnCanStartGamePlayObserver(...)?");
		}
		return _onCanStartGamePlayObserver.onCanStartGamePlay();
	}

	public void challengeControllerDidFailOnInsufficientBalance(final ChallengeController challengeController) {
		if (challengeController == _challengeController) {
			_lastStatus = OnScoreSubmitObserver.STATUS_ERROR_BALANCE;
			_lastChallenge = null;
			_challengeOngoing = false;
			if (_onScoreSubmitObserver != null) {
				_onScoreSubmitObserver.onScoreSubmit(_lastStatus, null);
			}
		}
	}

	public void challengeControllerDidFailToAcceptChallenge(final ChallengeController challengeController) {
		requestControllerDidFail(challengeController, new RuntimeException("challengeControllerDidFailToAcceptChallenge"));
	}

	public void challengeControllerDidFailToRejectChallenge(final ChallengeController challengeController) {
		requestControllerDidFail(challengeController, new RuntimeException("challengeControllerDidFailToRejectChallenge"));
	}

	private void checkHasAwards() {
		if (!_configuration.isFeatureEnabled(Feature.ACHIEVEMENT)) {
			throw new IllegalStateException("you have to set 'ui.feature.achievement = true' in the scoreloop.properties first!");
		}
	}

	private void checkHasLoadedAchievements() {
		if (!hasLoadedAchievements()) {
			throw new IllegalStateException("you have to load the achievements first!");
		}
	}

	public ScreenDescription createAchievementScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), AchievementHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), AchievementListActivity.class));
		return description;
	}

	public ScreenDescription createChallengeAcceptScreenDescription(final Challenge challenge) {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), ChallengeHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), ChallengeAcceptListActivity.class)).getArguments().putValue(
				Constant.CHALLENGE, challenge);

		return description;
	}

	public ScreenDescription createChallengeCreateScreenDescription(final User user, final Integer mode) {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), ChallengeHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), ChallengeCreateListActivity.class)).getArguments().putValue(
				Constant.CONTESTANT, user);

		return description;
	}

	public ScreenDescription createChallengePaymentScreenDescription() {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), ChallengeHeaderActivity.class)).getArguments().putValue(
				Constant.CHALLENGE_HEADER_MODE, Constant.CHALLENGE_HEADER_MODE_BUY);
		description.setBodyDescription(new Intent(getContext(), ChallengePaymentActivity.class));
		return description;
	}

	public ScreenDescription createChallengeScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), ChallengeHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), ChallengeListActivity.class));
		return description;
	}

	private ValueStore createContentValues(final User user, final Game game) {
		if ((user == null) || (game == null)) {
			throw new IllegalArgumentException("user and game must not be null");
		}
		final ValueStore valueStore = new ValueStore();

		valueStore.setValueSourceFactroy(this);
		valueStore.putValue(Constant.USER, user);
		valueStore.putValue(Constant.GAME, game);

		return valueStore;
	}

	ScreenDescription createEntryScreenDescription() {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class)).getArguments().putValue(Constant.MODE,
				UserHeaderActivity.ControlMode.PROFILE);
		description.setBodyDescription(new Intent(getContext(), EntryListActivity.class));
		description.setShortcutSelectionId(R.string.sl_home);
		return description;
	}

	public ScreenDescription createGameDetailScreenDescription(final Game game) {
		final ScreenDescription description = createScreenDescription(null, game, false);
		description.setHeaderDescription(new Intent(getContext(), GameDetailHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), GameDetailListActivity.class));
		return description;
	}

	public ScreenDescription createGameScreenDescription(final User user, final int mode) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), mode == Constant.GAME_MODE_USER ? UserHeaderActivity.class
				: MarketHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), GameListActivity.class)).getArguments().putValue(Constant.MODE, mode);
		return description;
	}

	public ScreenDescription createMarketScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, false);
		description.setHeaderDescription(new Intent(getContext(), MarketHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), MarketListActivity.class));
		description.setShortcutSelectionId(R.string.sl_market);
		return description;
	}

	public ScreenDescription createNewsScreenDescription() {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), NewsHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), NewsListActivity.class));
		return description;
	}

	public ScreenDescription createProfileSettingsPictureScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), ProfileSettingsPictureListActivity.class));
		return description;
	}

	public ScreenDescription createProfileSettingsScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), ProfileSettingsListActivity.class));
		return description;
	}

	public ScreenDescription createScoreScreenDescription(final Game game, final Integer mode, final Integer leaderboard) {
		final ScreenDescription description = createScreenDescription(null, game, true);
		description.getScreenValues().putValue(Constant.MODE, mode != null ? mode : ensureGame(game).getMinMode());

		description.setHeaderDescription(new Intent(getContext(), ScoreHeaderActivity.class));

		description.addBodyDescription(R.string.sl_global, new Intent(getContext(), ScoreListActivity.class)).getArguments().putValue(
				Constant.SEARCH_LIST, SearchList.getGlobalScoreSearchList());
		description.addBodyDescription(R.string.sl_friends, new Intent(getContext(), ScoreListActivity.class)).getArguments().putValue(
				Constant.SEARCH_LIST, SearchList.getBuddiesScoreSearchList());
		description.addBodyDescription(R.string.sl_twentyfour, new Intent(getContext(), ScoreListActivity.class)).getArguments().putValue(
				Constant.SEARCH_LIST, SearchList.getTwentyFourHourScoreSearchList());
		if (leaderboard != null) {
			description.setSelectedBodyIndex(leaderboard);
		}

		return description;
	}

	private ScreenDescription createScreenDescription(User user, Game game, final boolean useCached) {
		final ScreenDescription description = new ScreenDescription();

		final ValueStore screenValues = description.getScreenValues();
		user = ensureUser(user);
		game = ensureGame(game);
		screenValues.putValue(Constant.CONTENT_VALUES, useCached ? getContentValues(user, game) : createContentValues(user, game));
		screenValues.putValue(Constant.SESSION_USER_VALUES, getSessionUserValues());
		screenValues.putValue(Constant.MANAGER, this);
		screenValues.putValue(Constant.FACTORY, this);
		screenValues.putValue(Constant.CONFIGURATION, _configuration);

		description.addShortcutObserver(this);
		description.addShortcutDescription(R.string.sl_home, R.drawable.sl_shortcut_home_default, R.drawable.sl_shortcut_home_active);
		description.addShortcutDescription(R.string.sl_friends, R.drawable.sl_shortcut_friends_default,
				R.drawable.sl_shortcut_friends_active);
		description.addShortcutDescription(R.string.sl_market, R.drawable.sl_shortcut_market_default, R.drawable.sl_shortcut_market_active);

		return description;
	}

	public ScreenDescription createUserAddBuddyScreenDescription() {
		final ScreenDescription description = createScreenDescription(null, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class)).getArguments().putValue(Constant.MODE,
				UserHeaderActivity.ControlMode.BLANK);
		description.setBodyDescription(new Intent(getContext(), UserAddBuddyListActivity.class));
		return description;
	}

	public ScreenDescription createUserDetailScreenDescription(final User user, final Boolean playsSessionGame) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class)).getArguments().putValue(Constant.MODE,
				UserHeaderActivity.ControlMode.BUDDY);
		description.setBodyDescription(new Intent(getContext(), UserDetailListActivity.class)).getArguments().putValue(
				Constant.USER_PLAYS_SESSION_GAME, playsSessionGame);
		return description;
	}

	public ScreenDescription createUserGameScreenDescription(final User user, final int mode) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class));
		description.setBodyDescription(new Intent(getContext(), GameListActivity.class)).getArguments().putValue(Constant.MODE, mode);
		return description;
	}

	public ScreenDescription createUserScreenDescription(final User user) {
		final ScreenDescription description = createScreenDescription(user, null, true);
		description.setHeaderDescription(new Intent(getContext(), UserHeaderActivity.class)).getArguments().putValue(Constant.MODE,
				ensureUser(user).ownsSession(getSession()) ? UserHeaderActivity.ControlMode.BLANK : UserHeaderActivity.ControlMode.BUDDY);
		description.setBodyDescription(new Intent(getContext(), UserListActivity.class));
		if (ensureUser(user).ownsSession(getSession())) {
			description.setShortcutSelectionId(R.string.sl_friends);
		}
		return description;
	}

	private void displayWithEmptyStack(final ScreenDescription description) {
		ScreenManagerSingleton.get().displayWithEmptyStack(description);
	}

	private Game ensureGame(final Game game) {
		return game != null ? game : getSession().getGame();
	}

	private User ensureUser(final User user) {
		return user != null ? user : getSession().getUser();
	}

	private Map<String, Object> extractActivityInfo(final PackageInfo packageInfo) {
		final Map<String, Object> result = new HashMap<String, Object>();
		for (final ActivityInfo info : packageInfo.activities) {
			Map<String, Object> details = null;
			if (info.theme != 0) {
				details = new HashMap<String, Object>();
				details.put("theme", info.theme);
			}
			result.put(info.name, details);
		}
		return result;
	}

	private Map<String, Object> extractPermissionInfo(final PackageInfo packageInfo) {
		final Map<String, Object> result = new HashMap<String, Object>();
		final String[] permissions = packageInfo.requestedPermissions;
		if (permissions != null) {
			for (final String name : permissions) {
				result.put(name, null);
			}
		}
		return result;
	}

	public Achievement getAchievement(final String awardId) {
		checkHasAwards();
		checkHasLoadedAchievements();
		return _achievementsEngine.getAchievementsController().getAchievementForAwardIdentifier(awardId);
	}

	public List<Achievement> getAchievements() {
		checkHasAwards();
		checkHasLoadedAchievements();
		return _achievementsEngine.getAchievementsController().getAchievements();
	}

	private AchievementsEngine getAchievementsEngine() {
		if (_achievementsEngine == null) {
			_achievementsEngine = new AchievementsEngine();
		}
		return _achievementsEngine;
	}

	public AwardList getAwardList() {
		checkHasAwards();
		return getAchievementsEngine().getAchievementsController().getAwardList();
	}

	private ChallengeController getChallengeController() {
		if (_challengeController == null) {
			_challengeController = new ChallengeController(this);
		}
		return _challengeController;
	}

	Configuration getConfiguration() {
		return _configuration;
	}

	private Object getContentValues(final User user, final Game game) {

		// NOTE: the following is necessary as the session user does not have stable hash and equals before and after authentication :-(
		final String userId = user == getSession().getUser() ? "session-user" : user.getIdentifier();
		final UserGameKey key = new UserGameKey(userId, game.getIdentifier());

		// first look into cache if we can recycle a store
		WeakReference<ValueStore> weakValueStore = _valueStoreCache.get(key);
		if (weakValueStore != null) {
			final ValueStore valueStore = weakValueStore.get();
			if (valueStore != null) {
				return valueStore;
			}
		}

		// not found, so create one and cache it
		final ValueStore valueStore = createContentValues(user, game);
		_valueStoreCache.put(key, new WeakReference<ValueStore>(valueStore));

		// compact the cache every now and then
		if (++_cacheCollectionCounter >= CACHE_COLLECTION_THRESHOLD) {
			_cacheCollectionCounter = 0;
			for (final UserGameKey iterKey : new HashSet<UserGameKey>(_valueStoreCache.keySet())) {
				weakValueStore = _valueStoreCache.get(iterKey);
				if (weakValueStore.get() == null) {
					_valueStoreCache.remove(iterKey);
				}
			}
		}

		return valueStore;
	}

	private Context getContext() {
		return _context;
	}

	Challenge getLastChallenge() {
		return _lastChallenge;
	}

	Score getLastScore() {
		return _lastScore;
	}

	int getLastStatus() {
		return _lastStatus;
	}

	private String getPersistedUserName() {
		final SharedPreferences preferences = getContext().getSharedPreferences(PREFERENCES_NAME, 0);
		return preferences.getString(PREFERENCES_ENTRY_USER_NAME, null);
	}

	private ScoreController getScoreController() {
		if (_scoreController == null) {
			_scoreController = new ScoreController(this);
		}
		return _scoreController;
	}

	protected Session getSession() {
		return _client.getSession();
	}

	private ValueStore getSessionUserValues() {
		if (_sessionUserValueStore == null) {
			_sessionUserValueStore = new ValueStore();
			_sessionUserValueStore.setValueSourceFactroy(this);
			_sessionUserValueStore.putValue(Constant.USER, getSession().getUser());
		}
		return _sessionUserValueStore;
	}

	public List<ValueStore> getSessionUserValueStores() {
		final List<ValueStore> result = new ArrayList<ValueStore>();
		final Session session = getSession();
		for (final WeakReference<ValueStore> weakValueStore : _valueStoreCache.values()) {
			final ValueStore valueStore = weakValueStore.get();
			if (valueStore == null) {
				continue;
			}
			final User user = valueStore.<User> getValue(Constant.USER);
			if (user.ownsSession(session)) {
				result.add(valueStore);
			}
		}
		if (_sessionUserValueStore != null) {
			result.add(_sessionUserValueStore);
		}
		return result;
	}

	public ValueSource getValueSourceForKeyInStore(final String key, final ValueStore valueStore) {
		if (containsKey(UserAgent.SUPPORTED_KEYS, key)) {
			return new UserAgent();
		} else if (containsKey(UserDetailsAgent.SUPPORTED_KEYS, key)) {
			return new UserDetailsAgent();
		} else if (containsKey(NumberAchievementsAgent.SUPPORTED_KEYS, key)) {
			return new NumberAchievementsAgent();
		} else if (containsKey(NewsAgent.SUPPORTED_KEYS, key)) {
			return new NewsAgent();
		} else if (containsKey(UserBuddiesAgent.SUPPORTED_KEYS, key)) {
			return new UserBuddiesAgent();
		}
		return null;
	}

	public boolean hasLoadedAchievements() {
		checkHasAwards();
		return getAchievementsEngine().hasLoadedAchievements();
	}

	public boolean isAwardAchieved(final String awardId) {
		final Achievement achievement = getAchievement(awardId); // checks made here
		return achievement.isAchieved();
	}

	public boolean isChallengeOngoing() {
		return _challengeOngoing;
	}

	public void loadAchievements(final Runnable continuation) {
		checkHasAwards();
		getAchievementsEngine().loadAchievements(continuation);
	}

	public void onGamePlayEnded(final Double scoreResult, final Integer mode) {
		final Game game = getSession().getGame();

		// NOTE: discuss introduction of Game.isValidMode(Integer mode)
		if (game.hasModes()) {
			if (mode == null) {
				throw new IllegalArgumentException("the game has modes, but no mode was passed");
			}
			final int minMode = game.getMinMode();
			final int maxMode = game.getMaxMode();
			if ((mode < minMode) || (mode >= maxMode)) {
				throw new IllegalArgumentException("mode out of range [" + minMode + "," + maxMode + "[");
			}
		}

		if (!game.hasModes() && (mode != null)) {
			throw new IllegalArgumentException("the game has no modes, but a mode was passed");
		}

		_lastStatus = OnScoreSubmitObserver.STATUS_UNDEFINED;
		_lastScore = new Score(scoreResult, null);
		_lastScore.setMode(mode);

		if (_challengeOngoing) {
			if (!_configuration.isFeatureEnabled(Configuration.Feature.CHALLENGE)) {
				throw new IllegalStateException(
						"we're in challenge mode, but the challenge feature is not enabled in the scoreloop.properties");
			}

			if (_lastChallenge.isCreated()) {
				_lastChallenge.setContenderScore(_lastScore);
			}

			if (_lastChallenge.isAccepted()) {
				_lastChallenge.setContestantScore(_lastScore);
			}

			getChallengeController().setChallenge(_lastChallenge);
			_challengeController.submitChallenge();
		} else {
			_lastChallenge = null;
			getScoreController().submitScore(_lastScore);
		}
	}

	public void onShortcut(final int textId) {
		if (textId == R.string.sl_home) {
			displayWithEmptyStack(createEntryScreenDescription());
		} else if (textId == R.string.sl_friends) {
			displayWithEmptyStack(createUserScreenDescription(null));
		} else if (textId == R.string.sl_market) {
			displayWithEmptyStack(createMarketScreenDescription(null));
		}
	}

	private void persistSessionUserName() {
		final User sessionUser = getSession().getUser();
		if (sessionUser.isAuthenticated()) {
			final SharedPreferences.Editor preferences = getContext().getSharedPreferences(PREFERENCES_NAME, 0).edit();
			preferences.putString(PREFERENCES_ENTRY_USER_NAME, sessionUser.getDisplayName());
			preferences.commit();
		}
	}

	public void requestControllerDidFail(final RequestController aRequestController, final Exception anException) {
		if ((aRequestController == _scoreController) || (aRequestController == _challengeController)) {
			_lastScore = null;
			_lastChallenge = null;
			_lastStatus = OnScoreSubmitObserver.STATUS_ERROR_NETWORK;
			_challengeOngoing = false;

			if (_onScoreSubmitObserver != null) {
				_onScoreSubmitObserver.onScoreSubmit(_lastStatus, anException);
			}
		}
	}

	public void requestControllerDidReceiveResponse(final RequestController aRequestController) {
		if ((aRequestController == _scoreController) || (aRequestController == _challengeController)) {
			if (_challengeOngoing) {
				_lastStatus = OnScoreSubmitObserver.STATUS_SUCCESS_CHALLENGE;
				_challengeOngoing = false;
			} else {
				_lastStatus = OnScoreSubmitObserver.STATUS_SUCCESS_SCORE;
			}

			if (_onScoreSubmitObserver != null) {
				_onScoreSubmitObserver.onScoreSubmit(_lastStatus, null);
			}
		}
	}

	public void run() {
		final String userName = getPersistedUserName();
		if (userName != null) {
			final String message = String.format(getContext().getString(R.string.sl_format_welcome_back), userName);
			BaseActivity.showToast(getContext(), message, null, Toast.LENGTH_LONG);
		}
	}

	public void screenManagerDidLeaveFramework(final ScreenManager manager) {
		_valueStoreCache.clear();
		_sessionUserValueStore = null;
		persistSessionUserName();
	}

	public boolean screenManagerWantsNewScreen(final ScreenManager screenManager, final ScreenDescription description,
			final ScreenDescription referenceDescription) {

		// we want a new screen, when the user or game of the description has changed

		final ValueStore contentValues = description.getScreenValues().getValue(Constant.CONTENT_VALUES);
		final ValueStore referenceContentValues = referenceDescription.getScreenValues().getValue(Constant.CONTENT_VALUES);

		final User user = contentValues.getValue(Constant.USER);
		final User referenceUser = referenceContentValues.getValue(Constant.USER);
		if (!user.equals(referenceUser)) {
			return true;
		}

		final Game game = contentValues.getValue(Constant.GAME);
		final Game referenceGame = referenceContentValues.getValue(Constant.GAME);
		return !game.equals(referenceGame);
	}

	public void screenManagerWillEnterFramework(final ScreenManager manager) {
		final String userName = getPersistedUserName();
		if (userName != null) {
			getSession().getUser().setLogin(userName);
		}
	}

	public void setOnCanStartGamePlayObserver(final OnCanStartGamePlayObserver observer) {
		_onCanStartGamePlayObserver = observer;
	}

	public void setOnScoreSubmitObserver(final OnScoreSubmitObserver observer) {
		_onScoreSubmitObserver = observer;
	}

	public void setOnStartGamePlayRequestObserver(final OnStartGamePlayRequestObserver observer) {
		_onStartGamePlayRequestObserver = observer;
	}

	private void showToastForAchievement(final Achievement achievement) {
		final String text = String.format(getContext().getString(R.string.sl_format_unlocked), achievement.getAward().getLocalizedTitle());
		BaseActivity.showToast(getContext(), text);
	}

	public void showWelcomeBackToast(final long delay) {
		if (delay < 0) {
			throw new IllegalArgumentException("delay to showWelcomeBackToast must be zero or postive");
		}
		_handler.postDelayed(this, delay);
	}

	public void startGamePlay(final Integer mode, final Challenge challenge) {
		if (_onStartGamePlayRequestObserver == null) {
			throw new IllegalStateException(
					"trying to start gameplay, but the callback is not set - did you call ScoreloopManagerSingleton.get().setOnStartGamePlayRequestObserver(...)?");
		}
		_challengeOngoing = true;
		_lastChallenge = challenge;
		_onStartGamePlayRequestObserver.onStartGamePlayRequest(mode);
	}

	public void submitAchievements(final Runnable continuation) {
		checkHasAwards();
		getAchievementsEngine().submitAchievements(continuation);
	}

	private void verifyManifest() {
		final PackageManager packageManager = getContext().getPackageManager();
		PackageInfo packageInfo = null;
		try {
			packageInfo = packageManager.getPackageInfo(getContext().getPackageName(), PackageManager.GET_ACTIVITIES
					| PackageManager.GET_PERMISSIONS);
		} catch (final NameNotFoundException e) {
			throw new VerifyException();
		}

		// verify activities
		final Map<String, Object> info = extractActivityInfo(packageInfo);

		final Checker required = new Checker("activity", info);
		required.add("com.scoreloop.client.android.ui.framework.ScreenActivity");
		required.add("com.scoreloop.client.android.ui.framework.TabsActivity");
		required.add("com.scoreloop.client.android.ui.component.market.MarketHeaderActivity");
		required.add("com.scoreloop.client.android.ui.component.market.MarketListActivity");
		required.add("com.scoreloop.client.android.ui.component.entry.EntryListActivity");
		required.add("com.scoreloop.client.android.ui.component.post.PostOverlayActivity", "theme", R.style.sl_dialog);
		required.add("com.scoreloop.client.android.ui.component.score.ScoreHeaderActivity");
		required.add("com.scoreloop.client.android.ui.component.score.ScoreListActivity");
		required.add("com.scoreloop.client.android.ui.component.user.UserAddBuddyListActivity");
		required.add("com.scoreloop.client.android.ui.component.user.UserHeaderActivity");
		required.add("com.scoreloop.client.android.ui.component.user.UserDetailListActivity");
		required.add("com.scoreloop.client.android.ui.component.user.UserListActivity");
		required.add("com.scoreloop.client.android.ui.component.game.GameDetailHeaderActivity");
		required.add("com.scoreloop.client.android.ui.component.game.GameDetailListActivity");
		required.add("com.scoreloop.client.android.ui.component.game.GameListActivity");
		required.add("com.scoreloop.client.android.ui.component.achievement.AchievementHeaderActivity", Feature.ACHIEVEMENT);
		required.add("com.scoreloop.client.android.ui.component.achievement.AchievementListActivity", Feature.ACHIEVEMENT);
		required.add("com.scoreloop.client.android.ui.component.news.NewsHeaderActivity", Feature.NEWS);
		required.add("com.scoreloop.client.android.ui.component.news.NewsListActivity", Feature.NEWS);
		required.add("com.scoreloop.client.android.ui.component.challenge.ChallengeHeaderActivity", Feature.CHALLENGE);
		required.add("com.scoreloop.client.android.ui.component.challenge.ChallengeListActivity", Feature.CHALLENGE);
		required.add("com.scoreloop.client.android.ui.component.challenge.ChallengeAcceptListActivity", Feature.CHALLENGE);
		required.add("com.scoreloop.client.android.ui.component.challenge.ChallengeCreateListActivity", Feature.CHALLENGE);
		required.add("com.scoreloop.client.android.ui.component.challenge.ChallengePaymentActivity", Feature.CHALLENGE);
		required.add("com.scoreloop.client.android.ui.component.profile.ProfileSettingsListActivity");
		required.add("com.scoreloop.client.android.ui.component.profile.ProfileSettingsPictureListActivity");
		required.reportRequired();

		final Checker optional = new Checker("activity", info);
		optional.add("com.scoreloop.client.android.ui.EntryScreenActivity");
		optional.add("com.scoreloop.client.android.ui.BuddiesScreenActivity");
		optional.add("com.scoreloop.client.android.ui.LeaderboardsScreenActivity");
		optional.add("com.scoreloop.client.android.ui.ChallengesScreenActivity", Feature.CHALLENGE);
		optional.add("com.scoreloop.client.android.ui.AchievementsScreenActivity", Feature.ACHIEVEMENT);
		optional.add("com.scoreloop.client.android.ui.SocialMarketScreenActivity");
		optional.add("com.scoreloop.client.android.ui.ProfileScreenActivity");
		optional.reportOptional();

		// verify permissions
		final Checker permission = new Checker("uses-permission", extractPermissionInfo(packageInfo));
		permission.add("android.permission.INTERNET");
		permission.add("android.permission.READ_PHONE_STATE");
		permission.add("android.permission.READ_CONTACTS", Feature.ADDRESS_BOOK);
		permission.reportRequired();

		required.check();
		optional.check();
		permission.check();
	}
}
