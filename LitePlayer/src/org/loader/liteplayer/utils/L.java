package org.loader.liteplayer.utils;

/**
 * liteplayer by loader
 * @author qibin
 */
public class L {
	private static final boolean debug = true;
	
	public static void l(String tag, Object msg) {
		l(tag + "-->" + msg);
	}
	
	public static void l(Object msg) {
		if(!debug) return;
		System.out.println(msg);
	}
}
