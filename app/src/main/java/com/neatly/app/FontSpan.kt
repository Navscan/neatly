package com.neatly.app

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Applies a custom Typeface to a span of text.
 *
 * Android's built-in TypefaceSpan(Typeface) constructor requires API 28.
 * This class provides the same behaviour for API 26 and 27 by overriding
 * the TextPaint in both the drawing and measuring passes.
 */
class FontSpan(private val typeface: Typeface) : MetricAffectingSpan() {

    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = typeface
    }
}
