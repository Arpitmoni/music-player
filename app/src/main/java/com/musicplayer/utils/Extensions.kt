package com.musicplayer.utils

import android.view.View
import android.view.animation.DecelerateInterpolator

// ─────────────────────────────────────────────────────────────────────────
// Animation helpers
// ─────────────────────────────────────────────────────────────────────────

/** Fade a view in from alpha 0 → 1 */
fun View.animateFadeIn(duration: Long = 400) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

/** Slide a view up from 60dp below its natural position */
fun View.animateSlideUp(duration: Long = 350) {
    val startY = 60f * resources.displayMetrics.density
    translationY = startY
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .translationY(0f)
        .alpha(1f)
        .setDuration(duration)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

/** Scale in from 80% with a fade */
fun View.animateScaleIn(duration: Long = 300) {
    scaleX = 0.8f
    scaleY = 0.8f
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .scaleX(1f).scaleY(1f)
        .alpha(1f)
        .setDuration(duration)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

// ─────────────────────────────────────────────────────────────────────────
// Time formatting
// ─────────────────────────────────────────────────────────────────────────

/** Convert milliseconds to mm:ss string (e.g. 203000 → "3:23") */
fun formatMillis(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val minutes  = totalSec / 60
    val seconds  = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
