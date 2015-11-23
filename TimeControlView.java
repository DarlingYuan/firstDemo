/**
 * Project Name:  ListViewDemo
 * File Name:     TimeContrlView.java
 * Package Name:  org.com.cctest.widget
 * @Date:         2015年11月17日
 * Copyright (c)  2015, wulian All Rights Reserved.
 */

package com.wulian.icam.view.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.util.LruCache;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * @ClassName: TimeControlView
 * @Function: 时间卡尺 <Li>Activity中调用{@link TimeControlView}案例</Li>
 * 
 *            <pre>
 * public class TimeControlViewActivity extends FragmentActivity {
 * 	private TimeControlView timeControl;
 * 	private OnMiddleTimeChangeListener mOnMiddleTimeChangeListener;
 * 	private TextView tv_show_time;
 * 	private HashMap&lt;String, String&gt; listTimeMap;
 * 	private long initTime;
 * 
 * 	&#064;Override
 * 	protected void onCreate(Bundle savedInstanceState) {
 * 		super.onCreate(savedInstanceState);
 * 		setContentView(R.layout.activity_time_control);
 * 		timeControl = (TimeControlView) findViewById(R.id.time_control_view2);
 * 		tv_show_time = (TextView) findViewById(R.id.tv_show_time);
 * 		mOnMiddleTimeChangeListener = new OnMiddleTimeChangeListener() {
 * 			&#064;Override
 * 			public void setOnMiddleTimeChange(Long middleTime) {
 * 				tv_show_time.setText(timeControl.getTimeStandard(middleTime));
 * 			}
 * 		};
 * 		timeControl.setOnMiddleTimeChangeListener(mOnMiddleTimeChangeListener);
 * 		// 设置时间卡尺的中间时间，通常为当前时间的前一个小时，精确到分钟
 * 		initTime = System.currentTimeMillis() / 60 / 1000 * 60 * 1000 - 60 * 60
 * 				* 1000;
 * 		timeControl.setMiddleTime(initTime);
 * 		// 设置视频回看时间段
 * 		testListTimeMap();
 * 		timeControl.setRecordPeriodsTime(listTimeMap);
 * 	}
 * 
 * 	private void testListTimeMap() {
 * 		listTimeMap = new HashMap&lt;String, String&gt;();
 * 		listTimeMap.put(&quot;1447917180000&quot;, &quot;1447920780000&quot;);
 * 		listTimeMap.put(&quot;1447924380000&quot;, &quot;1447927980000&quot;);
 * 		listTimeMap.put(&quot;1447931580000&quot;, &quot;1447935180000&quot;);
 * 	}
 * }
 * </pre>
 * 
 *            <Li>布局文件如下：</Li>
 * 
 *            <pre>
 *            <?xml version="1.0" encoding="utf-8"?>
 *            <LinearLayout
 *            xmlns:android="http://schemas.android.com/apk/res/android"
 *            android:layout_width="match_parent"
 *            android:layout_height="match_parent" 
 *            android:background="#eeffff"
 *            android:orientation="vertical" > 
 *            	<TextView
 *            	android:id="@+id/tv_show_time" 
 *              android:layout_width="wrap_content"
 *            	android:layout_height="wrap_content"
 *            	android:layout_gravity="center_horizontal"
 *            	android:textColor="@color/black" 
 *              android:layout_marginTop="50dp"
 *            	android:layout_marginBottom="50dp"/>
 *           	<org.com.cctest.widget.TimeControlView
 *            	android:id="@+id/time_control_view2"
 *            	android:layout_width="match_parent" 
 *              android:layout_height="170dp"
 *            	></org.com.cctest.widget.TimeControlView>
 *            </LinearLayout>
 * </pre>
 * @date: 2015年11月17日
 * @author: yuanjs
 * @email: jiansheng.yuan@wuliangroup.com
 */
public class TimeControlView extends View {
	private static final int FIVE_DAY = 7200;// 5天
	private static final int ONE_DAY = 1440;// 1天
	private static final int TEN_HOUR = 600;// 10小时
	private static final int FIVE_HOUR = 300;// 5小时
	private static final int TWO_HOUR = 120;// 2小时
	private static final int ONE_HOUR = 60;// 1小时

	private final static int MINI_LENGTH = 30;// 时间标尺最小高度
	private final static int MIDDILE_LENGTH = 40;// 时间标尺中等高度
	private final static int HOUR_LENGTH = 80;// 时间标尺小时高度
	private final static int DAY_LENGTH = 100;// 时间标尺小时高度
	private static final int TEXT_SIZE = 13;// 绘制文字大小
	private static final String TAG = "TimeControlView";
	private int maxSize = 4 * 1024 * 1024;
	private LruCache<Long, Integer> cacheMH = new LruCache<Long, Integer>(
			maxSize) {
		protected int sizeOf(Long key, Integer value) {
			return value.byteValue();
		};
	};

	// 清空缓存
	public void clear() {
		if (cacheMH != null) {
			cacheMH.evictAll();
		}
	}

	/**
	 * 画笔
	 */
	private Paint bluePaint, commonPaint;
	private TextPaint textPaint;
	/**
	 * 密度缩放比例 与160dpi的比例
	 */
	private float mDensity;
	/**
	 * 屏幕宽度
	 */
	private int screenWidth;

	/**
	 * 时间长度（一屏幕显示的总时间），通过手势缩放来改变改值
	 */
	private int timeLength;
	/**
	 * 时间单位，有三种，具体查看{@link TimeUnit}（最小刻度之间代表的时间）
	 */
	private TimeUnit timeUnit;
	/**
	 * 最小刻度之间的距离，distance = screenWidth/(timeLength/timeUnit.getTimeUnit())
	 */
	private float distance;
	/**
	 * view的高度
	 */
	private int contentheight, height;
	/**
	 * 使View滚动
	 */
	// private Scroller mScroller;
	/**
	 * 传进来的初始化时间,也是中间时间
	 */
	private long initTime;
	/**
	 * "00:00"的宽度和"09月13日"的宽度
	 */
	private float textWidth, textWidthMD;
	/**
	 * "00:00"的高度"09月13日"的高度度
	 */
	private float textHeight, textHeightMD;

	private int margin_top;// 距离顶端的距离
	private float offset; // 时间偏移量
	private RectF rectF, timeRectF;
	private Path mPath;
	/**
	 * 所有的回放时间段
	 */
	private HashMap<String, String> listTimeMap;

	/**
	 * @ClassName: TimeUnit
	 * @Function: 时间单位，代表两格之间的时间
	 * @date: 2015年11月17日
	 * @author: yuanjs
	 * @email: jiansheng.yuan@wuliangroup.com
	 */
	public enum TimeUnit {
		TWENTY_FOUR_MINUTE(24), // 24minute
		TWO_MINUTE(2), // 2minute
		ONE_MINUTE(1);// 1minute
		int timeUnit; // 时间单位

		private TimeUnit(int timeUnit) {
			this.timeUnit = timeUnit;
		}

		public int getTimeUnit() {
			return this.timeUnit;
		}
	}

	public TimeControlView(Context context) {
		this(context, null);

	}

	public TimeControlView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimeControlView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public OnMiddleTimeChangeListener mOnMiddleTimeChangeListener;

	private void init(Context context, AttributeSet attrs) {

		mDensity = getContext().getResources().getDisplayMetrics().density;

		bluePaint = new Paint();
		bluePaint.setStrokeWidth(1f);
		bluePaint.setStyle(Paint.Style.FILL);
		bluePaint.setAntiAlias(true);
		bluePaint.setColor(Color.BLUE);
		bluePaint.setAlpha(100);

		commonPaint = new Paint();
		commonPaint.setStrokeWidth(1f);
		commonPaint.setStyle(Paint.Style.FILL);
		commonPaint.setAntiAlias(true);
		commonPaint.setColor(Color.argb(255, 90, 90, 90));

		textPaint = new TextPaint();
		textPaint.setColor(Color.argb(255, 150, 150, 150));
		textPaint.setTextSize(TEXT_SIZE * mDensity);
		textPaint.setStrokeWidth(0.5f);
		textPaint.setAntiAlias(true);

		mPath = new Path();

		screenWidth = getDeviceSize(context).widthPixels;
		timeLength = TEN_HOUR;
		timeUnit = TimeUnit.TWO_MINUTE;
		distance = getDistance();

		setBackgroundColor(Color.rgb(226, 226, 226));
		textWidth = getTextWidth("00:00", textPaint);
		textWidthMD = getTextWidth("11月03日", textPaint);
		textHeight = getTextHeight("00:00", textPaint);
		textHeightMD = getTextHeight("11月03日", textPaint);
		// 设置初始化时间
		if (initTime == 0) {
			// 默认为当前时间一小时前,只要精确到分
			initTime = System.currentTimeMillis() / 60 / 1000 * 60 * 1000 - 60
					* 60 * 1000;
		}
		middleTime = initTime;
		rectF = new RectF();
		timeRectF = new RectF();
		// testListTimeMap();
	}

	private void init() {
		contentheight = height - 12;
		margin_top = contentheight * 4 / 15;
		rectF.set(screenWidth * 2 / 5, (margin_top - textHeightMD) / 2 - 10,
				screenWidth * 3 / 5, (margin_top + textHeightMD) / 2 + 10);
		mPath.lineTo(screenWidth / 2, margin_top);
		mPath.lineTo(screenWidth / 2 - 6, margin_top - 12);
		mPath.lineTo(screenWidth / 2 + 6, margin_top - 12);
		mPath.lineTo(screenWidth / 2, margin_top);
		mPath.close();
		mPath.lineTo(screenWidth / 2, contentheight);
		mPath.lineTo(screenWidth / 2 - 6, contentheight + 12);
		mPath.lineTo(screenWidth / 2 + 6, contentheight + 12);
		mPath.lineTo(screenWidth / 2, contentheight);
		mPath.close();
	}

	// 得到text的高度
	private int getTextHeight(String text, Paint paint) {
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		int height = bounds.bottom + bounds.height();
		return height;
	}

	// 得到text的宽度
	private int getTextWidth(String text, Paint paint) {
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		int width = bounds.left + bounds.width();
		return width;
	}

	// 将9--->09，25-->01
	private String timePattern(int i) {
		/*
		 * if (i > 23) { i = i % 24; } if (i < 0) { i = i % 24 + 24; }
		 */
		return (i + "").length() > 1 ? i + "" : "0" + i;
	}

	/**
	 * @MethodName: getDistance
	 * @Function: 得到标准的时间单位间的距离
	 * @author: yuanjs
	 * @date: 2015年11月17日
	 * @email: jiansheng.yuan@wuliangroup.com
	 * @return
	 */
	private float getDistance() {
		return screenWidth * 1.0f / (timeLength / timeUnit.getTimeUnit());
	}

	/**
	 * @MethodName: getLenght
	 * @Function: 根据不同的时间单位和偏移量来计算当前画线的长度
	 * @author: yuanjs
	 * @date: 2015年11月17日
	 * @email: jiansheng.yuan@wuliangroup.com
	 * @param i
	 *            传过来的第几个绘图点
	 * @return 该绘图点处的绘图长度
	 */
	private Integer timeH, timeM;

	private int getLineLenght(long time) {
		timeM = cacheMH.get(time);
		if (timeM == null) {
			timeM = getTimeShortM(time);
			cacheMH.put(time, timeM);
		}
		timeH = cacheMH.get(time / 1000 / 60 / 60);
		if (timeH == null) {
			timeH = getTimeShortH(time);
			cacheMH.put(time / 1000 / 60 / 60, timeH);
		}
		switch (timeUnit) {
		case TWENTY_FOUR_MINUTE:
			if (timeH % 2 == 0) {
				if (timeH == 0 && timeM == 0) {
					return DAY_LENGTH;
				} else if (timeH % 12 == 0 && timeM == 0) {
					return HOUR_LENGTH;
				} else if (timeH % 2 == 0 && timeM == 0) {
					return MIDDILE_LENGTH;
				} else if (timeM % 24 == 0) {
					return MINI_LENGTH;
				}
			}

		case TWO_MINUTE:
			if (timeH == 0 && timeM == 0) {
				return DAY_LENGTH;
			} else if (timeM == 0) {
				return HOUR_LENGTH;
			} else if (timeM % 10 == 0) {
				return MIDDILE_LENGTH;
			} else if (timeM % 2 == 0) {
				return MINI_LENGTH;
			}
		case ONE_MINUTE:
			if (timeH == 0 && timeM == 0) {
				return DAY_LENGTH;
			} else if (timeM % 30 == 0) {
				return HOUR_LENGTH;
			} else if (timeM % 5 == 0) {
				return MIDDILE_LENGTH;
			} else {
				return MINI_LENGTH;
			}
		}
		return DAY_LENGTH;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		if (height == 0) {
			height = 200;
		}
		margin_top = contentheight * 2 / 9;
		init();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		commonPaint.setColor(Color.argb(255, 100, 100, 100));
		commonPaint.setStrokeWidth(1f);
		float drawDistance = 0f;
		timeM = cacheMH.get(middleTime);
		if (timeM == null) {
			timeM = getTimeShortM(middleTime);
			cacheMH.put(middleTime, timeM);
		}
		timeH = cacheMH.get(middleTime / 1000 / 60 / 60);
		if (timeH == null) {
			timeH = getTimeShortH(middleTime);
			cacheMH.put(middleTime / 1000 / 60 / 60, timeH);
		}
		int offsetTime = timeM % (timeUnit.getTimeUnit());
		if (timeUnit == TimeUnit.TWENTY_FOUR_MINUTE) {
			if (timeH % 2 != 0) {
				// 奇
				offsetTime = (timeM / (timeUnit.getTimeUnit() / 2)) % 2 == 0 ? timeUnit
						.getTimeUnit()
						/ 2
						+ timeM
						% (timeUnit.getTimeUnit() / 2) : timeM
						% (timeUnit.getTimeUnit() / 2);
			} else {
				// 偶
				offsetTime = timeM % (timeUnit.getTimeUnit());
			}
		}
		drawDistance = screenWidth / 2 - offsetTime * 1.0f
				/ timeUnit.getTimeUnit() * distance;
		float leftDrawDistance = drawDistance;
		long time = 0;
		for (int i = 0; leftDrawDistance >= 0; i++) {
			time = middleTime - offsetTime * 60 * 1000 - timeUnit.getTimeUnit()
					* 60 * 1000 * i;
			timeM = cacheMH.get(time);
			if (timeM == null) {
				timeM = getTimeShortM(time);
				cacheMH.put(time, timeM);
			}
			timeH = cacheMH.get(time / 1000 / 60 / 60);
			if (timeH == null) {
				timeH = getTimeShortH(time);
				cacheMH.put(time / 1000 / 60 / 60, timeH);
			}
			int lineLength = getLineLenght(time);
			// 绘线
			if (lineLength == DAY_LENGTH) {
				commonPaint.setColor(Color.RED);
				canvas.drawLine(leftDrawDistance, margin_top, leftDrawDistance,
						margin_top + DAY_LENGTH, commonPaint);
				canvas.drawLine(leftDrawDistance, contentheight,
						leftDrawDistance, contentheight - DAY_LENGTH,
						commonPaint);
				commonPaint.setColor(Color.argb(255, 90, 90, 90));
				// 绘制"09月17日"
				canvas.drawText(getTimeMD(time), leftDrawDistance - textWidthMD
						/ 2, (margin_top - textHeightMD) / 2 + textHeightMD,
						textPaint);
			} else {
				canvas.drawLine(leftDrawDistance, margin_top, leftDrawDistance,
						margin_top + lineLength, commonPaint);
				canvas.drawLine(leftDrawDistance, contentheight,
						leftDrawDistance, contentheight - lineLength,
						commonPaint);
			}
			textPaint.setColor(Color.argb(255, 150, 150, 150));
			switch (timeUnit) {
			case TWENTY_FOUR_MINUTE:
				if (lineLength == DAY_LENGTH || lineLength == HOUR_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							leftDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				}
				break;
			case TWO_MINUTE:
				if (lineLength == HOUR_LENGTH || lineLength == DAY_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							leftDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				}
				break;
			case ONE_MINUTE:
				if (lineLength == DAY_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							leftDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				} else if (lineLength == HOUR_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":"
							+ timePattern(timeM), leftDrawDistance, margin_top
							+ MIDDILE_LENGTH + 3 + textHeight, textPaint);
				}
				break;
			}
			leftDrawDistance -= distance;
		}
		float rightDrawDistance = drawDistance;
		// 绘制右边
		for (int i = 0; rightDrawDistance <= screenWidth; i++) {
			time = middleTime - offsetTime * 60 * 1000 + timeUnit.getTimeUnit()
					* 60 * 1000 * i;
			timeM = cacheMH.get(time);
			if (timeM == null) {
				timeM = getTimeShortM(time);
				cacheMH.put(time, timeM);
			}
			timeH = cacheMH.get(time / 1000 / 60 / 60);
			if (timeH == null) {
				timeH = getTimeShortH(time);
				cacheMH.put(time / 1000 / 60 / 60, timeH);
			}
			int lineLength = getLineLenght(time);
			if (lineLength == DAY_LENGTH) {
				commonPaint.setColor(Color.RED);
				canvas.drawLine(rightDrawDistance, margin_top,
						rightDrawDistance, margin_top + DAY_LENGTH, commonPaint);
				canvas.drawLine(rightDrawDistance, contentheight,
						rightDrawDistance, contentheight - DAY_LENGTH,
						commonPaint);
				commonPaint.setColor(Color.argb(255, 90, 90, 90));
				canvas.drawText(getTimeMD(time), rightDrawDistance
						- textWidthMD / 2, (margin_top - textHeightMD) / 2
						+ textHeightMD, textPaint);
			} else {
				canvas.drawLine(rightDrawDistance, margin_top,
						rightDrawDistance, margin_top + lineLength, commonPaint);
				canvas.drawLine(rightDrawDistance, contentheight,
						rightDrawDistance, contentheight - lineLength,
						commonPaint);
			}
			switch (timeUnit) {
			case TWENTY_FOUR_MINUTE:
				if (lineLength == DAY_LENGTH || lineLength == HOUR_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							rightDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				}
				break;
			case TWO_MINUTE:
				if (lineLength == HOUR_LENGTH || lineLength == DAY_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							rightDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				}
				break;
			case ONE_MINUTE:
				if (lineLength == DAY_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":00",
							rightDrawDistance, margin_top + MIDDILE_LENGTH + 3
									+ textHeight, textPaint);
				} else if (lineLength == HOUR_LENGTH) {
					canvas.drawText(timePattern(timeH) + ":"
							+ timePattern(timeM), rightDrawDistance, margin_top
							+ MIDDILE_LENGTH + 3 + textHeight, textPaint);
				}
				break;
			}
			rightDrawDistance += distance;
		}
		if (listTimeMap != null) {
			Long timeLeft = middleTime - timeLength * 60 * 1000 / 2;
			Long timeRight = middleTime + timeLength * 60 * 1000 / 2;
			Long drawLeft, drawRight;
			Iterator<String> startTimeIterator = listTimeMap.keySet()
					.iterator();
			// TODO 绘画回看时间段区域
			while (startTimeIterator.hasNext()) {
				String timeStartStr = startTimeIterator.next();
				Long timeStart = Long.parseLong(timeStartStr);
				Long timeEnd = Long.parseLong(listTimeMap.get(timeStartStr));
				if (timeStart > timeLeft) {
					drawLeft = timeStart;
				} else {
					drawLeft = timeLeft;
				}
				if (timeEnd < timeRight) {
					drawRight = timeEnd;
				} else {
					drawRight = timeRight;
				}
				timeRectF.set((drawLeft - timeLeft) * 1.0f
						/ (timeLength * 60 * 1000) * screenWidth, margin_top,
						screenWidth - (timeRight - drawRight) * 1.0f
								/ (timeLength * 60 * 1000) * screenWidth,
						contentheight);
				canvas.drawRect(timeRectF, bluePaint);
			}
		}
		// 绘制外围上下两条线
		commonPaint.setColor(Color.argb(255, 80, 80, 80));
		commonPaint.setStrokeWidth(2f);
		canvas.drawLine(0, margin_top, screenWidth, margin_top, commonPaint);
		canvas.drawLine(0, contentheight - 1, screenWidth, contentheight - 1,
				commonPaint);

		commonPaint.setColor(Color.argb(255, 9, 166, 228));
		canvas.drawOval(rectF, commonPaint);

		canvas.drawLine(screenWidth / 2, margin_top, screenWidth / 2,
				contentheight, commonPaint);
		// 中间线上下两菱形箭头,初始化放到onMeasure下
		canvas.drawPath(mPath, commonPaint);
		// 显示中间时间的月和日
		textPaint.setColor(Color.WHITE);
		canvas.drawText(getTimeMD(middleTime), (screenWidth - textWidthMD) / 2,
				(margin_top - textHeightMD) / 2 + textHeightMD, textPaint);
		super.onDraw(canvas);
	}

	private float lastX, curX, moveX;
	private long middleTime;
	private float lastScaleX, curScaleX;
	private boolean isOnePoint = true, isOut;// 手指数和触碰区域

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() == 1) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				lastX = event.getX();
				isOut = event.getY() > margin_top ? false : true;
				isOnePoint = true;
			case MotionEvent.ACTION_MOVE:
				curX = event.getX();
				moveX = lastX - curX;
				if (isOnePoint && !isOut)
					changeMoveAndValue();
				break;
			case MotionEvent.ACTION_UP:
				break;
			default:
				break;
			}
			lastX = curX;
		} else if (event.getPointerCount() == 2) {
			isOnePoint = false;
			switch (event.getAction() & event.getActionMasked()) {
			case MotionEvent.ACTION_POINTER_DOWN:
				lastScaleX = Math.abs(event.getX(0) - event.getX(1));
				isOut = event.getY() > margin_top ? false : true;
			case MotionEvent.ACTION_MOVE:
				curScaleX = Math.abs(event.getX(0) - event.getX(1));
				if (!isOut)
					changeTimeUnitandDistance();
				break;
			case MotionEvent.ACTION_UP:
				break;
			default:
				break;
			}
			lastScaleX = curScaleX;
		}
		return true;
	}

	private int tempTimeLength;// 减少绘图次数

	/**
	 * @MethodName: changeTimeUnitandDistance
	 * @Function: 缩放实现
	 * @author: yuanjs
	 * @date: 2015年11月17日
	 * @email: jiansheng.yuan@wuliangroup.com
	 */
	private void changeTimeUnitandDistance() {
		/**
		 * 1.要保持中间值不变 2.改变时间长度,从而改变时间单位距离 3.注意临界点，要改变时间单位
		 */
		if (Math.abs(curScaleX - lastScaleX) > 2) {
			timeLength -= (curScaleX - lastScaleX) / distance
					* timeUnit.getTimeUnit();
			timeLength = timeLength / 2 * 2;// 取整
			switch (timeUnit) {
			case TWENTY_FOUR_MINUTE:
				if (timeLength <= ONE_DAY) {
					timeLength = TEN_HOUR;
					timeUnit = TimeUnit.TWO_MINUTE;
				}
				if (timeLength >= FIVE_DAY) {
					timeLength = FIVE_DAY;
				}
				break;
			case TWO_MINUTE:
				if (timeLength < TWO_HOUR) {
					timeLength = FIVE_HOUR;
					timeUnit = TimeUnit.ONE_MINUTE;
				}
				if (timeLength > TEN_HOUR) {
					timeLength = ONE_DAY;
					timeUnit = TimeUnit.TWENTY_FOUR_MINUTE;
				}
				break;
			case ONE_MINUTE:
				if (timeLength < ONE_HOUR) {
					timeLength = ONE_HOUR;
				}
				if (timeLength >= FIVE_HOUR) {
					timeLength = TWO_HOUR;
					timeUnit = TimeUnit.TWO_MINUTE;
				}
				break;
			}
		}
		distance = getDistance();
		initTime = middleTime;
		offset = 0;
		if (tempTimeLength != timeLength) {
			invalidate();
			tempTimeLength = timeLength;
		}
	}

	private long lastMiddleTime;// 减少绘图次数

	/**
	 * @MethodName: changeMoveAndValue
	 * @Function: 实现滑动
	 * @author: yuanjs
	 * @date: 2015年11月17日
	 * @email: jiansheng.yuan@wuliangroup.com
	 */
	private void changeMoveAndValue() {
		distance = getDistance();
		offset += moveX;
		long moveTime = (long) ((int) (offset / distance * timeUnit
				.getTimeUnit()) * 60 * 1000);
		middleTime = initTime + moveTime;
		if (lastMiddleTime != middleTime) {
			if (mOnMiddleTimeChangeListener != null) {
				mOnMiddleTimeChangeListener.setOnMiddleTimeChange(middleTime);
			}
			lastMiddleTime = middleTime;
			invalidate();
		}
	}

	private DisplayMetrics getDeviceSize(Context context) {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metrics);
		return metrics;
	}

	private SimpleDateFormat timeFormatStandard = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	private SimpleDateFormat timeFormatMD = new SimpleDateFormat("MM月dd日",
			Locale.getDefault());
	private SimpleDateFormat timeFormatH = new SimpleDateFormat("HH",
			Locale.getDefault());
	private SimpleDateFormat timeFormatM = new SimpleDateFormat("mm",
			Locale.getDefault());
	private Date date = new Date();

	public String getTimeStandard(long time) {
		date.setTime(time);
		return timeFormatStandard.format(date);
	}

	private String getTimeMD(long time) {
		date.setTime(time);
		return timeFormatMD.format(date);
	}

	private int getTimeShortH(long time) {
		date.setTime(time);
		return Integer.parseInt(timeFormatH.format(date));
	}

	private int getTimeShortM(long time) {
		date.setTime(time);
		return Integer.parseInt(timeFormatM.format(date));
	}

	public interface OnMiddleTimeChangeListener {
		void setOnMiddleTimeChange(Long middleTime);
	}

	/** 中间时间接口回调 */
	public void setOnMiddleTimeChangeListener(
			OnMiddleTimeChangeListener mOnMiddleTimeChangeListener) {
		if (mOnMiddleTimeChangeListener != null) {
			this.mOnMiddleTimeChangeListener = mOnMiddleTimeChangeListener;
		} else {
			throw new IllegalArgumentException(
					"OnMiddleTimeChangeListener is null!");
		}
	}

	/** 设置当前时间 */
	public void setMiddleTime(Long time) {
		if (middleTime == time) {
			return;
		}
		this.middleTime = time;
		postInvalidate();
	}

	/** 设置视频回看的所有时间段 ，时间单位：毫秒 */
	public void setRecordPeriodsTime(HashMap<String, String> listTimeMap) {
		if (listTimeMap != null) {
			if (this.listTimeMap != null && this.listTimeMap.size() > 0) {
				this.listTimeMap.clear();
			} else {
				this.listTimeMap = new HashMap<String, String>();
			}
			this.listTimeMap.putAll(listTimeMap);
			postInvalidate();
		} else {
			throw new IllegalArgumentException("the listTimeMap is null");
		}
	}

	/** 内测回看时间段代码 */
	private void testListTimeMap() {
		listTimeMap = new HashMap<String, String>();
		listTimeMap.put("1447917180000", "1447920780000");
		listTimeMap.put("1447924380000", "1447927980000");
		listTimeMap.put("1447931580000", "1447935180000");
	}
}
