package org.loader.liteplayer;

import org.loader.liteplayer.service.DownloadService;
import org.loader.liteplayer.service.PlayService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

/**
 * liteplayer by loader
 * @author qibin
 */
public class SplashActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// no title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 全屏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.splash_layout);
		// 开启播放和下载服务
		startService(new Intent(this, PlayService.class));
		startService(new Intent(this, DownloadService.class));
		
		// 2s跳转到主界面
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				startActivity(new Intent(SplashActivity.this, MainActivity.class));
				finish();
			}
		}, 2000);
	}
}
