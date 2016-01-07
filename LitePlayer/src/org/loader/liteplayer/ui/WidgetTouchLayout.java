package org.loader.liteplayer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public class WidgetTouchLayout extends LinearLayout {
	private int mDownX;
	private int mDownY;
	
	private int mTouchSlop;
	
	private OnWidgetTouchListener mListener; 
	
	public WidgetTouchLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WidgetTouchLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownX = (int) ev.getRawX();
			mDownY = (int) ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			int distanceX = (int) (ev.getRawX() - mDownX);
			int distanceY = (int) (ev.getRawY() - mDownY);
			if(Math.abs(distanceX) > mTouchSlop || Math.abs(distanceY) > mTouchSlop) {
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			mDownX = 0;
			mDownY = 0;
			break;
		}
		
		return super.onInterceptTouchEvent(ev);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
			boolean touched = mListener.onTouch((int)(ev.getRawX()-mDownX), 
					(int)(ev.getRawY() - mDownY));
			
			mDownX = (int) ev.getRawX();
			mDownY = (int) ev.getRawY();
			
			return touched;
		case MotionEvent.ACTION_UP:
			mDownX = 0;
			mDownY = 0;
			break;
		}
		
		return super.onTouchEvent(ev);
	}
	
	public void setOnWidgetTouchListener(OnWidgetTouchListener l) {
		mListener = l;
	}
	
	public interface OnWidgetTouchListener {
		public boolean onTouch(int distanceX, int distanceY);
	}
}
