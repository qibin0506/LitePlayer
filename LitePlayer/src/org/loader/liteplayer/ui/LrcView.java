package org.loader.liteplayer.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.loader.liteplayer.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

/**
 * liteplayer by loader
 * 显示lrc歌词控件
 */
@SuppressLint("DrawAllocation")
public class LrcView extends View {
	private static final int SCROLL_TIME = 500;
	private static final String DEFAULT_TEXT = "暂无歌词";
	
	private List<String> mLrcs = new ArrayList<String>(); // 存放歌词
	private List<Long> mTimes = new ArrayList<Long>(); // 存放时间

	private long mNextTime = 0l; // 保存下一句开始的时间

	private int mViewWidth; // view的宽度
	private int mLrcHeight; // lrc界面的高度
	private int mRows;      // 多少行
	private int mCurrentLine = 0; // 当前行
	private int mOffsetY;   // y上的偏移
	private int mMaxScroll; // 最大滑动距离=一行歌词高度+歌词间距

	private float mTextSize; // 字体
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
		inflateAttributes(attrs);
	}

	// 初始化操作
	private void inflateAttributes(AttributeSet attrs) {
		// <begin>
		// 解析自定义属性
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.Lrc);
		mTextSize = ta.getDimension(R.styleable.Lrc_textSize, 50.0f);
		mRows = ta.getInteger(R.styleable.Lrc_rows, 5);
		mDividerHeight = ta.getDimension(R.styleable.Lrc_dividerHeight, 0.0f);

		int normalTextColor = ta.getColor(R.styleable.Lrc_normalTextColor, 0xffffffff);
		int currentTextColor = ta.getColor(R.styleable.Lrc_currentTextColor, 0xff00ffde);
		ta.recycle();
		// </end>

		// 计算lrc面板的高度
		mLrcHeight = (int) (mTextSize + mDividerHeight) * mRows + 5;

		mNormalPaint = new Paint();
		mCurrentPaint = new Paint();
		
		// 初始化paint
		mNormalPaint.setTextSize(mTextSize);
		mNormalPaint.setColor(normalTextColor);
		mNormalPaint.setAntiAlias(true);
		mCurrentPaint.setTextSize(mTextSize);
		mCurrentPaint.setColor(currentTextColor);
		mCurrentPaint.setAntiAlias(true);
		
		mTextBounds = new Rect();
		mCurrentPaint.getTextBounds(DEFAULT_TEXT, 0, DEFAULT_TEXT.length(), mTextBounds);
		mMaxScroll = (int) (mTextBounds.height() + mDividerHeight);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// 重新设置view的高度
		int measuredHeightSpec = MeasureSpec.makeMeasureSpec(mLrcHeight, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, measuredHeightSpec);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// 获取view宽度
		mViewWidth = getMeasuredWidth();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mBackground != null) {
			canvas.drawBitmap(Bitmap.createScaledBitmap(mBackground, mViewWidth, mLrcHeight, true),
					new Matrix(), null);
		}
		
		// float centerY = (getMeasuredHeight() + mTextBounds.height() - mDividerHeight) / 2;
		float centerY = (getMeasuredHeight() + mTextBounds.height()) / 2;
		if (mLrcs.isEmpty() || mTimes.isEmpty()) {
			canvas.drawText(DEFAULT_TEXT, 
					(mViewWidth - mCurrentPaint.measureText(DEFAULT_TEXT)) / 2,
					centerY, mCurrentPaint);
			return;
		}

		float offsetY = mTextBounds.height() + mDividerHeight;
		String currentLrc = mLrcs.get(mCurrentLine);
		float currentX = (mViewWidth - mCurrentPaint.measureText(currentLrc)) / 2;
		// 画当前行
		canvas.drawText(currentLrc, currentX, centerY - mOffsetY, mCurrentPaint);
		
		int firstLine = mCurrentLine - mRows / 2;
		firstLine = firstLine <= 0 ? 0 : firstLine;
		int lastLine = mCurrentLine + mRows / 2 + 2;
		lastLine = lastLine >= mLrcs.size() - 1 ? mLrcs.size() - 1 : lastLine;
		
		// 画当前行上面的
		for (int i = mCurrentLine - 1,j=1; i >= firstLine; i--,j++) {
			String lrc = mLrcs.get(i);
			float x = (mViewWidth - mNormalPaint.measureText(lrc)) / 2;
			canvas.drawText(lrc, x, centerY - j * offsetY - mOffsetY, mNormalPaint);
		}

		// 画当前行下面的
		for (int i = mCurrentLine + 1,j=1; i <= lastLine; i++,j++) {
			String lrc = mLrcs.get(i);
			float x = (mViewWidth - mNormalPaint.measureText(lrc)) / 2;
			canvas.drawText(lrc, x, centerY + j * offsetY - mOffsetY, mNormalPaint);
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

	// 解析时间
	private Long parseTime(String time) {
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

	// 解析每行
    private List<LrcLine> parseLine(String line) {
        Matcher matcher = Pattern.compile("\\[\\d.+\\].+").matcher(line);
        // 如果形如：[xxx]后面啥也没有的，则return空
        if (!matcher.matches()) {
            return null;
        }

        line = line.replaceAll("\\[", "");
        if (line.endsWith("]")) {
            line += " ";
        }
        String[] result = line.split("\\]");
        int size = result.length;
        if (size == 0) {
            return null;
        }
        List<LrcLine> ret = new LinkedList<>();
        if (size == 1) {
            LrcLine l = new LrcLine();
            l.time = parseTime(result[0]);
            l.line = "";
            ret.add(l);
        } else {
            for (int i = 0; i < size - 1; i++) {
                LrcLine l = new LrcLine();
                l.time = parseTime(result[i]);
                l.line = result[size - 1];
                ret.add(l);
            }
        }

        return ret;
    }

	// 外部提供方法
	// 传入当前播放时间
	public synchronized void changeCurrent(long time) {
		// 如果当前时间小于下一句开始的时间
		// 直接return
		if (mNextTime > time) {
			return;
		}
		
		// 每次进来都遍历存放的时间
		int timeSize = mTimes.size();
		for (int i = 0; i < timeSize; i++) {
			
			// 解决最后一行歌词不能高亮的问题
			if(mNextTime == mTimes.get(timeSize - 1)) {
				mNextTime += 60 * 1000;
				mScroller.abortAnimation();
				mScroller.startScroll(timeSize, 0, 0, mMaxScroll, SCROLL_TIME);
//				mNextTime = mTimes.get(i);
//				mCurrentLine = i <= 1 ? 0 : i - 1;
				postInvalidate();
				return;
			}
			
			// 发现这个时间大于传进来的时间
			// 那么现在就应该显示这个时间前面的对应的那一行
			// 每次都重新显示，是不是要判断：现在正在显示就不刷新了
			if (mTimes.get(i) > time) {
				mNextTime = mTimes.get(i);
				mScroller.abortAnimation();
				mScroller.startScroll(i, 0, 0, mMaxScroll, SCROLL_TIME);
//				mNextTime = mTimes.get(i);
//				mCurrentLine = i <= 1 ? 0 : i - 1;
				postInvalidate();
				return;
			}
		}
	}
	
	// 外部提供方法
	// 拖动进度条时
	public void onDrag(int progress) {
		for(int i=0;i<mTimes.size();i++) {
			if(Integer.parseInt(mTimes.get(i).toString()) > progress) {
				mNextTime = i == 0 ? 0 : mTimes.get(i-1);
				return;
			}
		}
	}

	// 外部提供方法
	// 设置lrc的路径
    public void setLrcPath(String path) {
        reset();
        File file = new File(path);
        if (!file.exists()) {
            postInvalidate();
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String line;
            List<LrcLine> lrcLinesPerInFileLine;
            List<LrcLine> allLines = new LinkedList<>();

            while (null != (line = reader.readLine())) {
                lrcLinesPerInFileLine = parseLine(line);
                if (lrcLinesPerInFileLine == null) {
                    continue;
                }

                allLines.addAll(lrcLinesPerInFileLine);
            }
            Collections.sort(allLines);

            mTimes.clear();
            mLrcs.clear();
            if (allLines.isEmpty()) {
                return;
            }
            LrcLine lastLine = allLines.get(allLines.size() - 1);
            if (TextUtils.isEmpty(lastLine.line) || lastLine.line.trim().isEmpty()) {
                allLines.remove(allLines.size() - 1);
            }
            for (LrcLine l : allLines) {
                mTimes.add(l.time);
                mLrcs.add(l.line);
            }
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

	private void reset() {
		mLrcs.clear();
		mTimes.clear();
		mCurrentLine = 0;
		mNextTime = 0l;
	}
	
	// 是否设置歌词
	public boolean hasLrc() {
		return mLrcs != null && !mLrcs.isEmpty();
	}

	// 外部提供方法
	// 设置背景图片
	public void setBackground(Bitmap bmp) {
		mBackground = bmp;
	}

    private static class LrcLine implements Comparable<LrcLine> {
        long time;
        String line;
        @Override
        public int compareTo(LrcLine another) {
            return (int) (time - another.time);
        }
    }
}
