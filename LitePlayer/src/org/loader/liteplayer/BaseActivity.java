package org.loader.liteplayer;

import org.loader.liteplayer.service.DownloadService;
import org.loader.liteplayer.service.PlayService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;

/**
 * liteplayer by loader
 * @author qibin
 */
public abstract class BaseActivity extends FragmentActivity {
	protected PlayService mPlayService;
	protected DownloadService mDownloadService;
	
	private ServiceConnection mPlayServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mPlayService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mPlayService = ((PlayService.PlayBinder) service).getService();
			mPlayService.setOnMusicEventListener(mMusicEventListener);
			onChange(mPlayService.getPlayingPosition());
		}
	};
	
	private ServiceConnection mDownloadServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mDownloadService = null;			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mDownloadService = ((DownloadService.DownloadBinder) service).getService();			
		}
	};
	
	/**
	 * 音乐播放服务回调接口的实现类
	 */
	private PlayService.OnMusicEventListener mMusicEventListener = 
			new PlayService.OnMusicEventListener() {
		@Override
		public void onPublish(int progress) {
			BaseActivity.this.onPublish(progress);
		}

		@Override
		public void onChange(int position) {
			BaseActivity.this.onChange(position);
		}
	};
	
	/**
	 * Fragment的view加载完成后回调
	 */
	public void allowBindService() {
		bindService(new Intent(this, PlayService.class), mPlayServiceConnection,
				Context.BIND_AUTO_CREATE);
	}
	
	/**
	 * fragment的view消失后回调
	 */
	public void allowUnbindService() {
		unbindService(mPlayServiceConnection);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(this, DownloadService.class), mDownloadServiceConnection,Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mDownloadServiceConnection);
		super.onDestroy();
	}
	
	public DownloadService getDownloadService() {
		return mDownloadService;
	}
	
	/**
	 * 更新进度
	 * @param progress 进度
	 */
	public abstract void onPublish(int progress);
	/**
	 * 切换歌曲
	 * @param position 歌曲在list中的位置
	 */
	public abstract void onChange(int position);
}
