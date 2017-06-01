package com.nkrhelper.mylibrary.carousel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nkrhelper.mylibrary.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created 09/05/2017.
 */

public class CarouselView extends FrameLayout {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;

    private RecyclerView mCarouselRecyclerView;
    private AutoPlayController mAutoPlayController;
    private IndicatorView mIndicatorView;

    public CarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // inflate layout

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_carousel, this, true);

        // setup components

        setupCarouselRecyclerView(context, attrs);
        setupAutoPlayController(context, attrs);
        setupIndicatorView(context, attrs);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(visibility == GONE || visibility == INVISIBLE) {
            mAutoPlayController.pause();
        } else if(visibility == VISIBLE) {
            mAutoPlayController.resume();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAutoPlayController.resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAutoPlayController.pause();
    }

    private void setupCarouselRecyclerView(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CarouselView);
        int orientation = typedArray.getInteger(R.styleable.CarouselView_carousel_orientation, HORIZONTAL);
        int carouselOrientation = (orientation == HORIZONTAL ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL);
        typedArray.recycle();

        // get recycler view

        mCarouselRecyclerView = (RecyclerView) getChildAt(0);

        // set layout manager

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, carouselOrientation, false);
        mCarouselRecyclerView.setLayoutManager(layoutManager);

        // set snap helper

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mCarouselRecyclerView);

        // set scroll listener

        mCarouselRecyclerView.addOnScrollListener(new CarouselScrollListener());

        // set touch listener

        mCarouselRecyclerView.setOnTouchListener(new CarouselTouchListener());
    }

    private void setupAutoPlayController(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CarouselView);
        boolean autoPlayEnabled = typedArray.getBoolean(R.styleable.CarouselView_carousel_auto_play, true);
        int autoPlayInterval = typedArray.getInteger(R.styleable.CarouselView_carousel_auto_play_interval, 5000);
        int autoPlayDirection = typedArray.getInteger(R.styleable.CarouselView_carousel_auto_play_direction, FORWARD);
        typedArray.recycle();

        mAutoPlayController = new AutoPlayController(autoPlayInterval) {
            @Override
            void onPlayForward() {
                navigateForward();
            }

            @Override
            void onPlayBackward() {
                navigateBackward();
            }
        };

        mAutoPlayController.setPlayDirection(autoPlayDirection);

        if(autoPlayEnabled) {
            mAutoPlayController.start();
        }
    }

    private void setupIndicatorView(Context context, @Nullable AttributeSet attrs) {
        mIndicatorView = new IndicatorView(context);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // setup indicator properties

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CarouselView);
        int orientation = typedArray.getInteger(R.styleable.CarouselView_carousel_orientation, HORIZONTAL);
        int indicatorOrientation = (orientation == HORIZONTAL ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        boolean showIndicatorView = typedArray.getBoolean(R.styleable.CarouselView_show_indicators, true);
        int indicatorLayoutGravity = typedArray.getInteger(R.styleable.CarouselView_indicator_layout_gravity,
                Gravity.BOTTOM | Gravity.CENTER);
        int indicatorLayoutMarginLeft = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_layout_margin_left, 0);
        int indicatorLayoutMarginTop = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_layout_margin_top, 0);
        int indicatorLayoutMarginRight = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_layout_margin_right, 0);
        int indicatorLayoutMarginBottom = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_layout_margin_bottom, 0);
        int indicatorSize = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_size, 20);
        int indicatorMarginLeft = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_margin_left, 0);
        int indicatorMarginTop = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_margin_top, 0);
        int indicatorMarginRight = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_margin_right, 0);
        int indicatorMarginBottom = typedArray.getDimensionPixelSize(R.styleable.CarouselView_indicator_margin_bottom, 0);
        Drawable indicatorNormalState = typedArray.getDrawable(R.styleable.CarouselView_indicator_normal_state);
        if(indicatorNormalState == null) {
            indicatorNormalState = ContextCompat.getDrawable(context, R.drawable.ic_carousel_indicator_normal);
        }
        Drawable indicatorSelectedState = typedArray.getDrawable(R.styleable.CarouselView_indicator_selected_state);
        if(indicatorSelectedState == null) {
            indicatorSelectedState = ContextCompat.getDrawable(context, R.drawable.ic_carousel_indicator_selected);
        }
        int normalStateColor = typedArray.getColor(R.styleable.CarouselView_indicator_normal_state_color,
                ContextCompat.getColor(context, android.R.color.white));
        int selectedStateColor = typedArray.getColor(R.styleable.CarouselView_indicator_selected_state_color,
                ContextCompat.getColor(context, android.R.color.white));
        typedArray.recycle();

        layoutParams.gravity = indicatorLayoutGravity;
        layoutParams.setMargins(indicatorLayoutMarginLeft, indicatorLayoutMarginTop,
                indicatorLayoutMarginRight, indicatorLayoutMarginBottom);
        addView(mIndicatorView, layoutParams);

        mIndicatorView.setVisibility(showIndicatorView ? View.VISIBLE : View.GONE);
        mIndicatorView.setOrientation(indicatorOrientation);
        mIndicatorView.setIndicatorSize(indicatorSize);
        mIndicatorView.setIndicatorMargins(indicatorMarginLeft, indicatorMarginTop,
                indicatorMarginRight, indicatorMarginBottom);
        mIndicatorView.setIndicatorStateDrawables(indicatorNormalState, indicatorSelectedState);
        mIndicatorView.setIndicatorStateColors(normalStateColor, selectedStateColor);
    }

    public void setCarouselAdapter(RecyclerView.Adapter adapter) {
        mCarouselRecyclerView.setAdapter(adapter);

        if(adapter.getItemCount() > 0) {
            mCarouselRecyclerView.scrollToPosition(1);
        }

        mIndicatorView.buildIndicators(adapter.getItemCount() - 2);
    }

    public void navigateForward() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mCarouselRecyclerView.getLayoutManager();
        mCarouselRecyclerView.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition() + 1);
    }

    public void navigateBackward() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mCarouselRecyclerView.getLayoutManager();
        mCarouselRecyclerView.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition() - 1);
    }

    // carousel scroll logic

    private class CarouselScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int currentPosition = layoutManager.findFirstVisibleItemPosition();
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            int itemCount = adapter.getItemCount();
            int adapterPosition = currentPosition;

            if(itemCount <= 0) {
                return;
            }

            if(currentPosition == itemCount - 1) {
                // scrolled to last item, switch to the first item

                adapterPosition = 1;
                recyclerView.scrollToPosition(adapterPosition);
            } else if(currentPosition == 0) {
                // scrolled to the first dump item, switch to the last item

                adapterPosition = itemCount - 1;
                recyclerView.scrollToPosition(adapterPosition);
            }

            mIndicatorView.setIndicatorPosition(adapterPosition - 1);
        }
    }

    // carousel touch logic

    private class CarouselTouchListener implements RecyclerView.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN : {
                    mAutoPlayController.pause();
                    break;
                }
                case MotionEvent.ACTION_UP : {
                    mAutoPlayController.resume();
                    break;
                }
            }

            return false;
        }
    }

    // carousel auto play

    abstract class AutoPlayController {
        private boolean mEnabled;
        private Observable<Long> mObservable;
        private Disposable mDisposable;
        private long mInterval;
        private int mPlayDirection;

        AutoPlayController(int interval) {
            mInterval = interval;
        }

        abstract void onPlayForward();
        abstract void onPlayBackward();

        void start() {
            mEnabled = true;
            resume();
        }

        void resume() {
            if(mEnabled) {
                if(mObservable == null) {
                    mObservable = Observable.interval(mInterval, TimeUnit.MILLISECONDS);
                }

                if(mDisposable == null || mDisposable.isDisposed()){
                    // start a new subscription

                    mDisposable = mObservable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                                    if(mPlayDirection == FORWARD) {
                                        onPlayForward();
                                    } else if(mPlayDirection == BACKWARD) {
                                        onPlayBackward();
                                    }
                                }
                            });
                }
            }
        }

        void pause() {
            if(mDisposable != null && !mDisposable.isDisposed()) {
                mDisposable.dispose();
            }
        }

        void stop() {
            mEnabled = false;
            pause();
        }

        void setPlayDirection(int direction) {
            mPlayDirection = direction;
        }
    }

    // carousel adapter

    public static abstract class Adapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
        @Override
        public void onBindViewHolder(VH holder, int position) {
            onBindCarouselViewHolder(holder, getItemIndex(position));
        }

        private int getItemIndex(int position) {
            if(position == 0) {
                return getActualItemCount() - 1;
            } else if(position > 0 && position <= getActualItemCount()) {
                return position - 1;
            } else {
                return 0;
            }
        }

        @Override
        public int getItemCount() {
            return getActualItemCount() + 2;
        }

        public abstract void onBindCarouselViewHolder(VH holder, int index);
        public abstract int getActualItemCount();
    }

    // indicator

    class IndicatorView extends LinearLayout {
        private Drawable mNormalState;
        private Drawable mSelectedState;
        private int mIndicatorCount;
        private int mCurrentPosition;
        private int mIndicatorSize;
        private int mIndicatorMarginLeft;
        private int mIndicatorMarginTop;
        private int mIndicatorMarginRight;
        private int mIndicatorMarginBottom;

        public IndicatorView(Context context) {
            super(context);

            mIndicatorCount = 0;
            mCurrentPosition = 0;
        }

        private int updateIndicatorState(int position, boolean selected) {
            if(mIndicatorCount == 0) {
                return -1;
            }

            position %= mIndicatorCount;
            if(position >= 0  && position < getChildCount()) {
                ((AppCompatImageView) getChildAt(position)).setImageDrawable(
                        selected ? mSelectedState : mNormalState
                );

                return position;
            }

            return -1;
        }

        public void setIndicatorStateDrawables(Drawable normalState, Drawable selectedState) {
            mNormalState = normalState;
            mSelectedState = selectedState;
        }

        public void setIndicatorStateColors(int normalColor, int selectedColor) {
            mNormalState.setColorFilter(normalColor, PorterDuff.Mode.SRC_ATOP);
            mSelectedState.setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
        }

        public void setIndicatorSize(int size) {
            mIndicatorSize = size;
        }

        public void setIndicatorMargins(int left, int top, int right, int bottom) {
            mIndicatorMarginLeft = left;
            mIndicatorMarginTop = top;
            mIndicatorMarginRight = right;
            mIndicatorMarginBottom = bottom;
        }

        public void setIndicatorPosition(int position) {
            if(position != mCurrentPosition) {
                updateIndicatorState(mCurrentPosition, false);
                position = updateIndicatorState(position, true);
                if(position >= 0) {
                    mCurrentPosition = position;
                }
            }
        }

        public void buildIndicators(int count) {
            removeAllViews();

            mIndicatorCount = count;

            for(int i = 0; i < mIndicatorCount; i++) {
                AppCompatImageView indicatorImg = new AppCompatImageView(getContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        mIndicatorSize, mIndicatorSize);
                layoutParams.setMargins(mIndicatorMarginLeft, mIndicatorMarginTop,
                        mIndicatorMarginRight, mIndicatorMarginBottom);
                indicatorImg.setImageDrawable(mNormalState);
                addView(indicatorImg, layoutParams);
            }

            updateIndicatorState(mCurrentPosition, true);
        }
    }
}
