package com.nkrhelper.mylibrary.carousel;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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
    private int mCarouselOrientation;
    private boolean mAutoPlayEnabled;
    private Observable<Long> mAutoPlayObservable;
    private Disposable mAutoPlayDisposable;
    private long mAutoPlayInterval;
    private int mAutoPlayDirection;

    private IndicatorView mIndicatorView;

    public CarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // inflate layout

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_carousel, this, true);

        // properties

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CarouselView);

        int orientation = typedArray.getInteger(R.styleable.CarouselView_carousel_orientation, HORIZONTAL);
        if(orientation == HORIZONTAL) {
            mCarouselOrientation = LinearLayoutManager.HORIZONTAL;
        } else if(orientation == VERTICAL) {
            mCarouselOrientation = LinearLayoutManager.VERTICAL;
        }

        mAutoPlayEnabled = typedArray.getBoolean(R.styleable.CarouselView_carousel_auto_play, true);
        mAutoPlayInterval = typedArray.getInteger(R.styleable.CarouselView_carousel_auto_play_interval, 5000);
        mAutoPlayDirection = typedArray.getInteger(R.styleable.CarouselView_carousel_auto_play_direction, FORWARD);

        typedArray.recycle();

        // setup components

        setupCarouselRecyclerView(context);
        setupIndicatorView(context);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(visibility == GONE || visibility == INVISIBLE) {
            enableCarouseAutoPlay(false);
        } else if(visibility == VISIBLE) {
            enableCarouseAutoPlay(mAutoPlayEnabled);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        enableCarouseAutoPlay(mAutoPlayEnabled);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        enableCarouseAutoPlay(false);
    }

    private void setupCarouselRecyclerView(Context context) {
        // get recycler view

        mCarouselRecyclerView = (RecyclerView) getChildAt(0);

        // set layout manager

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, mCarouselOrientation, false);
        mCarouselRecyclerView.setLayoutManager(layoutManager);

        // set snap helper

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mCarouselRecyclerView);

        // set scroll listener

        mCarouselRecyclerView.addOnScrollListener(new CarouselScrollListener());

        // set touch listener

        mCarouselRecyclerView.setOnTouchListener(new CarouselTouchListener());
    }

    private void setupIndicatorView(Context context) {
        mIndicatorView = new IndicatorView(context);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // TODO: setup indicator properties

        layoutParams.gravity = Gravity.CENTER | Gravity.BOTTOM;

        mIndicatorView.setOrientation(LinearLayout.HORIZONTAL);
        addView(mIndicatorView, layoutParams);
    }

    public void enableCarouseAutoPlay(boolean autoPlay) {
        mAutoPlayEnabled = autoPlay;

        if(autoPlay) {
            if(mAutoPlayObservable == null) {
                mAutoPlayObservable = Observable.interval(mAutoPlayInterval, TimeUnit.MILLISECONDS);
            }

            if(mAutoPlayDisposable == null || mAutoPlayDisposable.isDisposed()){
                // start a new subscription

                mAutoPlayDisposable = mAutoPlayObservable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                                if(mAutoPlayDirection == FORWARD) {
                                    navigateNext();
                                } else {
                                    navigatePrevious();
                                }
                            }
                        });
            }
        } else {
            if(mAutoPlayDisposable != null && !mAutoPlayDisposable.isDisposed()) {
                mAutoPlayDisposable.dispose();
            }
        }
    }

    public void setCarouselOrientation(int orientation) {
        if(mCarouselOrientation != orientation) {
            mCarouselOrientation = orientation;
        }
    }

    public void setCarouselAutoPlayDirection(int direction) {
        mAutoPlayDirection = direction;
    }

    public void setCarouselAdapter(RecyclerView.Adapter adapter) {
        mCarouselRecyclerView.setAdapter(adapter);

        if(adapter.getItemCount() > 0) {
            mCarouselRecyclerView.scrollToPosition(1);
        }

        mIndicatorView.buildIndicators(adapter.getItemCount() - 2);
    }

    public void setCarouselAutoPlay(boolean autoPlay) {
        enableCarouseAutoPlay(autoPlay);
    }


    public void navigateNext() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mCarouselRecyclerView.getLayoutManager();
        mCarouselRecyclerView.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition() + 1);
    }

    public void navigatePrevious() {
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
                    enableCarouseAutoPlay(false);
                    break;
                }
                case MotionEvent.ACTION_UP : {
                    enableCarouseAutoPlay(true);
                    break;
                }
            }

            return false;
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

        public IndicatorView(Context context) {
            super(context);

            initIndicatorView(context);
        }

        private void initIndicatorView(Context context) {
            mIndicatorCount = 0;
            mCurrentPosition = 0;

            mNormalState = ContextCompat.getDrawable(context, R.drawable.ic_carousel_indicator_normal);
            mNormalState.setColorFilter(ContextCompat.getColor(context, android.R.color.white),
                    PorterDuff.Mode.SRC_ATOP);
            mSelectedState = ContextCompat.getDrawable(context, R.drawable.ic_carousel_indicator_selected);
            mSelectedState.setColorFilter(ContextCompat.getColor(context, android.R.color.white),
                    PorterDuff.Mode.SRC_ATOP);
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

            // TODO: rebuild indicators
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
                        28,
                        28);
                indicatorImg.setImageDrawable(mNormalState);
                addView(indicatorImg, layoutParams);
            }

            updateIndicatorState(mCurrentPosition, true);
        }
    }
}
