package com.nkrhelper.mylibrary.carousel;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.nkrhelper.mylibrary.R;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public CarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // inflate layout

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_carousel, this, true);

        // setup properties

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

        // TODO: build indicators


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

            // TODO: update indicator position
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
}
