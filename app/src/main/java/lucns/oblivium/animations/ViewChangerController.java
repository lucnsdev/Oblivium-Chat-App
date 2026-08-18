package lucns.oblivium.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ViewChangerController {

    private View a, b;

    public ViewChangerController(View a, View b) {
        this.a = a;
        this.b = b;
    }

    public void change(boolean showLoading) {
        change(showLoading, null);
    }

    public void change(boolean showLoading, AnimatorListenerAdapter listener) {
            long duration = 300;
            ObjectAnimator alpha;
            if (showLoading) {
                b.setAlpha(0f);
                alpha = ObjectAnimator.ofFloat(a, View.ALPHA, 1f, 0f);
            } else {
                a.setAlpha(0f);
                alpha = ObjectAnimator.ofFloat(b, View.ALPHA, 1f, 0f);
            }
            a.setVisibility(View.VISIBLE);
            b.setVisibility(View.VISIBLE);
            alpha.setInterpolator(new LinearInterpolator());
            alpha.setDuration(duration);
            alpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator alpha;
                    if (showLoading) {
                        a.setVisibility(View.INVISIBLE);
                        alpha = ObjectAnimator.ofFloat(b, View.ALPHA, 0f, 1f);
                    } else {
                        b.setVisibility(View.INVISIBLE);
                        alpha = ObjectAnimator.ofFloat(a, View.ALPHA, 0f, 1f);
                    }
                    alpha.setInterpolator(new LinearInterpolator());
                    alpha.setDuration(duration);
                    if (listener != null) alpha.addListener(listener);
                    alpha.start();
                }
            });
            alpha.start();
    }
}
