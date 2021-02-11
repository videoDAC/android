package com.videodac.publisher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View


class AspectTextureView : TextureView {
    private var targetAspect = -1.0
    private var aspectMode = MODE_OUTSIDE

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    /**
     * @param mode        [.MODE_FITXY],[.MODE_INSIDE],[.MODE_OUTSIDE]
     * @param aspectRatio width/height
     */
    fun setAspectRatio(mode: Int, aspectRatio: Double) {
        require(!(mode != MODE_INSIDE && mode != MODE_OUTSIDE && mode != MODE_FITXY)) { "illegal mode" }
        require(aspectRatio >= 0) { "illegal aspect ratio" }
        if (targetAspect != aspectRatio || aspectMode != mode) {
            targetAspect = aspectRatio
            aspectMode = mode
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (targetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
            var initialHeight = MeasureSpec.getSize(heightMeasureSpec)
            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = targetAspect / viewAspectRatio - 1
            if (Math.abs(aspectDiff) > 0.01 && aspectMode != MODE_FITXY) {
                if (aspectMode == MODE_INSIDE) {
                    if (aspectDiff > 0) {
                        initialHeight = (initialWidth / targetAspect).toInt()
                    } else {
                        initialWidth = (initialHeight * targetAspect).toInt()
                    }
                } else if (aspectMode == MODE_OUTSIDE) {
                    if (aspectDiff > 0) {
                        initialWidth = (initialHeight * targetAspect).toInt()
                    } else {
                        initialHeight = (initialWidth / targetAspect).toInt()
                    }
                }
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        var l = l
        var t = t
        var r = r
        var b = b
        val p = parent as View
        if (p != null) {
            val pw = p.measuredWidth
            val ph = p.measuredHeight
            val w = measuredWidth
            val h = measuredHeight
            t = (ph - h) / 2
            l = (pw - w) / 2
            r += l
            b += t
        }
        super.layout(l, t, r, b)
    }

    companion object {
        const val MODE_FITXY = 0
        const val MODE_INSIDE = 1
        const val MODE_OUTSIDE = 2
    }
}