package lucns.oblivium.views;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BlurEffectView extends FrameLayout {

    private Paint paint;

    public BlurEffectView(Context context) {
        super(context);
        initialize();
    }

    public BlurEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BlurEffectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public void initialize() {
        float blurRadius = 250;
        RenderEffect blurEffect = RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.CLAMP // Edge treatment policy
        );
        setRenderEffect(blurEffect);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        PolygonView polygon = new PolygonView(getContext(), PolygonView.TYPE_TRIANGLE, false);
        polygon.setAlpha(0.5f);
        polygon.setLayoutParams(params);
        PolygonView polygon2 = new PolygonView(getContext(), PolygonView.TYPE_SQUARE, true);
        polygon2.setAlpha(0.25f);
        polygon.setLayoutParams(params);
        addView(polygon);
        addView(polygon2);
    }
}
