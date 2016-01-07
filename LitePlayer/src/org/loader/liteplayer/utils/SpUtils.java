package org.loader.liteplayer.utils;

import org.loader.liteplayer.application.App;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {
	public static void put(final String key, final Object value) {
		SharedPreferences sp = App.sContext.getSharedPreferences(Constants.SP_NAME, 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		
		if(value instanceof Integer) {
			editor.putInt(key, (Integer) value);
		}else if(value instanceof Float) {
			editor.putFloat(key, (Float) value);
		}else if(value instanceof Boolean) {
			editor.putBoolean(key, (Boolean) value);
		}else if(value instanceof Long) {
			editor.putLong(key, (Long) value);
		}else {
			editor.putString(key, (String) value);
		}
		
		editor.commit();
	}
	
	public static Object get(Context context, String key, Object defaultObject) {
		SharedPreferences sp = App.sContext.getSharedPreferences(Constants.SP_NAME,
				Context.MODE_PRIVATE);

		if (defaultObject instanceof String) {
			return sp.getString(key, (String) defaultObject);
		} else if (defaultObject instanceof Integer) {
			return sp.getInt(key, (Integer) defaultObject);
		} else if (defaultObject instanceof Boolean) {
			return sp.getBoolean(key, (Boolean) defaultObject);
		} else if (defaultObject instanceof Float) {
			return sp.getFloat(key, (Float) defaultObject);
		} else if (defaultObject instanceof Long) {
			return sp.getLong(key, (Long) defaultObject);
		}

		return defaultObject;
	}
	
	/**
	 * 移除某个key值已经对应的值
	 * @param context
	 * @param key
	 */
	public static void remove(Context context, String key) {
		SharedPreferences sp = context.getSharedPreferences(Constants.SP_NAME,
				Context.MODE_PRIVATE);
		sp.edit().remove(key).commit();
	}

	/**
	 * 清除所有数据
	 * @param context
	 */
	public static void clear(Context context) {
		SharedPreferences sp = context.getSharedPreferences(Constants.SP_NAME,
				Context.MODE_PRIVATE);
		sp.edit().clear().commit();
	}
}
