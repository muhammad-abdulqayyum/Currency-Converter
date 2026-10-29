package com.example.muhammad.usd_converter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.provider.Settings;
import android.view.animation.LinearInterpolator;

/** One west-to-east revolution; returns to the same Americas-centered view. */
final class GlobeMotion {
    private final CategoryIconView icon;
    private final Rect visible = new Rect();
    private ValueAnimator animator;
    private boolean active, selected, completed;

    GlobeMotion(CategoryIconView icon) { this.icon = icon; }
    void setActive(boolean value) { active = value; refreshVisibility(); }
    void setSelected(boolean value) { selected = value; refreshVisibility(); }
    void replay() { if (animator != null) animator.cancel(); animator = null; completed = false; icon.setGlobeLongitude(-80); refreshVisibility(); }
    private boolean motionEnabled() {
        return Settings.Global.getFloat(icon.getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0;
    }
    void refreshVisibility() {
        if (!motionEnabled()) {
            if (animator != null) animator.cancel();
            animator = null; completed = true; icon.setGlobeLongitude(-80); return;
        }
        boolean shown = active && selected && icon.isShown() && icon.getGlobalVisibleRect(visible)
                && visible.height() >= icon.getHeight() / 2;
        if (!shown) { if (animator != null && animator.isStarted()) animator.pause(); return; }
        if (completed) return;
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(10000);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> icon.setGlobeLongitude(-80 - 360 * (float)a.getAnimatedValue()));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { completed = true; icon.setGlobeLongitude(-80); }
            });
            animator.start();
        } else if (animator.isPaused()) animator.resume();
    }
    void destroy() { if (animator != null) { animator.removeAllUpdateListeners(); animator.removeAllListeners(); animator.cancel(); animator = null; } }
}
