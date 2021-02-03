package com.videodac.publisher.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.LruCache


/**
 * Style a [Spannable] with a custom [Typeface].
 *
 * @author Mulili Nzuki
 */
class TypefaceSpan(context: Context, typefaceName: String?) :
    MetricAffectingSpan() {
    private var mTypeface: Typeface?
    override fun updateMeasureState(p: TextPaint) {
        p.typeface = mTypeface

        // Note: This flag is required for proper typeface rendering
        p.flags = p.flags or Paint.SUBPIXEL_TEXT_FLAG
    }

    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = mTypeface

        // Note: This flag is required for proper typeface rendering
        tp.flags = tp.flags or Paint.SUBPIXEL_TEXT_FLAG
    }

    companion object {
        /** An `LruCache` for previously loaded typefaces.  */
        private val sTypefaceCache: LruCache<String, Typeface> = LruCache<String, Typeface>(12)
    }

    /**
     * Load the [Typeface] and apply to a [Spannable].
     */
    init {
        mTypeface = sTypefaceCache.get(typefaceName)
        if (mTypeface == null) {
            mTypeface = Typeface.createFromAsset(
                context.applicationContext
                    .assets, String.format("fonts/%s", typefaceName)
            )

            // Cache the loaded Typeface
            sTypefaceCache.put(typefaceName, mTypeface)
        }
    }
}
