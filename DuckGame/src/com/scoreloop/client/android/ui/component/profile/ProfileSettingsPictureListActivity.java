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

package com.scoreloop.client.android.ui.component.profile;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.phundroid.duck.R;
import com.scoreloop.client.android.core.controller.RequestController;
import com.scoreloop.client.android.core.controller.SocialProviderController;
import com.scoreloop.client.android.core.controller.SocialProviderControllerObserver;
import com.scoreloop.client.android.core.controller.UserController;
import com.scoreloop.client.android.core.controller.UserControllerObserver;
import com.scoreloop.client.android.core.model.ImageSource;
import com.scoreloop.client.android.core.model.SocialProvider;
import com.scoreloop.client.android.core.model.User;
import com.scoreloop.client.android.ui.component.base.CaptionListItem;
import com.scoreloop.client.android.ui.component.base.ComponentListActivity;
import com.scoreloop.client.android.ui.component.base.Constant;
import com.scoreloop.client.android.ui.framework.BaseListAdapter;
import com.scoreloop.client.android.ui.framework.BaseListItem;
import com.scoreloop.client.android.ui.util.Base64;

public class ProfileSettingsPictureListActivity extends ComponentListActivity<BaseListItem> implements UserControllerObserver,
		SocialProviderControllerObserver {

	class PictureListAdapter extends BaseListAdapter<BaseListItem> {
		public PictureListAdapter(final Context context) {
			super(context);
			// screen contains static profile list items
			add(new CaptionListItem(context, null, getString(R.string.sl_change_picture)));
			add(_deviceLibraryItem);
			add(_facebookItem);
			add(_twitterItem);
			add(_myspaceItem);
			add(_setDefaultItem);
		}
	}

	private static final int		CROP_PICTURE	= 0x2;
	private static final int		PICK_PICTURE	= 0x1;

	private Bitmap					_bitmap;
	private Runnable				_continuation;
	private ProfilePictureListItem	_deviceLibraryItem;
	private ProfilePictureListItem	_facebookItem;
	private ProfilePictureListItem	_myspaceItem;
	private Uri						_selectedImageUri;
	private ProfilePictureListItem	_setDefaultItem;
	private ProfilePictureListItem	_twitterItem;
	private User					_user;
	private UserController			_userController;

	private void cropPhoto() {
		final Intent intent = new Intent();
		intent.setData(_selectedImageUri);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.setClassName("com.android.gallery", "com.android.camera.CropImage");
		intent.putExtra("return-data", true);
		intent.putExtra("outputX", 64);
		intent.putExtra("outputY", 64);
		intent.putExtra("scale", true);
		try {
			startActivityForResult(intent, CROP_PICTURE);
		} catch (final Exception e) {
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if ((requestCode == PICK_PICTURE) && (data != null)) {
			_selectedImageUri = data.getData();
			if (_selectedImageUri != null) {
				getHandler().post(new Runnable() {
					public void run() {
						cropPhoto();
					}
				});
			}
		} else if ((requestCode == CROP_PICTURE) && (data != null) && (data.getExtras() != null)) {
			_bitmap = (Bitmap) data.getExtras().get("data");
		} else if (_selectedImageUri != null) {
			try {
				System.gc();
				Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), _selectedImageUri);
				if(bmp != null) {
					_bitmap = Bitmap.createScaledBitmap(bmp, 72, 72, true);
				}
			} catch (final FileNotFoundException e) {
			} catch (final IOException e) {
			}
		}
		if (_bitmap != null) {
			getHandler().post(new Runnable() {
				public void run() {
					_user.setImageSource(ImageSource.IMAGE_SOURCE_SCORELOOP);
					_user.setMimeType("image/png");
					final ByteArrayOutputStream out = new ByteArrayOutputStream();
					_bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
					_user.setImageData(Base64.encodeBytes(out.toByteArray()));
					showSpinnerFor(_userController);
					_userController.submitUser();
				}
			});
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Resources res = getResources();
		_deviceLibraryItem = new ProfilePictureListItem(this, res.getDrawable(R.drawable.sl_icon_device),
				getString(R.string.sl_device_library));
		_facebookItem = new ProfilePictureListItem(this, res.getDrawable(R.drawable.sl_icon_facebook), getString(R.string.sl_facebook));
		_twitterItem = new ProfilePictureListItem(this, res.getDrawable(R.drawable.sl_icon_twitter), getString(R.string.sl_twitter));
		_myspaceItem = new ProfilePictureListItem(this, res.getDrawable(R.drawable.sl_icon_myspace), getString(R.string.sl_myspace));
		_setDefaultItem = new ProfilePictureListItem(this, res.getDrawable(R.drawable.sl_icon_user), getString(R.string.sl_set_default));
		setListAdapter(new PictureListAdapter(this));
		_user = getUser();
		_userController = new UserController(this);
		_userController.setUser(_user);
	}

	@Override
	public void onListItemClick(final BaseListItem item) {
		if (item == _deviceLibraryItem) {
			pickDeviceLibraryPicture();
		} else if (item == _facebookItem) {
			withConnectedProvider(SocialProvider.FACEBOOK_IDENTIFIER, new Runnable() {
				public void run() {
					pickFacebookPicture();
				}
			});
		} else if (item == _twitterItem) {
			withConnectedProvider(SocialProvider.TWITTER_IDENTIFIER, new Runnable() {
				public void run() {
					pickTwitterPicture();
				}
			});
		} else if (item == _myspaceItem) {
			withConnectedProvider(SocialProvider.MYSPACE_IDENTIFIER, new Runnable() {
				public void run() {
					pickMyspacePicture();
				}
			});
		} else if (item == _setDefaultItem) {
			pickDefaultPicture();
		}
	}

	private void pickDefaultPicture() {
		_user.setImageSource(ImageSource.IMAGE_SOURCE_DEFAULT);
		_user.setMimeType(null);
		_user.setImageData(null);
		showSpinnerFor(_userController);
		_userController.submitUser();
	}

	private void pickDeviceLibraryPicture() {
		final Intent intent = new Intent();
		_bitmap = null;
		_selectedImageUri = null;
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.putExtra("windowTitle", getString(R.string.sl_choose_photo));
		try {
			startActivityForResult(intent, PICK_PICTURE);
		} catch (final Exception e) {
		}
	}

	private void pickFacebookPicture() {
		_user.setImageSource(SocialProvider.getSocialProviderForIdentifier(SocialProvider.FACEBOOK_IDENTIFIER));
		_user.setMimeType(null);
		_user.setImageData(null);
		showSpinnerFor(_userController);
		_userController.submitUser();
	}

	private void pickMyspacePicture() {
		_user.setImageSource(SocialProvider.getSocialProviderForIdentifier(SocialProvider.MYSPACE_IDENTIFIER));
		_user.setMimeType(null);
		_user.setImageData(null);
		showSpinnerFor(_userController);
		_userController.submitUser();
	}

	private void pickTwitterPicture() {
		_user.setImageSource(SocialProvider.getSocialProviderForIdentifier(SocialProvider.TWITTER_IDENTIFIER));
		_user.setMimeType(null);
		_user.setImageData(null);
		showSpinnerFor(_userController);
		_userController.submitUser();
	}

	@Override
	public void requestControllerDidFailSafe(final RequestController controller, final Exception anException) {
		hideSpinnerFor(controller);
	}

	@Override
	public void requestControllerDidReceiveResponseSafe(final RequestController controller) {
		getContentValues().putValue(Constant.USER_IMAGE_URL, _user.getImageUrl());
		hideSpinnerFor(controller);
	}

	public void socialProviderControllerDidCancel(final SocialProviderController controller) {
		hideSpinnerFor(controller);
	}

	public void socialProviderControllerDidEnterInvalidCredentials(final SocialProviderController controller) {
		socialProviderControllerDidFail(controller, new RuntimeException("Invalid Credentials"));
	}

	public void socialProviderControllerDidFail(final SocialProviderController controller, final Throwable error) {
		hideSpinnerFor(controller);
		showToast(String.format(getString(R.string.sl_format_connect_failed), controller.getSocialProvider().getName()));
	}

	public void socialProviderControllerDidSucceed(final SocialProviderController controller) {
		hideSpinnerFor(controller);
		if (!isPaused() && (_continuation != null)) {
			_continuation.run();
		}
	}

	public void userControllerDidFailOnEmailAlreadyTaken(final UserController controller) {
		hideSpinnerFor(controller);
	}

	public void userControllerDidFailOnInvalidEmailFormat(final UserController controller) {
		hideSpinnerFor(controller);
	}

	public void userControllerDidFailOnUsernameAlreadyTaken(final UserController controller) {
		hideSpinnerFor(controller);
	}

	private void withConnectedProvider(final String socialProviderIdentifier, final Runnable runnable) {
		final SocialProvider socialProvider = SocialProvider.getSocialProviderForIdentifier(socialProviderIdentifier);
		if (socialProvider.isUserConnected(getSessionUser())) {
			runnable.run();
		} else {
			final SocialProviderController socialProviderController = SocialProviderController.getSocialProviderController(getSession(),
					this, socialProvider);
			_continuation = runnable;
			showSpinnerFor(socialProviderController);
			socialProviderController.connect(this);
		}
	}
}
