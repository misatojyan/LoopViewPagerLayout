package com.github.why168;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.why168.animate.DepthPageTransformer;
import com.github.why168.animate.ZoomOutPageTransformer;
import com.github.why168.entity.LoopStyle;
import com.github.why168.scroller.LoopScroller;
import com.github.why168.utils.L;
import com.github.why168.utils.Tools;

import java.lang.reflect.Field;
import java.util.ArrayList;


/**
 * LoopViewPagerLayout
 *
 * @USER Edwin
 * @DATE 16/6/14 下午11:58
 */
public class LoopViewPagerLayout extends RelativeLayout implements ViewPager.OnPageChangeListener, View.OnTouchListener {
    private ViewPager loopViewPager;
    private LinearLayout indicatorLayout;
    private OnBannerItemClickListener onBannerItemClickListener = null;
    private LoopPagerAdapterWrapper loopPagerAdapterWrapper;
    private int totalDistance;//Little red dot all the distance to move
    private int startX;//The little red point position
    private int size = Tools.dip2px(getContext(), 8);//The size of the set point;
    private ArrayList<BannerInfo> bannerInfos;//banner data
    private TextView animIndicator;//Little red dot on the move
    private TextView[] indicators;//Initializes the white dots
    private static final int MESSAGE_LOOP = 5;
    private int loop_ms = 4000;//loop speed(ms)
    private int loop_style = -1; //loop style(enum values[-1:empty,1:depth 2:zoom])
    private int loop_duration = 2000;//loop rate(ms)
    private Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            if (msg.what == MESSAGE_LOOP) {
                if (loopViewPager.getCurrentItem() < Short.MAX_VALUE - 1) {
                    loopViewPager.setCurrentItem(loopViewPager.getCurrentItem() + 1, true);
                    sendEmptyMessageDelayed(MESSAGE_LOOP, getLoop_ms());
                }
            }
        }
    };

    public LoopViewPagerLayout(Context context) {
        super(context);
        L.e("Initialize LoopViewPagerLayout ---> context");
    }

    public LoopViewPagerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        L.e("Initialize LoopViewPagerLayout ---> context, attrs");
    }

    public LoopViewPagerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        L.e("Initialize LoopViewPagerLayout ---> context, attrs, defStyleAttr");
    }

    public void setLoopData(ArrayList<BannerInfo> bannerInfos, OnBannerItemClickListener onBannerItemClickListener) {
        L.e("LoopViewPager 1---> setLoopData");

        this.bannerInfos = bannerInfos;
        this.onBannerItemClickListener = onBannerItemClickListener;
        //TODO Initialize multiple times, clear images and little red dot
        if (indicatorLayout.getChildCount() > 0) {
            indicatorLayout.removeAllViews();
            removeView(animIndicator);
        }
        indicators = new TextView[bannerInfos.size()];
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new TextView(getContext());
            indicators[i].setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i != indicators.length - 1) {
                params.setMargins(0, 0, size, 0);
            } else {
                params.setMargins(0, 0, 0, 0);
            }
            indicators[i].setLayoutParams(params);
            indicators[i].setBackgroundResource(R.drawable.indicator_normal_background);//设置默认的背景颜色
            indicatorLayout.addView(indicators[i]);
        }

        //TODO little dots
        animIndicator = new TextView(getContext());
        animIndicator.setLayoutParams(new LinearLayout.LayoutParams(size, size));
//        animIndicator.setGravity(Gravity.CENTER);
        animIndicator.setBackgroundResource(R.drawable.indicator_selected_background);//设置选中的背景颜色
        addView(animIndicator);

        indicatorLayout.getViewTreeObserver().addOnPreDrawListener(new MyOnPreDrawListener());

        loopPagerAdapterWrapper = new LoopPagerAdapterWrapper();
        loopViewPager.setAdapter(loopPagerAdapterWrapper);
        loopViewPager.addOnPageChangeListener(this);

        int index = Short.MAX_VALUE / 2 - (Short.MAX_VALUE / 2) % bannerInfos.size();
        loopViewPager.setCurrentItem(index);
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (loopPagerAdapterWrapper.getCount() > 0) {
            float length = ((position % 4) + positionOffset) / (bannerInfos.size() - 1);
            //TODO To prevent the last picture the little red dot slip out.
            if (length >= 1)
                length = 1;
            float path = length * totalDistance;
            ViewCompat.setTranslationX(animIndicator, startX + path);

        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    class MyOnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        @Override
        public boolean onPreDraw() {
            Rect rootRect = new Rect();
            Point globalOffset = new Point();
            getGlobalVisibleRect(rootRect, globalOffset);

            Rect firstRect = new Rect();
            indicatorLayout.getChildAt(0).getGlobalVisibleRect(firstRect);
            firstRect.offset(-globalOffset.x, -globalOffset.y);

            Rect lastRect = new Rect();
            indicatorLayout.getChildAt(indicators.length - 1).getGlobalVisibleRect(lastRect);

            totalDistance = lastRect.left - indicatorLayout.getLeft();
            startX = firstRect.left;

            ViewCompat.setTranslationX(animIndicator, firstRect.left);
            ViewCompat.setTranslationY(animIndicator, firstRect.top);
            indicatorLayout.getViewTreeObserver().removeOnPreDrawListener(this);
            return false;
        }
    }


    class LoopPagerAdapterWrapper extends PagerAdapter {

        @Override
        public int getCount() {
            return Short.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final int index = position % bannerInfos.size();
            final BannerInfo bannerInfo = bannerInfos.get(index);
            final ImageView child = new ImageView(getContext());
            child.setImageResource(bannerInfo.resId);
            child.setScaleType(ImageView.ScaleType.CENTER_CROP);
            child.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onBannerItemClickListener != null)
                        onBannerItemClickListener.onBannerClick(index, bannerInfos);
                }
            });
            container.addView(child);
            return child;
        }
    }

    public static class BannerInfo {
        public int resId;
        public String title;
        public String url;

        public BannerInfo(int resId, String title) {
            this.resId = resId;
            this.title = title;
        }

        public BannerInfo(int resId, String title, String url) {
            this.resId = resId;
            this.title = title;
            this.url = url;
        }
    }


    public interface OnBannerItemClickListener {
        void onBannerClick(int index, ArrayList<BannerInfo> banner);
    }

    public int getLoop_ms() {
        if (loop_ms < 1500)
            loop_ms = 1500;
        return loop_ms;
    }

    /**
     * loop speed
     *
     * @param loop_ms (ms)
     */
    public void setLoop_ms(int loop_ms) {
        this.loop_ms = loop_ms;
    }

    /**
     * loop rate
     *
     * @param loop_duration (ms)
     */
    public void setLoop_duration(int loop_duration) {
        this.loop_duration = loop_duration;
    }

    /**
     * loop style
     *
     * @param loop_style (enum values[-1:empty,1:depth 2:zoom])
     */
    public void setLoop_style(LoopStyle loop_style) {
        this.loop_style = loop_style.getValue();
    }

    /**
     * startLoop
     */
    public void startLoop() {
        handler.removeCallbacksAndMessages(MESSAGE_LOOP);
        handler.sendEmptyMessageDelayed(MESSAGE_LOOP, getLoop_ms());
        L.e("LoopViewPager ---> startLoop");
    }

    /**
     * stopLoop
     * Be sure to in onDestory,To prevent a memory leak
     */
    public void stopLoop() {
        handler.removeMessages(MESSAGE_LOOP);
        L.e("LoopViewPager ---> stopLoop");
    }


    /**
     * Be sure to initialize the View
     */
    public void initializeView() {
        L.e("LoopViewPager ---> initializeView");
        float density = getResources().getDisplayMetrics().density;

        loopViewPager = new ViewPager(getContext());
        LayoutParams loop_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addView(loopViewPager, loop_params);

        indicatorLayout = new LinearLayout(getContext());
        LayoutParams ind_params = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ind_params.leftMargin = (int) (10 * density);
        ind_params.topMargin = (int) (10 * density);
        ind_params.rightMargin = (int) (10 * density);
        ind_params.bottomMargin = (int) (10 * density);
        ind_params.addRule(RelativeLayout.CENTER_IN_PARENT);//android:layout_centerInParent="true"
        ind_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);//android:layout_alignParentBottom="true"

        indicatorLayout.setGravity(Gravity.CENTER);

        indicatorLayout.setOrientation(LinearLayout.HORIZONTAL);
        addView(indicatorLayout, ind_params);
    }

    /**
     * Be sure to initialize the Data
     *
     * @param context context
     */
    public void initializeData(Context context) {
        L.e("LoopViewPager ---> initViewPager");
        //TODO To prevent the flower screen
        if (loop_duration > loop_ms)
            loop_duration = loop_ms;

        //TODO loop_duration
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            LoopScroller mScroller = new LoopScroller(context, new AccelerateInterpolator());
            //可以用setDuration的方式调整速率
            mScroller.setmDuration(loop_duration);
            mField.set(loopViewPager, mScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO loop_style
        if (loop_style == 1) {
            loopViewPager.setPageTransformer(true, new DepthPageTransformer());
        } else if (loop_style == 2) {
            loopViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        }

        //TODO Listener
        loopViewPager.setOnTouchListener(this);
        L.e("LoopViewPager init");
    }


    /**
     * ViewPager-onTouch
     *
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                L.e("ACTION_DOWN");
                stopLoop();
                break;
            case MotionEvent.ACTION_MOVE:
                L.e("ACTION_MOVE");
                stopLoop();
                break;
            case MotionEvent.ACTION_UP:
                L.e("ACTION_UP");
                startLoop();
                break;
            default:
                break;
        }
        return false;
    }
}
