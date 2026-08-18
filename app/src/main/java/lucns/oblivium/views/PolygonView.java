package lucns.oblivium.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Random;

import lucns.oblivium.R;

public class PolygonView extends View {

    public static final int TYPE_SQUARE = 0;
    public static final int TYPE_TRIANGLE = 1;
    public static final int TYPE_CIRCLE = 2;

    private Paint paint;
    private ObjectAnimator animationRotation, animationScaleX, animationScaleY;
    private int currentAngle;
    private float currentScaleX = 1;
    private float currentScaleY = 1;
    private int width;
    private boolean reverse;
    private int type;

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) init();
    }

    public PolygonView(Context context, int type, boolean reverse) {
        super(context);
        this.type = type;
        this.reverse = reverse;
        if (!isInEditMode()) init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(getContext().getColor(R.color.polygon_blurred_color));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true); // Smooths out the edges

        animationRotation = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 1f);
        animationRotation.setDuration(13500);
        animationRotation.setInterpolator(new AccelerateDecelerateInterpolator());
        animationRotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAttachedToWindow()) startRotation();
            }
        });

        animationScaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1f);
        animationScaleX.setDuration(7500);
        animationScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        animationScaleX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAttachedToWindow()) startScaleX();
            }
        });

        animationScaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1f);
        animationScaleY.setDuration(10000);
        animationScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        animationScaleY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAttachedToWindow()) startScaleY();
            }
        });

        post(new Runnable() {
            @Override
            public void run() {
                width = getMeasuredWidth();
                ViewGroup.LayoutParams params = getLayoutParams();
                params.width = width;
                params.height = width;
                setLayoutParams(params);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (type != TYPE_CIRCLE)startRotation();
        startScaleX();
        startScaleY();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animationRotation.cancel();
        animationScaleX.cancel();
        animationScaleY.cancel();
    }

    private void startRotation() {
        int angle = new Random().nextInt(60) + 30 + currentAngle;
        if (angle > 359) angle = angle % 360;
        animationRotation.setFloatValues(currentAngle, angle);
        if (reverse) animationRotation.start();
        else animationRotation.start();
        currentAngle = angle;
    }

    private void startScaleX() {
        float scale;
        if (type == TYPE_CIRCLE) scale = (new Random().nextInt(25) + 25) / 100f;
        else scale = (new Random().nextInt(100) + 100) / 100f;
        animationScaleX.setFloatValues(currentScaleX, scale);
        animationScaleX.start();
        currentScaleX = scale;
    }

    private void startScaleY() {
        float scale = (new Random().nextInt(100) + 100) / 100f;
        animationScaleY.setFloatValues(currentScaleY, scale);
        animationScaleY.start();
        currentScaleY = scale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int paddingX = (width / 3);
        int paddingY = width / 10;

        switch (type) {
            case TYPE_SQUARE:
                float w = width / 2f;
                float h = width;
                canvas.drawRect((width - w) / 2f, 0, w, h, paint);
                break;
            case TYPE_CIRCLE:
                canvas.drawCircle(width / 2f, width / 2f, width / 2f, paint);
                break;
            case TYPE_TRIANGLE:
                Path path = new Path();
                path.moveTo(paddingX, paddingY);
                path.lineTo(width - paddingX, paddingY);
                path.lineTo(width / 2f, width);
                path.close();
                canvas.drawPath(path, paint);
                break;
        }

        super.onDraw(canvas);
    }
}
