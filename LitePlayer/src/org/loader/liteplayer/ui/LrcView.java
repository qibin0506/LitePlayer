package org.loader.liteplayer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * liteplayer by loader
 * 显示lrc歌词控件
 */
public class LrcView extends View {

	private static final int SCROLL_TIME = 500;
	private static final String DEFAULT_TEXT = "暂无歌词";

	private List<LrcLine> mLrcLines = new LinkedList<>();

	private long mNextTime = 0L; // 保存下一句开始的时间

	private int mLrcHeight; // lrc界面的高度
	private int mRows;      // 多少行
	private int mCurrentLine = 0; // 当前行
	private int mOffsetY;   // y上的偏移
	private int mMaxScroll; // 最大滑动距离=一行歌词高度+歌词间距
	private int mCurrentXOffset;

	private float mDividerHeight; // 行间距

	private Rect mTextBounds;

	private Paint mNormalPaint; // 常规的字体
	private Paint mCurrentPaint; // 当前歌词的大小

	private Bitmap mBackground;

	private Scroller mScroller;

	public LrcView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mScroller = new Scroller(context, new LinearInterpolator());
		init(attrs);
	}

	// 初始化操作
	private void init(AttributeSet attrs) {
		// <begin>
		// 解析自定义属性
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.Lrc);
		float textSize = ta.getDimension(R.styleable.Lrc_android_textSize, 10.0f);
		mRows = ta.getInteger(R.styleable.Lrc_rows, 0);
		mDividerHeight = ta.getDimension(R.styleable.Lrc_dividerHeight, 0.0f);

		int normalTextColor = ta.getColor(R.styleable.Lrc_normalTextColor, 0xffffffff);
		int currentTextColor = ta.getColor(R.styleable.Lrc_currentTextColor, 0xff00ffde);
		ta.recycle();
		// </end>

		if (mRows != 0) {
			// 计算lrc面板的高度
			mLrcHeight = (int) (textSize + mDividerHeight) * mRows + 5;
		}

		mNormalPaint = new Paint();
		mCurrentPaint = new Paint();

		// 初始化paint
		mNormalPaint.setTextSize(textSize);
		mNormalPaint.setColor(normalTextColor);
		mNormalPaint.setAntiAlias(true);
		mCurrentPaint.setTextSize(textSize);
		mCurrentPaint.setColor(currentTextColor);
		mCurrentPaint.setAntiAlias(true);

		mTextBounds = new Rect();
		mCurrentPaint.getTextBounds(DEFAULT_TEXT, 0, DEFAULT_TEXT.length(), mTextBounds);
		computeMaxScroll();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// 如果没有设置固定行数， 则默认测量高度，并根据高度计算行数
		if (mRows == 0) {
			int width = getPaddingLeft() + getPaddingRight();
			int height = getPaddingTop() + getPaddingBottom();
			width = Math.max(width, getSuggestedMinimumWidth());
			height = Math.max(height, getSuggestedMinimumHeight());

			widthMeasureSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
			setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0),
					resolveSizeAndState(height, heightMeasureSpec, 0));

			mLrcHeight = getMeasuredHeight();
			computeRows();
			return;
		}

		// 设置了固定行数，重新设置view的高度
		int measuredHeightSpec = MeasureSpec.makeMeasureSpec(mLrcHeight, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, measuredHeightSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mBackground != null) {
			mBackground = Bitmap.createScaledBitmap(mBackground, getMeasuredWidth(), mLrcHeight, true);
		}
	}

	/**
	 * 根据高度计算行数
	 */
	private void computeRows() {
		float lineHeight = mTextBounds.height() + mDividerHeight;
		mRows = (int) (getMeasuredHeight() / lineHeight);
	}

	/**
	 * 计算滚动距离
	 */
	private void computeMaxScroll() {
		mMaxScroll = (int) (mTextBounds.height() + mDividerHeight);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		int width = getMeasuredWidth();

		if (mBackground != null) {
			canvas.drawBitmap(mBackground, new Matrix(), null);
		}

		// float centerY = (getMeasuredHeight() + mTextBounds.height() - mDividerHeight) / 2;
		float centerY = (getMeasuredHeight() + mTextBounds.height()) / 2;
		if (mLrcLines.isEmpty()) {
			canvas.drawText(DEFAULT_TEXT,
					(width - mCurrentPaint.measureText(DEFAULT_TEXT)) / 2,
					centerY, mCurrentPaint);
			return;
		}

		float offsetY = mTextBounds.height() + mDividerHeight;
		drawCurrentLine(canvas, width, centerY - mOffsetY);

		int firstLine = mCurrentLine - mRows / 2;
		firstLine = firstLine <= 0 ? 0 : firstLine;
		int lastLine = mCurrentLine + mRows / 2 + 2;
		lastLine = lastLine >= mLrcLines.size() - 1 ? mLrcLines.size() - 1 : lastLine;

		// 画当前行上面的
		for (int i = mCurrentLine - 1,j = 1; i >= firstLine; i--,j++) {
			String lrc = mLrcLines.get(i).lrc;
			float x = (width - mNormalPaint.measureText(lrc)) / 2;
			canvas.drawText(lrc, x, centerY - j * offsetY - mOffsetY, mNormalPaint);
		}

		// 画当前行下面的
		for (int i = mCurrentLine + 1,j = 1; i <= lastLine; i++,j++) {
			String lrc = mLrcLines.get(i).lrc;
			float x = (width - mNormalPaint.measureText(lrc)) / 2;
			canvas.drawText(lrc, x, centerY + j * offsetY - mOffsetY, mNormalPaint);
		}
	}


	private void drawCurrentLine(Canvas canvas, int width, float y) {
		mHandler.removeMessages(1);
		String currentLrc = mLrcLines.get(mCurrentLine).lrc;
		float contentWidth = mCurrentPaint.measureText(currentLrc);
		if (contentWidth > width) {
			canvas.drawText(currentLrc, mCurrentXOffset, y, mCurrentPaint);
			if (contentWidth - Math.abs(mCurrentXOffset) < width) {
				mCurrentXOffset = 0;
			} else {
				mHandler.sendEmptyMessage(1);
			}
		} else {
			float currentX = (width - mCurrentPaint.measureText(currentLrc)) / 2;
			// 画当前行
			canvas.drawText(currentLrc, currentX, y, mCurrentPaint);
		}
	}

	@Override
	public void computeScroll() {
		if(mScroller.computeScrollOffset()) {
			mOffsetY = mScroller.getCurrY();
			if(mScroller.isFinished()) {
				int cur = mScroller.getCurrX();
				mCurrentLine = cur <= 1 ? 0 : cur - 1;
				mOffsetY = 0;
			}

			postInvalidate();
		}
	}

	/**
	 * 解析时间
	 * @param time
	 * @return
	 */
	private long parseTime(String time) {
		// 03:02.12
		String[] min = time.split(":");
		String[] sec = min[1].split("\\.");

		long minInt = Long.parseLong(min[0].replaceAll("\\D+", "")
				.replaceAll("\r", "").replaceAll("\n", "").trim());
		long secInt = Long.parseLong(sec[0].replaceAll("\\D+", "")
				.replaceAll("\r", "").replaceAll("\n", "").trim());
		long milInt = Long.parseLong(sec[1].replaceAll("\\D+", "")
				.replaceAll("\r", "").replaceAll("\n", "").trim());

		return minInt * 60 * 1000 + secInt * 1000 + milInt * 10;
	}

	/**
	 * 解析每一行
	 * @param line
	 * @return
	 */
	private List<LrcLine> parseLine(String line) {
		Matcher matcher = Pattern.compile("\\[\\d.+\\].+").matcher(line);
		// 如果形如：[xxx]后面啥也没有的，则return空
		if (!matcher.matches()) {
			System.out.println("no matched");
			return null;
		}

		line = line.replaceAll("\\[", "");
		if (line.endsWith("]")) {
			line += " ";
		}

		String[] result = line.split("\\]");
		int size = result.length;
		if (size == 0) {
			System.out.println("size 0");
			return null;
		}
		List<LrcLine> ret = new LinkedList<>();
		if (size == 1) {
			LrcLine lrcLine = new LrcLine();
			lrcLine.time = parseTime(result[0]);
			lrcLine.lrc = "";
			ret.add(lrcLine);
		} else {
			for (int i = 0; i < size - 1; i++) {
				LrcLine lrcLine = new LrcLine();
				lrcLine.time = parseTime(result[i]);
				lrcLine.lrc = result[size - 1];
				ret.add(lrcLine);
			}
		}

		return ret;
	}

	/**
	 * 在音乐播放回调里调用
	 * @param time 当前播放时间
	 */
	public synchronized void onProgress(long time) {
		// 如果当前时间小于下一句开始的时间
		// 直接return
		if (mNextTime > time) {
			return;
		}

		// 每次进来都遍历存放的时间
		int size = mLrcLines.size();
		for (int i = 0; i < size; i++) {
			// 解决最后一行歌词不能高亮的问题
			if(mNextTime == mLrcLines.get(size - 1).time) {
				mHandler.removeMessages(1);

				mNextTime += 60 * 1000;
				mScroller.abortAnimation();
				mScroller.startScroll(size, 0, 0, mMaxScroll, SCROLL_TIME);
				postInvalidate();
				break;
			}

			// 发现这个时间大于传进来的时间
			// 那么现在就应该显示这个时间前面的对应的那一行
			// 每次都重新显示，是不是要判断：现在正在显示就不刷新了
			if (mLrcLines.get(i).time > time) {
				mNextTime = mLrcLines.get(i).time;
				if (mCurrentLine == 0 && i == 1) {
					postInvalidate();
					break;
				}
				mScroller.abortAnimation();
				mScroller.startScroll(i, 0, 0, mMaxScroll, SCROLL_TIME);
				postInvalidate();
				break;
			}
		}
	}

	/**
	 * 拖动进度条时调用，用来改变歌词位置
	 * @param progress
	 */
	public void onDrag(int progress) {
		int lineCount = mLrcLines.size();

		for (int i = 0; i < lineCount; i++) {
			if(mLrcLines.get(i).time > progress) {
				mNextTime = i == 0 ? 0 : mLrcLines.get(i - 1).time;
				return;
			}
		}

		mNextTime = mLrcLines.get(mLrcLines.size() - 1).time;
	}

	/**
	 * 设置lrc内容
	 * @param lrc
	 */
	public void setLrc(String lrc) {
		reset();
		if (TextUtils.isEmpty(lrc)) { return;}
		parseLrc(new ByteArrayInputStream(lrc.getBytes()));
	}

	/**
	 * 设置lrc路径
	 * @param path
	 */
	public void setLrcPath(String path) {
		reset();
		if (TextUtils.isEmpty(path)) { return;}

		File file = new File(path);
		if (!file.exists()) {
			postInvalidate();
			return;
		}

		try {
			parseLrc(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 解析歌词内容
	 * @param inputStream
	 */
	private void parseLrc(InputStream inputStream) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream));

			String line;
			List<LrcLine> perLine;
			List<LrcLine> allLines = new LinkedList<>();

			while (null != (line = reader.readLine())) {
				perLine = parseLine(line);
				if (perLine == null) { continue;}
				allLines.addAll(perLine);
			}
			// sort by time
			Collections.sort(allLines);

			mLrcLines.clear();
			if (allLines.isEmpty()) { return;}

			LrcLine lastLine = allLines.get(allLines.size() - 1);
			if (TextUtils.isEmpty(lastLine.lrc) || lastLine.lrc.trim().isEmpty()) {
				allLines.remove(allLines.size() - 1);
			}

			mLrcLines.addAll(allLines);
			postInvalidate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void reset() {
		mLrcLines.clear();
		mCurrentLine = 0;
		mNextTime = 0L;
		mOffsetY = 0;
	}

	/**
	 * 是否设置了歌词
	 * @return
	 */
	public boolean hasLrc() {
		return mLrcLines != null && !mLrcLines.isEmpty();
	}

	/**
	 * 设置背景图片
	 * @param bmp
	 */
	public void setBackground(Bitmap bmp) {
		mBackground = bmp;
	}

	/**
	 * 代表每一行，实现Comparable接口用来排序处理
	 */
	public static class LrcLine implements Comparable<LrcLine> {
		long time;
		String lrc;

		@Override
		public int compareTo(@NonNull LrcLine another) {
			return (int) (time - another.time);
		}
	}

	private MarqueeHandler mHandler = new MarqueeHandler(this);
	private static class MarqueeHandler extends Handler {
		private WeakReference<LrcView> mLrcViewRef;
		MarqueeHandler(LrcView view) {
			mLrcViewRef = new WeakReference<>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1 && mLrcViewRef.get() != null) {
				mLrcViewRef.get().mCurrentXOffset--;
				mLrcViewRef.get().invalidate();
				sendEmptyMessageDelayed(1, 500);
			}
		}
	}
}
