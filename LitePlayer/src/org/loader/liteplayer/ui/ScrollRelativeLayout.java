package org.loader.liteplayer.ui;

import org.loader.liteplayer.utils.L;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Scroller;

/**
 * liteplayer by loader
 * @author qibin
 */
public class ScrollRelativeLayout extends RelativeLayout {
	private Scroller mScroller;
	private int mIndicatorHeight;
	
	public ScrollRelativeLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mScroller = new Scroller(context);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		View indicator = getChildAt(0);
		mIndicatorHeight = indicator.getMeasuredHeight();
		
		L.l("indicator height", mIndicatorHeight);
	}
	
	@Override
	public void computeScroll() {
		if(mScroller.computeScrollOffset()) {
			scrollTo(0, mScroller.getCurrY());
			postInvalidate();
		}
	}
	
	public void hideIndicator() {
		if(!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
		
		mScroller.startScroll(0, 0, 0, mIndicatorHeight, 500);
	}
	
	public void showIndicator() {
		scrollTo(0, 0);
		postInvalidate();
	}
}
