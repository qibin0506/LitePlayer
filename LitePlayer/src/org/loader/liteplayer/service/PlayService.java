package org.loader.liteplayer.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.loader.liteplayer.utils.Constants;
import org.loader.liteplayer.utils.L;
import org.loader.liteplayer.utils.MusicUtils;
import org.loader.liteplayer.utils.SpUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

/**
 * liteplayer by loader
 * 音乐播放服务
 * @author qibin
 */
public class PlayService extends Service implements MediaPlayer.OnCompletionListener  {
	
	private SensorManager mSensorManager;
	
	private MediaPlayer mPlayer;
	private OnMusicEventListener mListener;
	private int mPlayingPosition; // 当前正在播放
	
	private boolean isShaking;
	
	private ExecutorService mProgressUpdatedListener = Executors.newSingleThreadExecutor();
	
	public class PlayBinder extends Binder {
		public PlayService getService() {
			return PlayService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		mSensorManager.registerListener(mSensorEventListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		return new PlayBinder();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		MusicUtils.initMusicList();
		mPlayingPosition = (Integer) SpUtils.get(this, Constants.PLAY_POS, 0);
		
		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(this);
		// 开始更新进度的线程
		mProgressUpdatedListener.execute(mPublishProgressRunnable);
//		mPlayer.setOnBufferingUpdateListener(mBufferUpdateListener);
	}
	
//	private OnBufferingUpdateListener mBufferUpdateListener = 
//			new OnBufferingUpdateListener() {
//		@Override
//		public void onBufferingUpdate(MediaPlayer mp, int percent) {
//			L.l("percent", percent);
//			if(mListener != null) mListener.onPublish(percent);
//		}
//	};
	
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			if(isShaking) return;
			
			if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
				float[] values = event.values;
				if (Math.abs(values[0]) > 8 && Math.abs(values[1]) > 8
						&& Math.abs(values[2]) > 8) {
					isShaking = true;
					next();
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							isShaking = false;
						}
					}, 200);
				}
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
	};
	
	/**
	 * 更新进度的线程
	 */
	private Runnable mPublishProgressRunnable = new Runnable() {
		@Override
		public void run() {
			for(;;) {
				if(mPlayer != null && mPlayer.isPlaying() && 
						mListener != null) {
					mListener.onPublish(mPlayer.getCurrentPosition());
				}
				
				SystemClock.sleep(200);
			}
		}
	};
	
	/**
	 * 设置回调
	 * @param l
	 */
	public void setOnMusicEventListener(OnMusicEventListener l) {
		mListener = l;
	}
	
	/**
	 * 播放
	 * @param position 音乐列表的位置
	 * @return 当前播放的位置
	 */
	public int play(int position) {
		if(position < 0) position = 0;
		if(position >= MusicUtils.sMusicList.size()) position = MusicUtils.sMusicList.size() - 1;
		
		try {
			mPlayer.reset();
			mPlayer.setDataSource(MusicUtils.sMusicList.get(position).getUri());
			mPlayer.prepare();
			
			start();
			if(mListener != null) mListener.onChange(position);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mPlayingPosition = position;
		SpUtils.put(Constants.PLAY_POS, mPlayingPosition);
		return mPlayingPosition;
	}
	
	/**
	 * 继续播放
	 * @return 当前播放的位置 默认为0
	 */
	public int resume() {
		if(isPlaying()) return -1;
		mPlayer.start();

		return mPlayingPosition;
	}
	
	/**
	 * 暂停播放
	 * @return 当前播放的位置
	 */
	public int pause() {
		if(!isPlaying()) return -1;
		mPlayer.pause();
		
		return mPlayingPosition;
	}
	
	/**
	 * 下一曲
	 * @return 当前播放的位置
	 */
	public int next() {
		if(mPlayingPosition >= MusicUtils.sMusicList.size() - 1) {
			return play(0);
		}
		
		return play(mPlayingPosition + 1);
	}
	
	/**
	 * 上一曲
	 * @return 当前播放的位置
	 */
	public int pre() {
		if(mPlayingPosition <= 0) {
			return play(MusicUtils.sMusicList.size() - 1);
		}
		
		return play(mPlayingPosition - 1);
	}
	
	/**
	 * 是否正在播放
	 * @return
	 */
	public boolean isPlaying() {
		return mPlayer != null && mPlayer.isPlaying(); 
	}
	
	/**
	 * 获取正在播放的位置
	 * @return
	 */
	public int getPlayingPosition() {
		return mPlayingPosition;
	}
	
	/**
	 * 获取当前正在播放音乐的总时长
	 * @return
	 */
	public int getDuration() {
		if(!isPlaying()) return 0;
		return mPlayer.getDuration();
	}
	
	public void seek(int msec) {
		if(!isPlaying()) return;
		mPlayer.seekTo(msec);
	}
	
	/**
	 * 开始播放
	 */
	private void start() {
		mPlayer.start();
	}
	
	/**
	 * 音乐播放完毕 自动下一曲
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		next();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		L.l("play service", "unbind");
		// 置空Listener
		// 如果不置空，会出现的问题
		// 即便是activity解绑了service， service依然可以回调
		// 原因是service已经持有了activity的引用
		// 这样会导致一个问题:
		// activity在销毁后不能被回收， so 会造成内存泄漏
		mListener = null; 
		mSensorManager.unregisterListener(mSensorEventListener);
		return true;
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		if(mListener != null) mListener.onChange(mPlayingPosition);
	}
	
	@Override
	public void onDestroy() {
		release();
		mSensorManager.unregisterListener(mSensorEventListener);
		super.onDestroy();
	}

	/**
	 * 服务销毁时，释放各种控件
	 */
	private void release() {
		if(!mProgressUpdatedListener.isShutdown()) mProgressUpdatedListener.shutdownNow();
		mProgressUpdatedListener = null;
		
		if(mPlayer != null) mPlayer.release();
		mPlayer = null;
	}
	
	/**
	 * 音乐播放回调接口
	 * @author qibin
	 */
	public interface OnMusicEventListener {
		public void onPublish(int percent);
		public void onChange(int position);
	}
}

/**
public class PlayService extends MusicService implements OnClickListener {
	
	public class PlayBinder extends Binder {
		public PlayService getService() {
			return PlayService.this;
		}
	}
	
//	private WindowManager mWindowManager;
//	private WindowManager.LayoutParams mLayoutParams;
//	private WidgetTouchLayout mLayout;
//	
//	private Timer mTimer;
	
	@Override
	public IBinder onBind(Intent intent) {
		return new PlayBinder();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
//		setupWindowManager();
//		mLayout = (WidgetTouchLayout) View.inflate(this, R.layout.widget_layout, null);
//		mLayout.setOnWidgetTouchListener(new OnWidgetTouchListener() {
//			@Override
//			public boolean onTouch(int distanceX, int distanceY) {
//				mLayoutParams.x += distanceX;
//				mLayoutParams.y += distanceY;
//				mWindowManager.updateViewLayout(mLayout, mLayoutParams);
//				
//				return true;
//			}
//		});
//		
//		mLayout.findViewById(R.id.iv_widget_normal).setOnClickListener(this);
//		mWindowManager.addView(mLayout, mLayoutParams);
//		
//		if(mTimer == null) mTimer = new Timer();
//		mTimer.scheduleAtFixedRate(mCheckTimeTask, 0, 500);
	}

//	private void setupWindowManager() {
//		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//		mLayoutParams = new WindowManager.LayoutParams();
//		mLayoutParams.type = LayoutParams.TYPE_PHONE;
//		mLayoutParams.format = PixelFormat.RGBA_8888;
//		mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
//				|LayoutParams.FLAG_NOT_TOUCH_MODAL;
//		
//		mLayoutParams.gravity = Gravity.TOP|Gravity.LEFT;
//		mLayoutParams.width = LayoutParams.WRAP_CONTENT;
//		mLayoutParams.height = LayoutParams.WRAP_CONTENT;
//		mLayoutParams.x = (int) (App.sScreenWidth * 0.8);
//		mLayoutParams.y = (int) (App.sScreenHeight * 0.6);
//	}
//	
//	private boolean isLauncher() {
//		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//		// 获取栈顶的package
//		String topPackage = activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
//		System.out.println(topPackage);
//		return topPackage.equalsIgnoreCase("com.android.launcher");
//	}
//	
//	private void setLayoutVisible(final boolean visible) {
//		final int v = visible ? View.VISIBLE : View.GONE;
//		if(mLayout.getVisibility() == v) return;
//
//		new Handler(PlayService.this.getMainLooper()).post(new Runnable() {
//			@Override
//			public void run() {
//				mLayout.setVisibility(v);
//			}
//		});
//	}
//	
//	private TimerTask mCheckTimeTask = new TimerTask() {
//		@Override
//		public void run() {
//			setLayoutVisible(isLauncher());
//		}
//	};
//
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_widget_normal:
			System.out.println("normal model...");
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		System.out.println("destory");
//		mTimer.cancel();
//		mTimer = null;
		super.onDestroy();
	}
}
*/