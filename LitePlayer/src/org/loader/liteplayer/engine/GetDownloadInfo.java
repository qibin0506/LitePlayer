package org.loader.liteplayer.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.loader.liteplayer.utils.Constants;

import android.os.Handler;
import android.os.Message;

/**
 * liteplayer by loader
 * @author qibin
 */
public class GetDownloadInfo {
	private static final int GET_MUSIC_SUCCESS = 5;
	private static final int GET_LRC_SUCCESS = 6;
	private static final int GET_MUSIC_FAILED = 7;
	private static final int GET_LRC_FAILED = 8;
	
	private static GetDownloadInfo sInstance;
	private ExecutorService mThreadPool;
	private Handler mHandler;
	private OnDownloadGettedListener mListener;

	public synchronized static GetDownloadInfo getInstance() {
		if (sInstance == null) sInstance = new GetDownloadInfo();
		return sInstance;
	}
	
	private GetDownloadInfo() {
		mThreadPool = Executors.newCachedThreadPool();
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case GET_MUSIC_SUCCESS:
					if(mListener != null) mListener.onMusic(msg.arg1, (String) msg.obj);
					break;
				case GET_MUSIC_FAILED:
					if(mListener != null) mListener.onMusic(-1, null);
					break;
				case GET_LRC_SUCCESS:
					if(mListener != null) mListener.onLrc(msg.arg1, (String) msg.obj);
					break;
				case GET_LRC_FAILED:
					if(mListener != null) mListener.onLrc(-1, null);
					break;
				}
			}
		};
	}
	
	public GetDownloadInfo setListener(OnDownloadGettedListener l) {
		mListener = l;
		return this;
	}
	
	public void parse(final int position, final String url) {
		mThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				String songId = getSongId(url);
				getDownloadUrl(position, songId);
				getLrcUrl(position, url);
			}
		});
	}
	
	private synchronized String getSongId(String url) {
		String temp = url.replaceFirst("/song/", "");
		if(!temp.contains("/")) return temp;
		return temp.substring(0, temp.indexOf("/"));
	}
	
	private synchronized void getLrcUrl(final int position, final String song) {
		String url = Constants.MUSIC_URL + song;
		
		try {
			Document doc = Jsoup.connect(url)
					  .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.22 Safari/537.36")
					  .timeout(3000).get();
			Elements elements = doc.select(".down-lrc-btn");
			if(elements.size() <= 0) throw new Exception();
			String json = elements.get(0).attr("data-lyricdata");
			JSONObject jsonObject = new JSONObject(json);
			String result = jsonObject.getString("href");
			
			Message msg = mHandler.obtainMessage(GET_LRC_SUCCESS, result);
			msg.arg1 = position;
			msg.sendToTarget();
		} catch (Exception e) {
			mHandler.sendEmptyMessage(GET_LRC_FAILED);
			e.printStackTrace();
		}
	}
	
	private synchronized void getDownloadUrl(final int position, final String songId) {
		String url = Constants.MUSIC_URL + "/song/" + songId + "/download?__o=%2Fsearch%2Fsong";
		try {
			Document doc = Jsoup.connect(url)
					  .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.22 Safari/537.36")
					  .timeout(60 * 1000).get();
			
//			Elements targetElements = doc.select(".btn.btn-h.btn-download.download.{\"ids\":\"" + songId + "\",\"type\":\"song\"}");
//			if(targetElements.size() > 0) {
//				Element targetElement = targetElements.get(0);
//				String result = targetElement.attr("href");
//				Message msg = mHandler.obtainMessage(GET_MUSIC_SUCCESS, result);
//				msg.arg1 = position;
//				msg.sendToTarget();
//				return;
//			}
//			
//			Elements elements = doc.select(".btn.btn-h.btn-download.download");
//			if(elements.size() <= 0) throw new Exception();
//			String result = elements.get(0).attr("href");
			
			Elements targetElements = doc.select("a[data-btndata]");
			if(targetElements.size() <= 0) throw new Exception();
			for(Element e : targetElements) {
				if(e.attr("href").contains(".mp3")) {
					String result = e.attr("href");
					Message msg = mHandler.obtainMessage(GET_MUSIC_SUCCESS, result);
					msg.arg1 = position;
					msg.sendToTarget();
					return;
				}
				
				if(e.attr("href").startsWith("/vip")) {
					targetElements.remove(e);
				}
			}
			
			if(targetElements.size() <= 0) throw new Exception();
			
			String result = targetElements.get(0).attr("href");
			Message msg = mHandler.obtainMessage(GET_MUSIC_SUCCESS, result);
			msg.arg1 = position;
			msg.sendToTarget();
		} catch (Exception e) {
			mHandler.sendEmptyMessage(GET_MUSIC_FAILED);
			e.printStackTrace();
		}
	}
	
	public interface OnDownloadGettedListener {
		public void onMusic(int position, String url);
		public void onLrc(int position, String url);
	}
}
