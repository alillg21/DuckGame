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

package com.scoreloop.client.android.ui.component.entry;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import com.phundroid.duck.R;
import com.scoreloop.client.android.ui.component.base.CaptionListItem;
import com.scoreloop.client.android.ui.component.base.ComponentListActivity;
import com.scoreloop.client.android.ui.component.base.Configuration;
import com.scoreloop.client.android.ui.component.base.Constant;
import com.scoreloop.client.android.ui.component.base.Factory;
import com.scoreloop.client.android.ui.component.base.StringFormatter;
import com.scoreloop.client.android.ui.framework.BaseListAdapter;
import com.scoreloop.client.android.ui.framework.BaseListItem;
import com.scoreloop.client.android.ui.framework.ValueStore;

public class EntryListActivity extends ComponentListActivity<BaseListItem> {

	class EntryListAdapter extends BaseListAdapter<BaseListItem> {

		public EntryListAdapter(final Context context) {
			super(context);
			final Resources res = context.getResources();
			final Configuration configuration = getConfiguration();

			add(new CaptionListItem(context, null, getGame().getName()));
			leaderboardsItem = new EntryListItem(EntryListActivity.this, res.getDrawable(R.drawable.sl_icon_leaderboards), context
					.getString(R.string.sl_leaderboards), null);
			add(leaderboardsItem);
			if (configuration.isFeatureEnabled(Configuration.Feature.ACHIEVEMENT)) {
				achievementsItem = new EntryListItem(EntryListActivity.this, res.getDrawable(R.drawable.sl_icon_achievements), context
						.getString(R.string.sl_achievements), null);
				add(achievementsItem);
			}
			if (configuration.isFeatureEnabled(Configuration.Feature.CHALLENGE)) {
				challengesItem = new EntryListItem(EntryListActivity.this, res.getDrawable(R.drawable.sl_icon_challenges), context
						.getString(R.string.sl_challenges), null);
				add(challengesItem);
			}
			if (configuration.isFeatureEnabled(Configuration.Feature.NEWS)) {
				newsItem = new EntryListItem(EntryListActivity.this, res.getDrawable(R.drawable.sl_icon_news_closed), context.getString(R.string.sl_news), null);
				add(newsItem);
			}
		}
	}

	private EntryListItem	achievementsItem;
	private EntryListItem	challengesItem;
	private EntryListItem	leaderboardsItem;
	private EntryListItem	newsItem;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new EntryListAdapter(this));
		
		addObservedContentKeys(Constant.NUMBER_ACHIEVEMENTS, Constant.NUMBER_CHALLENGES_WON, Constant.NEWS_NUMBER_UNREAD_ITEMS);
	}

	@Override
	public void onListItemClick(final BaseListItem item) {
		final Factory factory = getFactory();
		if (item == leaderboardsItem) {
			display(factory.createScoreScreenDescription(getGame(), null, null));
		} else if (item == challengesItem) {
			display(factory.createChallengeScreenDescription(getUser()));
		} else if (item == achievementsItem) {
			display(factory.createAchievementScreenDescription(getUser()));
		} else if (item == newsItem) {
			display(factory.createNewsScreenDescription());
		}
	}

	@Override
	public void onValueChanged(final ValueStore valueStore, final String key, final Object oldValue, final Object newValue) {
		final ValueStore contentValues = getContentValues();
		if (isValueChangedFor(key, Constant.NUMBER_ACHIEVEMENTS, oldValue, newValue)) {
			achievementsItem.setSubTitle(StringFormatter.getAchievementsSubTitle(this, contentValues, false));
			getBaseListAdapter().notifyDataSetChanged();
		} else if (isValueChangedFor(key, Constant.NUMBER_CHALLENGES_WON, oldValue, newValue)) {
			challengesItem.setSubTitle(StringFormatter.getChallengesSubTitle(this, contentValues));
			getBaseListAdapter().notifyDataSetChanged();
		} else if (isValueChangedFor(key, Constant.NEWS_NUMBER_UNREAD_ITEMS, oldValue, newValue)) {
			newsItem.setSubTitle(StringFormatter.getNewsSubTitle(this, contentValues));
			newsItem.setDrawable(StringFormatter.getNewsDrawable(this, contentValues, false));
			getBaseListAdapter().notifyDataSetChanged();
		}
	}

	@Override
	public void onValueSetDirty(final ValueStore valueStore, final String key) {
		final Configuration configuration = getConfiguration();

		if (configuration.isFeatureEnabled(Configuration.Feature.ACHIEVEMENT)) {
			retrieveContentValueFor(key, Constant.NUMBER_ACHIEVEMENTS, ValueStore.RetrievalMode.NOT_DIRTY, null);
		}

		if (configuration.isFeatureEnabled(Configuration.Feature.CHALLENGE)) {
			retrieveContentValueFor(key, Constant.NUMBER_CHALLENGES_WON, ValueStore.RetrievalMode.NOT_DIRTY, null);
		}

		if (configuration.isFeatureEnabled(Configuration.Feature.NEWS)) {
			retrieveContentValueFor(key, Constant.NEWS_NUMBER_UNREAD_ITEMS, ValueStore.RetrievalMode.NOT_OLDER_THAN,
					Constant.NEWS_FEED_REFRESH_TIME);
		}
	}
}
