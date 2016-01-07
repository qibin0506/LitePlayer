package org.loader.liteplayer.utils;

import java.io.File;
import java.util.ArrayList;

import org.loader.liteplayer.application.App;
import org.loader.liteplayer.pojo.Music;

import android.os.Environment;

/**
 * liteplayer by loader
 * @author qibin
 */
public class MusicUtils {
	// 存放歌曲列表
	public static ArrayList<Music> sMusicList = new ArrayList<Music>();

	public static void initMusicList() {
		// 获取歌曲列表
		sMusicList.clear();
		sMusicList.addAll(LocalMusicUtils.queryMusic(getBaseDir()));
	}

	/**
	 * 获取内存卡根
	 * @return
	 */
	public static String getBaseDir() {
		String dir = null;
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED)) {
			dir = Environment.getExternalStorageDirectory() + File.separator;
		} else {
			dir = App.sContext.getFilesDir() + File.separator;
		}

		return dir;
	}

	/**
	 * 获取应用程序使用的本地目录
	 * @return
	 */
	public static String getAppLocalDir() {
		String dir = null;

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED)) {
			dir = Environment.getExternalStorageDirectory() + File.separator
					+ "liteplayer" + File.separator;
		} else {
			dir = App.sContext.getFilesDir() + File.separator + "liteplayer" + File.separator;
		}

		return mkdir(dir);
	}

	/**
	 * 获取音乐存放目录
	 * @return
	 */
	public static String getMusicDir() {
		String musicDir = getAppLocalDir() + "music" + File.separator;
		return mkdir(musicDir);
	}

	/**
	 * 获取歌词存放目录
	 * 
	 * @return
	 */
	public static String getLrcDir() {
		String lrcDir = getAppLocalDir() + "lrc" + File.separator;
		return mkdir(lrcDir);
	}

	/**
	 * 创建文件夹
	 * @param dir
	 * @return
	 */
	public static String mkdir(String dir) {
		File f = new File(dir);
		if (!f.exists()) {
			for (int i = 0; i < 5; i++) {
				if(f.mkdirs()) return dir;
			}
			return null;
		}
		
		return dir;
	}
}
