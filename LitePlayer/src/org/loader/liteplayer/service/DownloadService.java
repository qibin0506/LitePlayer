package org.loader.liteplayer.service;

import org.loader.liteplayer.R;
import org.loader.liteplayer.engine.Download;
import org.loader.liteplayer.utils.L;
import org.loader.liteplayer.utils.MusicUtils;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * liteplayer by loader
 * @author qibin
 */
public class DownloadService extends Service {
	private SparseArray<Download> mDownloads = new SparseArray<Download>();
	
	private RemoteViews mRemoteViews;
	
	public class DownloadBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new DownloadBinder();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public void download(final int id, final String url, final String name) {
		L.l("download", url);
		Download d = new Download(id, url, MusicUtils.getMusicDir() + name);
		d.setOnDownloadListener(mDownloadListener).start(false);
		mDownloads.put(id, d);
	}
	
	private void refreshRemoteView() {
		Notification notification = new Notification(android.R.drawable.stat_sys_download, 
				"", System.currentTimeMillis());
		mRemoteViews = new RemoteViews(getPackageName(), R.layout.download_remote_layout);  
	    notification.contentView = mRemoteViews;
	    
	    StringBuilder builder = new StringBuilder();
		for(int i=0,size=mDownloads.size();i<size;i++) {
			builder.append(mDownloads.get(mDownloads.keyAt(i)).getLocalFileName());
			builder.append("、");
		}
		
		mRemoteViews.setTextViewText(R.id.tv_download_name, 
				builder.substring(0, builder.lastIndexOf("、")));
	    
	    startForeground(R.drawable.ic_launcher, notification);
	}
	
	private void onDownloadComplete(int downloadId) {
		mDownloads.remove(downloadId);
		if(mDownloads.size() == 0) {
			stopForeground(true);
			return;
		}
		
		refreshRemoteView();
	}
	
	private void scanSDCard() {
		IntentFilter intentfilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentfilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentfilter.addDataScheme("file");
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
				Uri.parse("file://"+ MusicUtils.getMusicDir())));
	}
	
	private Download.OnDownloadListener mDownloadListener = 
			new Download.OnDownloadListener() {
		
		@Override
		public void onSuccess(int downloadId) {
			L.l("download", "success");
			Toast.makeText(DownloadService.this, 
					mDownloads.get(downloadId).getLocalFileName() + "下载完成",
					Toast.LENGTH_SHORT).show();
			onDownloadComplete(downloadId);
			scanSDCard();
		}
		
		@Override
		public void onStart(int downloadId, long fileSize) {
			L.l("download", "start");
			refreshRemoteView();
			Toast.makeText(DownloadService.this, "开始下载" + 
					mDownloads.get(downloadId).getLocalFileName(), Toast.LENGTH_SHORT).show();
		}
		
		@Override
		public void onPublish(int downloadId, long size) {
			L.l("download", "publish" + size);
		}
		
		@Override
		public void onPause(int downloadId) {
			L.l("download", "pause");
		}
		
		@Override
		public void onGoon(int downloadId, long localSize) {
			L.l("download", "goon");
		}
		
		@Override
		public void onError(int downloadId) {
			L.l("download", "error");
			Toast.makeText(DownloadService.this, 
					mDownloads.get(downloadId).getLocalFileName() + "下载失败",
					Toast.LENGTH_SHORT).show();
			onDownloadComplete(downloadId);
		}
		
		@Override
		public void onCancel(int downloadId) {
			L.l("download", "cancel");
			onDownloadComplete(downloadId);
		}
	};
}
