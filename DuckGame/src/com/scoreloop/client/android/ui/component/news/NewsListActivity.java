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

package com.scoreloop.client.android.ui.component.news;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.phundroid.duck.R;
import com.scoreloop.client.android.core.addon.RSSItem;
import com.scoreloop.client.android.ui.component.base.CaptionListItem;
import com.scoreloop.client.android.ui.component.base.ComponentListActivity;
import com.scoreloop.client.android.ui.component.base.Constant;
import com.scoreloop.client.android.ui.component.entry.EntryListItem;
import com.scoreloop.client.android.ui.framework.BaseListAdapter;
import com.scoreloop.client.android.ui.framework.BaseListItem;
import com.scoreloop.client.android.ui.framework.ValueStore;

public class NewsListActivity extends ComponentListActivity<BaseListItem> {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new BaseListAdapter<EntryListItem>(this));
		
		addObservedContentKeys(Constant.NEWS_FEED);
	}

	@Override
	public void onListItemClick(final BaseListItem item) {
		if (item.getType() == Constant.LIST_ITEM_TYPE_NEWS) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(((NewsListItem) item).getItem().getLinkUrlString())));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// mark feed items as read when this activity gets paused
		final List<RSSItem> feed = getContentValues().getValue(Constant.NEWS_FEED);
		if (feed != null) {
			for (final RSSItem item : feed) {
				item.setHasPersistentReadFlag(true);
			}
		}
		getContentValues().putValue(Constant.NEWS_NUMBER_UNREAD_ITEMS, 0);
	}

	@Override
	public void onValueChanged(final ValueStore valueStore, final String key, final Object oldValue, final Object newValue) {
		final BaseListAdapter<BaseListItem> adapter = getBaseListAdapter();
		adapter.clear();

		final List<RSSItem> feed = getContentValues().getValue(Constant.NEWS_FEED);
		if ((feed != null) && (feed.size() > 0)) {
			for (final RSSItem item : feed) {
				adapter.add(new NewsListItem(NewsListActivity.this, item));
			}
		} else {
			adapter.add(new CaptionListItem(NewsListActivity.this, null, getString(R.string.sl_no_news)));
		}
	}

	@Override
	public void onValueSetDirty(final ValueStore valueStore, final String key) {
		retrieveContentValueFor(key, Constant.NEWS_FEED, ValueStore.RetrievalMode.NOT_OLDER_THAN, Constant.NEWS_FEED_REFRESH_TIME);
	}
}
