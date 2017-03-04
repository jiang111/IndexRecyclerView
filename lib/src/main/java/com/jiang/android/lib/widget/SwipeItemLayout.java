
package com.jiang.android.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import com.jiang.android.lib.R;

import java.lang.reflect.Method;


public class SwipeItemLayout extends RelativeLayout {

    private static final String TAG = SwipeItemLayout.class.getSimpleName();
    private static final String INSTANCE_STATUS = "instance_status";
    private static final String STATUS_OPEN_CLOSE = "status_open_close";
    private static final int VEL_THRESHOLD = 400;
    private ViewDragHelper mDragHelper;
    // 顶部视图
    private View mTopView;
    // 底部视图
    private View mBottomView;
    // 拖动的弹簧距离
    private int mSpringDistance = 0;
    // 允许拖动的距离【注意：最终允许拖动的距离是 (mDragRange + mSpringDistance)】
    private int mDragRange;
    // 控件滑动方向（向左，向右），默认向左滑动
    private SwipeDirection mSwipeDirection = SwipeDirection.Left;
    // 移动过程中，底部视图的移动方式（拉出，被顶部视图遮住），默认是被顶部视图遮住
    private BottomModel mBottomModel = BottomModel.PullOut;
    // 滑动控件当前的状态（打开，关闭，正在移动），默认是关闭状态
    private Status mCurrentStatus = Status.Closed;
    // 滑动控件滑动前的状态
    private Status mPreStatus = mCurrentStatus;
    // 顶部视图下一次layout时的left
    private int mTopLeft;
    // 顶部视图外边距
    private MarginLayoutParams mTopLp;
    // 底部视图外边距
    private MarginLayoutParams mBottomLp;
    // 滑动比例，【关闭->展开  =>  0->1】
    private float mDragRatio;
    // 手动拖动打开和关闭代理
    private SwipeItemLayoutDelegate mDelegate;

    private GestureDetectorCompat mGestureDetectorCompat;
    private OnLongClickListener mOnLongClickListener;
    private OnClickListener mOnClickListener;
    /**
     * 是否可滑动
     */
    private boolean mSwipeable = true;

    public SwipeItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initProperty();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BGASwipeItemLayout);
        final int N = typedArray.getIndexCount();
        for (int i = 0; i < N; i++) {
            initAttr(typedArray.getIndex(i), typedArray);
        }
        typedArray.recycle();
    }

    private void initAttr(int attr, TypedArray typedArray) {
        if (attr == R.styleable.BGASwipeItemLayout_bga_sil_swipeDirection) {
            // 默认向左滑动
            int leftSwipeDirection = typedArray.getInt(attr, mSwipeDirection.ordinal());

            if (leftSwipeDirection == SwipeDirection.Right.ordinal()) {
                mSwipeDirection = SwipeDirection.Right;
            }
        } else if (attr == R.styleable.BGASwipeItemLayout_bga_sil_bottomMode) {
            // 默认是拉出
            int pullOutBottomMode = typedArray.getInt(attr, mBottomModel.ordinal());

            if (pullOutBottomMode == BottomModel.LayDown.ordinal()) {
                mBottomModel = BottomModel.LayDown;
            }
        } else if (attr == R.styleable.BGASwipeItemLayout_bga_sil_springDistance) {
            // 弹簧距离，不能小于0，默认值为0
            mSpringDistance = typedArray.getDimensionPixelSize(attr, mSpringDistance);
            if (mSpringDistance < 0) {
                throw new IllegalStateException("bga_sil_springDistance不能小于0");
            }
        } else if (attr == R.styleable.BGASwipeItemLayout_bga_sil_swipeAble) {
            mSwipeable = typedArray.getBoolean(attr, mSwipeable);
        }
    }

    private void initProperty() {
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        mGestureDetectorCompat = new GestureDetectorCompat(getContext(), mSimpleOnGestureListener);
    }

    public void setDelegate(SwipeItemLayoutDelegate delegate) {
        mDelegate = delegate;
    }

    public void setSwipeAble(boolean swipeAble) {
        mSwipeable = swipeAble;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalStateException(SwipeItemLayout.class.getSimpleName() + "必须有且只有两个子控件");
        }
        mTopView = getChildAt(1);
        mBottomView = getChildAt(0);
        // 避免底部视图被隐藏时还能获取焦点被点击
        mBottomView.setVisibility(INVISIBLE);

        mTopLp = (MarginLayoutParams) mTopView.getLayoutParams();
        mBottomLp = (MarginLayoutParams) mBottomView.getLayoutParams();
        mTopLeft = getPaddingLeft() + mTopLp.leftMargin;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
        }
        return mDragHelper.shouldInterceptTouchEvent(ev) && mGestureDetectorCompat.onTouchEvent(ev);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
        mOnClickListener = l;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        super.setOnLongClickListener(l);
        mOnLongClickListener = l;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (insideAdapterView()) {
            if (mOnClickListener == null) {
                setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performAdapterViewItemClick();
                    }
                });
            }
            if (mOnLongClickListener == null) {
                setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        performAdapterViewItemLongClick();
                        return true;
                    }
                });
            }
        }
    }

    private void performAdapterViewItemClick() {
        ViewParent t = getParent();
        if (t instanceof AdapterView) {
            AdapterView view = (AdapterView) t;
            int p = view.getPositionForView(SwipeItemLayout.this);
            if (p != AdapterView.INVALID_POSITION) {
                view.performItemClick(view.getChildAt(p - view.getFirstVisiblePosition()), p, view.getAdapter().getItemId(p));
            }
        }
    }

    private boolean performAdapterViewItemLongClick() {
        ViewParent t = getParent();
        if (t instanceof AdapterView) {
            AdapterView view = (AdapterView) t;
            int p = view.getPositionForView(SwipeItemLayout.this);
            if (p == AdapterView.INVALID_POSITION) return false;
            long vId = view.getItemIdAtPosition(p);
            boolean handled = false;
            try {
                Method m = AbsListView.class.getDeclaredMethod("performLongPress", View.class, int.class, long.class);
                m.setAccessible(true);
                handled = (boolean) m.invoke(view, SwipeItemLayout.this, p, vId);

            } catch (Exception e) {
                e.printStackTrace();

                if (view.getOnItemLongClickListener() != null) {
                    handled = view.getOnItemLongClickListener().onItemLongClick(view, SwipeItemLayout.this, p, vId);
                }
                if (handled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
            }
            return handled;
        }
        return false;
    }

    private boolean insideAdapterView() {
        return getAdapterView() != null;
    }

    private AdapterView getAdapterView() {
        ViewParent t = getParent();
        if (t instanceof AdapterView) {
            return (AdapterView) t;
        }
        return null;
    }

    private void requestParentDisallowInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                requestParentDisallowInterceptTouchEvent();
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                requestParentDisallowInterceptTouchEvent();
                return true;
            }
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // 2
            setPressed(false);
            if (isClosed()) {
                return performClick();
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // 1
            if (isClosed()) {
                setPressed(true);
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (isClosed()) {
                setPressed(true);
                postDelayed(mCancelPressedTask, 300);
                performLongClick();
            }
        }

        // 作为ListView或者RecyclerView的item，双击事件很少，这里就不处理双击事件了╮(╯_╰)╭
        public boolean onDoubleTap(MotionEvent e) {
            if (isClosed()) {
                setPressed(true);
                return true;
            }
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            if (isClosed()) {
                setPressed(false);
                return true;
            }
            return false;
        }
    };

    private Runnable mCancelPressedTask = new Runnable() {
        @Override
        public void run() {
            setPressed(false);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        mGestureDetectorCompat.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mDragRange = mBottomView.getMeasuredWidth() + mBottomLp.leftMargin + mBottomLp.rightMargin;

        int topTop = getPaddingTop() + mTopLp.topMargin;
        int topBottom = topTop + mTopView.getMeasuredHeight();
        int topRight = mTopLeft + mTopView.getMeasuredWidth();

        int bottomTop = getPaddingTop() + mBottomLp.topMargin;
        int bottomBottom = bottomTop + mBottomView.getMeasuredHeight();
        int bottomLeft;
        int bottomRight;

        if (mSwipeDirection == SwipeDirection.Left) {
            // 向左滑动

            if (mBottomModel == BottomModel.LayDown) {
                // 遮罩，位置固定不变（先计算right，然后根据right计算left）

                bottomRight = r - getPaddingRight() - mBottomLp.rightMargin;
                bottomLeft = bottomRight - mBottomView.getMeasuredWidth();
            } else {
                // 拉出，位置随顶部视图的位置改变

                // 根据顶部视图的left计算底部视图的left
                bottomLeft = mTopLeft + mTopView.getMeasuredWidth() + mTopLp.rightMargin + mBottomLp.leftMargin;

                // 底部视图的left被允许的最小值
                int minBottomLeft = r - getPaddingRight() - mBottomView.getMeasuredWidth() - mBottomLp.rightMargin;
                // 获取最终的left
                bottomLeft = Math.max(bottomLeft, minBottomLeft);
                // 根据left计算right
                bottomRight = bottomLeft + mBottomView.getMeasuredWidth();
            }
        } else {
            // 向右滑动

            if (mBottomModel == BottomModel.LayDown) {
                // 遮罩，位置固定不变（先计算left，然后根据left计算right）

                bottomLeft = getPaddingLeft() + mBottomLp.leftMargin;
                bottomRight = bottomLeft + mBottomView.getMeasuredWidth();
            } else {
                // 拉出，位置随顶部视图的位置改变

                // 根据顶部视图的left计算底部视图的left
                bottomLeft = mTopLeft - mDragRange;
                // 底部视图的left被允许的最大值
                int maxBottomLeft = getPaddingLeft() + mBottomLp.leftMargin;
                // 获取最终的left
                bottomLeft = Math.min(maxBottomLeft, bottomLeft);
                // 根据left计算right
                bottomRight = bottomLeft + mBottomView.getMeasuredWidth();
            }
        }

        mBottomView.layout(bottomLeft, bottomTop, bottomRight, bottomBottom);
        mTopView.layout(mTopLeft, topTop, topRight, topBottom);
    }

    /**
     * 以动画方式打开
     */
    public void openWithAnim() {
        mPreStatus = Status.Moving;
        smoothSlideTo(1);
    }

    /**
     * 以动画方式关闭
     */
    public void closeWithAnim() {
        mPreStatus = Status.Moving;
        smoothSlideTo(0);
    }

    /**
     * 直接打开
     */
    public void open() {
        mPreStatus = Status.Moving;
        slideTo(1);
    }

    /**
     * 直接关闭。如果在AbsListView中删除已经打开的item时，请用该方法关闭item，否则重用item时有问题。RecyclerView中可以用该方法，也可以用closeWithAnim
     */
    public void close() {
        mPreStatus = Status.Moving;
        slideTo(0);
    }

    /**
     * 当前是否为打开状态
     *
     * @return
     */
    public boolean isOpened() {
        return (mCurrentStatus == Status.Opened) || (mCurrentStatus == Status.Moving && mPreStatus == Status.Opened);
    }

    /**
     * 当前是否为关闭状态
     *
     * @return
     */
    public boolean isClosed() {
        return mCurrentStatus == Status.Closed || (mCurrentStatus == Status.Moving && mPreStatus == Status.Closed);
    }

    /**
     * 获取顶部视图
     *
     * @return
     */
    public View getTopView() {
        return mTopView;
    }

    /**
     * 获取底部视图
     *
     * @return
     */
    public View getBottomView() {
        return mBottomView;
    }

    /**
     * 打开或关闭滑动控件
     *
     * @param isOpen 1表示打开，0表示关闭
     */
    private void smoothSlideTo(int isOpen) {
        if (mDragHelper.smoothSlideViewTo(mTopView, getCloseOrOpenTopViewFinalLeft(isOpen), getPaddingTop() + mTopLp.topMargin)) {
            if(isOpen == 1){
                mBottomView.setVisibility(VISIBLE);
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * 打开或关闭滑动控件
     *
     * @param isOpen 1表示打开，0表示关闭
     */
    private void slideTo(int isOpen) {
        if (isOpen == 1) {
            mBottomView.setVisibility(VISIBLE);
            ViewCompat.setAlpha(mBottomView, 1.0f);
            mCurrentStatus = Status.Opened;
            if (mDelegate != null) {
                mDelegate.onSwipeItemLayoutOpened(this);
            }
        } else {
            mBottomView.setVisibility(INVISIBLE);
            mCurrentStatus = Status.Closed;
            if (mDelegate != null) {
                mDelegate.onSwipeItemLayoutClosed(this);
            }
        }
        mPreStatus = mCurrentStatus;
        mTopLeft = getCloseOrOpenTopViewFinalLeft(isOpen);
        requestLayout();
    }

    private int getCloseOrOpenTopViewFinalLeft(int isOpen) {
        int left = getPaddingLeft() + mTopLp.leftMargin;
        if (mSwipeDirection == SwipeDirection.Left) {
            left = left - isOpen * mDragRange;
        } else {
            left = left + isOpen * mDragRange;
        }
        return left;
    }

    private int getCloseOrOpenBottomViewFinalLeft(int isOpen) {
        int left = getWidth() - mBottomView.getWidth();
        if (mSwipeDirection == SwipeDirection.Left) {
            left = left - isOpen * mDragRange;
        } else {
            left = left + isOpen * mDragRange;
        }
        return left;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATUS, super.onSaveInstanceState());
        bundle.putInt(STATUS_OPEN_CLOSE, mCurrentStatus.ordinal());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            if (bundle.getInt(STATUS_OPEN_CLOSE) == Status.Opened.ordinal()) {
                open();
            } else {
                close();
            }
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATUS));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return mSwipeable && child == mTopView;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return 0;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // 这里要返回控件的getPaddingTop() + mTopLp.topMargin，否则有margin和padding快速滑动松手时会上下跳动
            return getPaddingTop() + mTopLp.topMargin;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mDragRange + mSpringDistance;
        }

        /**
         *
         * @param child
         * @param left ViewDragHelper帮我们计算的当前所捕获的控件的left
         * @param dx
         * @return
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int minTopLeft;
            int maxTopLeft;

            if (mSwipeDirection == SwipeDirection.Left) {
                // 向左滑动

                // 顶部视图的left被允许的最小值
                minTopLeft = getPaddingLeft() + mTopLp.leftMargin - (mDragRange + mSpringDistance);
                // 顶部视图的left被允许的最大值
                maxTopLeft = getPaddingLeft() + mTopLp.leftMargin;
            } else {
                // 向右滑动

                // 顶部视图的left被允许的最小值
                minTopLeft = getPaddingLeft() + mTopLp.leftMargin;
                // 顶部视图的left被允许的最大值
                maxTopLeft = getPaddingLeft() + mTopLp.leftMargin + (mDragRange + mSpringDistance);
            }

            return Math.min(Math.max(minTopLeft, left), maxTopLeft);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mTopLeft = left;

            // 此时顶部视图水平方向偏移量的绝对值
            int topViewHorizontalOffset = Math.abs(mTopLeft - (getPaddingLeft() + mTopLp.leftMargin));
            if (topViewHorizontalOffset > mDragRange) {
                mDragRatio = 1.0f;
            } else {
                mDragRatio = 1.0f * topViewHorizontalOffset / mDragRange;
            }

            // 处理底部视图的透明度
            float alpha = 0.1f + 0.9f * mDragRatio;
            ViewCompat.setAlpha(mBottomView, alpha);

            dispatchSwipeEvent();

            // 兼容低版本
            invalidate();

            requestLayout();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            // 默认关闭，接下来再判断为打开时的条件
            int finalLeft = getPaddingLeft() + mTopLp.leftMargin;
            if (mSwipeDirection == SwipeDirection.Left) {
                // 向左滑动为打开，向右滑动为关闭

                if (xvel < -VEL_THRESHOLD || (mPreStatus == Status.Closed && xvel < VEL_THRESHOLD && mDragRatio >= 0.3f) || (mPreStatus == Status.Opened && xvel < VEL_THRESHOLD && mDragRatio >= 0.7f)) {
                    // 向左的速度达到条件
                    finalLeft -= mDragRange;
                }

            } else {
                // 向左滑动为关闭，向右滑动为打开

                if (xvel > VEL_THRESHOLD || (mPreStatus == Status.Closed && xvel > -VEL_THRESHOLD && mDragRatio >= 0.3f) || (mPreStatus == Status.Opened && xvel > -VEL_THRESHOLD && mDragRatio >= 0.7f)) {
                    finalLeft += mDragRange;
                }
            }
            mDragHelper.settleCapturedViewAt(finalLeft, getPaddingTop() + mTopLp.topMargin);

            // 要执行下面的代码，不然不会自动收缩完毕或展开完毕
            ViewCompat.postInvalidateOnAnimation(SwipeItemLayout.this);
        }

    };

    private void dispatchSwipeEvent() {
        Status preStatus = mCurrentStatus;
        updateCurrentStatus();
        if (mCurrentStatus != preStatus) {
            if (mCurrentStatus == Status.Closed) {
                mBottomView.setVisibility(INVISIBLE);
                if (mDelegate != null && mPreStatus != mCurrentStatus) {
                    mDelegate.onSwipeItemLayoutClosed(this);
                }
                mPreStatus = Status.Closed;
            } else if (mCurrentStatus == Status.Opened) {
                if (mDelegate != null && mPreStatus != mCurrentStatus) {
                    mDelegate.onSwipeItemLayoutOpened(this);
                }
                mPreStatus = Status.Opened;
            } else if (mPreStatus == Status.Closed) {
                mBottomView.setVisibility(VISIBLE);
                if (mDelegate != null) {
                    mDelegate.onSwipeItemLayoutStartOpen(this);
                }
            }
        }
    }

    private void updateCurrentStatus() {
        if (mSwipeDirection == SwipeDirection.Left) {
            // 向左滑动

            if (mTopLeft == getPaddingLeft() + mTopLp.leftMargin - mDragRange) {
                mCurrentStatus = Status.Opened;
            } else if (mTopLeft == getPaddingLeft() + mTopLp.leftMargin) {
                mCurrentStatus = Status.Closed;
            } else {
                mCurrentStatus = Status.Moving;
            }
        } else {
            // 向右滑动

            if (mTopLeft == getPaddingLeft() + mTopLp.leftMargin + mDragRange) {
                mCurrentStatus = Status.Opened;
            } else if (mTopLeft == getPaddingLeft() + mTopLp.leftMargin) {
                mCurrentStatus = Status.Closed;
            } else {
                mCurrentStatus = Status.Moving;
            }
        }
    }


    public enum SwipeDirection {
        Left, Right
    }

    public enum BottomModel {
        PullOut, LayDown
    }

    public enum Status {
        Opened, Closed, Moving
    }

    public interface SwipeItemLayoutDelegate {
        /**
         * 变为打开状态
         *
         * @param swipeItemLayout
         */
        void onSwipeItemLayoutOpened(SwipeItemLayout swipeItemLayout);

        /**
         * 变为关闭状态
         *
         * @param swipeItemLayout
         */
        void onSwipeItemLayoutClosed(SwipeItemLayout swipeItemLayout);

        /**
         * 从关闭状态切换到正在打开状态
         *
         * @param swipeItemLayout
         */
        void onSwipeItemLayoutStartOpen(SwipeItemLayout swipeItemLayout);
    }

}
