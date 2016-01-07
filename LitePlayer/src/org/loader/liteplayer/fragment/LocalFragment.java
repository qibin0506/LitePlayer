package org.loader.liteplayer.fragment;

import java.io.File;

import org.loader.liteplayer.MainActivity;
import org.loader.liteplayer.PlayActivity;
import org.loader.liteplayer.R;
import org.loader.liteplayer.adapter.MusicListAdapter;
import org.loader.liteplayer.adapter.MusicListAdapter.OnMoreClickListener;
import org.loader.liteplayer.pojo.Music;
import org.loader.liteplayer.utils.ImageTools;
import org.loader.liteplayer.utils.L;
import org.loader.liteplayer.utils.MusicIconLoader;
import org.loader.liteplayer.utils.MusicUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * liteplayer by loader
 * @author qibin
 */
public class LocalFragment extends Fragment implements OnClickListener {

	private ListView mMusicListView;
	private ImageView mMusicIcon;
	private TextView mMusicTitle;
	private TextView mMusicArtist;

	private ImageView mPreImageView;
	private ImageView mPlayImageView;
	private ImageView mNextImageView;

	private SeekBar mMusicProgress;
	
	private PopupWindow mPopupWindow;
	private TextView mPopView4Delete;
	private TextView mPopView4Cancel;

	private MusicListAdapter mMusicListAdapter = new MusicListAdapter();

	private MainActivity mActivity;
	
	private boolean isPause;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.local_music_layout, null);
		setupViews(layout);

		return layout;
	}
	
	/**
	 * view创建完毕 回调通知activity绑定服务
	 */
	@Override
	public void onStart() {
		super.onStart();
		L.l("fragment", "onViewCreated");
		mActivity.allowBindService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		isPause = false;
	}
	
	@Override
	public void onPause() {
		isPause = true;
		super.onPause();
	}

	/**
	 * stop时， 回调通知activity解除绑定服务
	 */
	@Override
	public void onStop() {
		super.onStop();
		L.l("fragment", "onDestroyView");
		mActivity.allowUnbindService();
	}

	private void setupViews(View layout) {
		mMusicListView = (ListView) layout.findViewById(R.id.lv_music_list);
		mMusicIcon = (ImageView) layout.findViewById(R.id.iv_play_icon);
		mMusicTitle = (TextView) layout.findViewById(R.id.tv_play_title);
		mMusicArtist = (TextView) layout.findViewById(R.id.tv_play_artist);

		mPreImageView = (ImageView) layout.findViewById(R.id.iv_pre);
		mPlayImageView = (ImageView) layout.findViewById(R.id.iv_play);
		mNextImageView = (ImageView) layout.findViewById(R.id.iv_next);

		mMusicProgress = (SeekBar) layout.findViewById(R.id.play_progress);
		mMusicListAdapter.setOnMoreClickListener(mOnMoreClickListener);
		mMusicListView.setAdapter(mMusicListAdapter);
		mMusicListView.setOnItemClickListener(mMusicItemClickListener);

		mMusicIcon.setOnClickListener(this);
		mPreImageView.setOnClickListener(this);
		mPlayImageView.setOnClickListener(this);
		mNextImageView.setOnClickListener(this);
	}
	
	private OnMoreClickListener mOnMoreClickListener = new OnMoreClickListener() {
		@Override
		public void onMoreClick(int position) {
			if(mPopupWindow != null && mPopupWindow.isShowing()) return;
			
			mActivity.onPopupWindowShown();
			if(mPopupWindow == null) {
				ViewGroup popView = (ViewGroup) View.inflate(mActivity, R.layout.more_pop_layout, null);
				mPopView4Delete = (TextView) popView.findViewById(R.id.tv_more_delete);
				mPopView4Cancel = (TextView) popView.findViewById(R.id.tv_more_cancel);
				
				mPopView4Cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mPopupWindow.dismiss();
					}
				});
				
				mPopupWindow = new PopupWindow(popView, LayoutParams.MATCH_PARENT, 
						LayoutParams.WRAP_CONTENT);
				mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				mPopupWindow.setAnimationStyle(R.style.popwin_anim);
				mPopupWindow.setFocusable(true);
				mPopupWindow.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss() {
						mActivity.onPopupWindowDismiss();
					}
				});
			}
			final int pos = position;
			mPopView4Delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mPopupWindow != null && mPopupWindow.isShowing()) mPopupWindow.dismiss();
					deleteMusic(pos);
				}
			});
			
			mPopupWindow.showAtLocation(mActivity.getWindow().getDecorView(), 
					Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
		}
	};
	
	private void deleteMusic(int position) {
		final int pos = position;
		String musicName = ((Music) mMusicListAdapter.getItem(position)).getTitle();
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle("删除该条目");
		builder.setMessage("确认要删除歌曲《"+ musicName +"》吗?");
		builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Music music = MusicUtils.sMusicList.remove(pos);
				mMusicListAdapter.notifyDataSetChanged();
				if(new File(music.getUri()).delete()) {
					scanSDCard();
				}
			}
		});
		builder.setNegativeButton("取消", null);
		builder.create().show();
	}
	
	private OnItemClickListener mMusicItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			play(position);
		}
	};
	
	private void scanSDCard() {
		IntentFilter intentfilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentfilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentfilter.addDataScheme("file");
		mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
				Uri.parse("file://"+ MusicUtils.getMusicDir())));
	}

	/**
	 * 播放时高亮当前播放条目
	 * 
	 * @param position
	 */
	private void onItemPlay(int position) {
		// if(!mActivity.getPlayService().isPlaying()) return;
		mMusicListView.smoothScrollToPosition(position);
		// 获取上次播放的位置
		int prePlayingPosition = mMusicListAdapter.getPlayingPosition();
		// 如果上次播放的位置在可视区域内
		// 则手动设置invisible
		if (prePlayingPosition >= mMusicListView.getFirstVisiblePosition()
				&& prePlayingPosition <= mMusicListView.getLastVisiblePosition()) {
			int preItem = prePlayingPosition - mMusicListView.getFirstVisiblePosition();
			((ViewGroup) mMusicListView.getChildAt(preItem)).getChildAt(0).setVisibility(
					View.INVISIBLE);
		}

		// 设置新的播放位置
		mMusicListAdapter.setPlayingPosition(position);
		
		// 如果新的播放位置不在可视区域
		// 则直接返回
		if (mMusicListView.getLastVisiblePosition() < position
				|| mMusicListView.getFirstVisiblePosition() > position)
			return;

		// 如果在可视区域
		// 手动设置改item visible
		int currentItem = position - mMusicListView.getFirstVisiblePosition();
		((ViewGroup) mMusicListView.getChildAt(currentItem)).getChildAt(0)
				.setVisibility(View.VISIBLE);
	}

	/**
	 * 播放音乐item
	 * @param position
	 */
	private void play(int position) {
		int pos = mActivity.getPlayService().play(position);
		onPlay(pos);
	}

	/**
	 * 播放时，更新控制面板
	 * 
	 * @param position
	 */
	public void onPlay(int position) {
		if (MusicUtils.sMusicList.isEmpty() || position < 0) return;

		mMusicProgress.setMax(mActivity.getPlayService().getDuration());
		onItemPlay(position);

		Music music = MusicUtils.sMusicList.get(position);
		Bitmap icon = MusicIconLoader.getInstance().load(music.getImage());
		mMusicIcon.setImageBitmap(icon == null ? ImageTools
				.scaleBitmap(R.drawable.ic_launcher) : ImageTools
				.scaleBitmap(icon));
		mMusicTitle.setText(music.getTitle());
		mMusicArtist.setText(music.getArtist());

		if (mActivity.getPlayService().isPlaying()) {
			mPlayImageView.setImageResource(android.R.drawable.ic_media_pause);
		} else {
			mPlayImageView.setImageResource(android.R.drawable.ic_media_play);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_play_icon:
			startActivity(new Intent(mActivity, PlayActivity.class));
			break;
		case R.id.iv_play:
			if (mActivity.getPlayService().isPlaying()) {
				mActivity.getPlayService().pause(); // 暂停
				mPlayImageView.setImageResource(android.R.drawable.ic_media_play);
			} else {
				onPlay(mActivity.getPlayService().resume()); // 播放
			}
			break;
		case R.id.iv_next:
			mActivity.getPlayService().next(); // 下一曲
			break;
		case R.id.iv_pre:
			mActivity.getPlayService().pre(); // 上一曲
			break;
		}
	}

	/**
	 * 设置进度条的进度(SeekBar)
	 * @param progress
	 */
	public void setProgress(int progress) {
		if(isPause) return;
		mMusicProgress.setProgress(progress);
	}
	
	public void onMusicListChanged() {
		mMusicListAdapter.notifyDataSetChanged();
	}
}
