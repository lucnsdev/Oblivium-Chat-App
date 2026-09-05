package lucns.oblivium.views.slider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.LinkedList;
import java.util.Queue;

public class SliderView extends RelativeLayout {

    public interface OnSliderChangedListener {
        void onSlidePositionChanged(int index);
    }

    private Slider slider;
    private int initialHeight;
    private boolean in;
    protected OnSliderChangedListener onSliderChangedListener;

    public SliderView(Context context) {
        super(context);
        initialize();
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        slider = new Slider(getContext());
        slider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(slider);
        setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {

            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int keyboardHeight = insets.getInsets(WindowInsets.Type.ime()).bottom;
                reduceRoot(keyboardHeight);
                return insets;
            }
        });
    }

    public void reduceRoot(int keyboardHeight) {
        if (keyboardHeight > 0) {
            if (!in) initialHeight = getHeight();
            in = true;
            changeSize(initialHeight - keyboardHeight, true);
        } else if (in) {
            in = false;
            changeSize(initialHeight, false);
        }
    }

    private void changeSize(int height, boolean in) {
        LayoutParams p = (LayoutParams) getLayoutParams();
        ValueAnimator translate = new ValueAnimator();
        translate.setDuration(200);
        translate.setIntValues(getHeight(), height);
        translate.setInterpolator(in ? new DecelerateInterpolator() : new AccelerateInterpolator());
        translate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                p.height = (Integer) animation.getAnimatedValue();
                slider.setChildrenSize(getWidth(), p.height);
                setLayoutParams(p);
            }
        });
        translate.start();
    }

    public void disableScroll(boolean state) {
        slider.scrollState = !state;
    }

    public void setSpaceBetweenViews(int space) {
        slider.spaceBetweenViews = space;
    }

    public void setWidthViewsPercentage(int percentage) {
        slider.widthViewsPercentage = percentage;
    }

    public void setOnSliderChangedListener(OnSliderChangedListener listener) {
        onSliderChangedListener = listener;
    }

    public boolean onBackPressed() {
        FragmentView f = ((FragmentView) slider.getChildAt(slider.lastIndex));
        if (f == null) return true;
        return f.onBackPressed();
    }

    public void goToIndex(int index) {
        slider.goToIndex(index);
    }

    public void goToIndex(int index, int duration) {
        slider.goToIndex(index, duration);
    }

    public void removeAllFragments() {
        slider.removeAllFragments();
    }

    public void addFragment(FragmentView fragmentView) {
        slider.addFragment(fragmentView);
    }

    public FragmentView[] getFragments() {
        FragmentView[] fragmentViews = new FragmentView[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            fragmentViews[i] = (FragmentView) getChildAt(i);
        }
        return fragmentViews;
    }

    public FragmentView getFragment(int index) {
        return (FragmentView) getChildAt(index);
    }

    public FragmentView getFragment() {
        return (FragmentView) getChildAt(slider.getCurrentIndex());
    }

    private class Slider extends FrameLayout {

        private final int WIDTH_DISPLAY = Resources.getSystem().getDisplayMetrics().widthPixels;
        public final int HEIGHT_DISPLAY = Resources.getSystem().getDisplayMetrics().heightPixels;
        private boolean adjustedSizes;
        private int adjustedHeight;
        private int totalWidth;
        private int startX, startPosition;
        private boolean movementCanceled;

        private int touchedX;
        private int minimumSwipe;
        private boolean smoothScrolled, isSmoothScrolled, isIndexChanging;

        private static final int DURATION = 200;
        private ValueAnimator translate;
        protected boolean scrollState = true;
        private boolean layoutReady;

        private int requestedIndex;
        protected int index, lastIndex;
        protected int spaceBetweenViews, widthViewsPercentage;
        private Queue<View> queue;

        public Slider(Context context) {
            super(context);
            initialize();
        }

        public Slider(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize();
        }

        private void initialize() {
            minimumSwipe = WIDTH_DISPLAY / 10;
            queue = new LinkedList<>();
        }

        public void setChildrenSize(int width, int height) {
            for (int i = 0; i < getChildCount(); i++) {
                FragmentView child = (FragmentView) getChildAt(i);
                child.setLayoutParams(new LayoutParams(width, height));
            }
        }

        public void addFragment(View view) {
            if (getChildCount() > getCurrentIndex() + 10) {
                queue.add(view);
                return;
            }
            computeAndAddView(view);
        }

        private void computeAndAddView(View view) {
            int children = getChildCount();
            if (view instanceof FragmentView) {
                FragmentView fragment = ((FragmentView) view);
                fragment.setFragmentIndex(children);
                fragment.setSlider((SliderView) getParent());
                fragment.create();
                if (children == 0 || children == 1) fragment.resume();
            }

            int spaces = spaceBetweenViews == 0 ? 0 : children * spaceBetweenViews;
            totalWidth = (getWidthView() * (children + 1)) + spaces;
            int x = (getWidthView() * children) + (spaceBetweenViews * children);
            view.setX(x);
            addView(view);
        }

        public void removeAllFragments() {
            totalWidth = 0;
            queue.clear();
            removeAllViews();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            post(new Runnable() {
                public void run() {
                    adjustedHeight = getHeight();
                    //updateScroll();
                    setX(0);
                    //if (onAttachedToWindowListener != null) onAttachedToWindowListener.onAttached();
                }
            });
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            adjustedSizes = right > 0 && bottom > 0;
            updateScroll();
        }

        private void updateScroll() {
            if (adjustedHeight > 0 && adjustedSizes) {
                layoutReady = true;
                if (requestedIndex > 0) goToIndex(requestedIndex, true);
                requestedIndex = 0;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (adjustedHeight == 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            setMeasuredDimension(totalWidth, adjustedHeight);
            int width = widthViewsPercentage == 0 ? WIDTH_DISPLAY : ((int) (WIDTH_DISPLAY * (widthViewsPercentage / 100.0d)));
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedHeight, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (!scrollState) return super.dispatchTouchEvent(event);
            else super.dispatchTouchEvent(event);

            if (event.getPointerCount() > 1) {
                movementCanceled = true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    smoothScrolled = false;
                    isSmoothScrolled = false;
                    touchedX = (int) event.getRawX();
                    startX = (int) getX();
                    startPosition = (int) (event.getRawX() - getX());
                    index = getCurrentIndex();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (movementCanceled) break;
                    float position = event.getRawX() - startPosition;
                    if (position < ((totalWidth - getWidthView()) * (-1)) || position > 0) break;
                    int moved = (((int) event.getRawX()) - touchedX);
                    if (isSmoothScrolled) {
                        setX(position);
                    } else {
                        if (moved > (minimumSwipe / 2) || moved < -(minimumSwipe / 2)) {
                            if (!smoothScrolled) {
                                smoothScrolled = true;
                                smoothScroll((int) position);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (movementCanceled) {
                        if (event.getPointerCount() == 1) movementCanceled = false;
                        break;
                    }
                    if (startX - ((int) getX()) > minimumSwipe * 2) {
                        index++;
                        goToIndex(index, 150, true);
                        break;
                    } else if (startX - ((int) getX()) < -minimumSwipe * 2) {
                        index--;
                        goToIndex(index, 150, true);
                        break;
                    }
                    goToIndex(index, true);
                    break;
            }
            return true;
        }

        private void smoothScroll(int x) {
            translate = new ValueAnimator();
            translate.setDuration(100);
            translate.setIntValues((int) getX(), x);
            translate.setInterpolator(new AccelerateInterpolator());
            translate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    isSmoothScrolled = true;
                    int value = (Integer) animation.getAnimatedValue();
                    setX(value);
                }
            });
            translate.start();
        }

        private int getWidthView() {
            return widthViewsPercentage == 0 ? WIDTH_DISPLAY : ((int) (WIDTH_DISPLAY * (widthViewsPercentage / 100.0d)));
        }

        public int getIndexOfX(int x) {
            if (widthViewsPercentage == 0) {
                return x / getWidthView();
            } else if (spaceBetweenViews == 0) {
                return x / getWidthView();
            } else {
                int index = 0;
                int a = x * (-1);
                for (int i = 0; i < getChildCount(); i++) {
                    int position = (getWidthView() * i) + (spaceBetweenViews * i);
                    if (a <= position) break;
                    index++;
                }
                return index;
            }
        }

        public int getCurrentIndex() {
            return getIndexOfX(((int) getX()) * (-1));
        }

        public void goToIndex(int index) {
            if (index < 0 || index > getChildCount() - 1 || index == getCurrentIndex()) return;
            FragmentView fragment = (FragmentView) getChildAt(index);
            fragment.requestFocus();
            fragment.onResume();
            ((FragmentView) getChildAt(getCurrentIndex())).onPause();
            animateTo(index * getWidthView() * (-1) - (spaceBetweenViews * index), DURATION, false);
        }

        public void goToIndex(int index, int duration) {
            if (!layoutReady) {
                requestedIndex = index;
                return;
            }
            if (index < 0 || index > getChildCount() - 1) return;
            animateTo(index * getWidthView() * (-1) - (spaceBetweenViews * index), duration, false);
        }

        private void goToIndex(int index, boolean byUser) {
            if (index < 0 || index > getChildCount() - 1) return;
            animateTo(index * getWidthView() * (-1) - (spaceBetweenViews * index), DURATION, byUser);
        }

        private void goToIndex(int index, int duration, boolean byUser) {
            if (index < 0 || index > getChildCount() - 1) return;
            animateTo(index * getWidthView() * (-1) - (spaceBetweenViews * index), duration, byUser);
        }

        public void cancelAnimation() {
            if (translate != null && translate.isRunning()) translate.cancel();
        }

        public boolean isIndexChanging() {
            return isIndexChanging;
        }

        private void animateTo(int x, int duration, boolean byUser) {
            isIndexChanging = true;
            cancelAnimation();
            if (!queue.isEmpty()) {
                int c = getChildCount();
                int i = getIndexOfX(x);
                while (c < i + 10) {
                    if (queue.isEmpty()) break;
                    computeAndAddView(queue.remove());
                    c = getChildCount();
                    i = getIndexOfX(x);
                }
            }

            translate = new ValueAnimator();
            translate.setDuration(duration);
            translate.setIntValues((int) getX(), x);
            translate.setInterpolator(new DecelerateInterpolator());
            translate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (Integer) animation.getAnimatedValue();
                    setX(value);
                }
            });
            translate.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    isIndexChanging = false;
                    int index = getCurrentIndex();
                    if (byUser && lastIndex != index) {
                        if (index > lastIndex) {
                            if (lastIndex - 1 >= 0) ((FragmentView) getChildAt(lastIndex - 1)).pause();
                            if (index + 1 < getChildCount()) ((FragmentView) getChildAt(index + 1)).resume();
                        } else {
                            if (index - 1 >= 0) ((FragmentView) getChildAt(index - 1)).resume();
                            if (lastIndex + 1 < getChildCount()) ((FragmentView) getChildAt(lastIndex + 1)).pause();
                        }

                        if (onSliderChangedListener != null) onSliderChangedListener.onSlidePositionChanged(index);
                    }
                    lastIndex = index;
                }
            });
            translate.start();
        }
    }

}
