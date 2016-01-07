package org.loader.liteplayer.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

/**
 * liteplayer by loader
 * @author qibin
 */
public class MusicIconLoader {
	private static MusicIconLoader sInstance;
	
	private LruCache<String, Bitmap> mCache;
	
	// 获取MusicIconLoader的实例
	public synchronized static MusicIconLoader getInstance() {
		if(sInstance == null) sInstance = new MusicIconLoader();
		return sInstance;
	}
	
	// 构造方法， 初始化LruCache
	private MusicIconLoader() {
		int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);
		mCache = new LruCache<String, Bitmap>(maxSize) {
			protected int sizeOf(String key, Bitmap value) {
//				return value.getByteCount();
				return value.getRowBytes() * value.getHeight();
			}
		};
	}
	
	// 根据路径获取图片
	public Bitmap load(final String uri) {
		if(uri == null) return null;
		
		final String key = Encrypt.md5(uri);
		Bitmap bmp = getFromCache(key);
		
		if(bmp != null) return bmp;
		
		bmp = BitmapFactory.decodeFile(uri);
		addToCache(key, bmp);
		
		return bmp;
	}
	
	// 从内存中获取图片
	private Bitmap getFromCache(final String key) {
		return mCache.get(key);
	}
	
	// 将图片缓存到内存中
	private void addToCache(final String key, final Bitmap bmp) {
		if(getFromCache(key) == null && key != null && bmp != null) mCache.put(key, bmp);
	}
}
