@file:Suppress("DEPRECATION")

package com.videodac.publisher.helpers

import android.content.Context
import android.hardware.Camera

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.videodac.publisher.helpers.Constants.STREAMING_TAG
import java.io.IOException

/** A basic Camera preview class */
class CameraPreview(
    context: Context,
    private val mCamera: Camera, private var mSupportedPreviewSizes: List<Camera.Size>? = null,
) : SurfaceView(context), SurfaceHolder.Callback {


    private lateinit var mPreviewSize: Camera.Size

    init {
        // supported preview sizes
        mSupportedPreviewSizes = mCamera.parameters.supportedPreviewSizes
    }
    private val mHolder: SurfaceHolder = holder.apply {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        addCallback(this@CameraPreview)
        // deprecated setting, but required on Android versions prior to 3.0
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        mCamera.apply {
            try {
                val params = mCamera.parameters
                params.setPreviewSize(mPreviewSize.width, mPreviewSize.height)
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                parameters = params
                setDisplayOrientation(90)
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(STREAMING_TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height)!!
        }
        val ratio: Float = if (mPreviewSize.height >= mPreviewSize.width) mPreviewSize.height.toFloat() / mPreviewSize.width.toFloat()  else mPreviewSize.width.toFloat()  / mPreviewSize.height.toFloat()

        setMeasuredDimension(width, (width * ratio).toInt())
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = h.toDouble() / w
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        for (size in sizes) {
            val ratio = size.height.toDouble() / size.width
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }
}