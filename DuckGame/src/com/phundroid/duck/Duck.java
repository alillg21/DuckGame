package com.phundroid.duck;

import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.opengl.texture.source.ITextureSource;
import org.anddev.andengine.opengl.texture.source.ResourceTextureSource;
import org.anddev.andengine.ui.activity.BaseSplashActivity;

import android.app.Activity;

public class Duck extends BaseSplashActivity {

	private static final float SPLASH_DURATION = 3.f;
    private static final float SPLASH_SCALE_FROM = 0.2f;
	
	        
	
	@Override
	protected Class<? extends Activity> getFollowUpActivity() {
		// TODO Auto-generated method stub
		return DuckGame.class;
	}

	@Override
	protected ScreenOrientation getScreenOrientation() {
		// TODO Auto-generated method stub
		return ScreenOrientation.LANDSCAPE;
	}

	@Override
	protected float getSplashDuration() {
		// TODO Auto-generated method stub
		return SPLASH_DURATION;
	}

	@Override
	protected float getSplashScaleFrom() {
		// TODO Auto-generated method stub
		return SPLASH_SCALE_FROM;
	}
	
	@Override
	protected ITextureSource onGetSplashTextureSource() {

		return new ResourceTextureSource(this, R.drawable.splash);
	}


	@Override

	public Scene onLoadScene() {
	    final Scene splashScene = super.onLoadScene();
	    splashScene.setBackground(new ColorBackground(0.f, 0.f, 0.f));
	    return splashScene;
	}
}
