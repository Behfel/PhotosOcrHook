package com.example.photoshook

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.oneplus.gallery" && 
            lpparam.packageName != "com.coloros.gallery3d") {
            return
        }

        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onPostCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    injectOcrButton(activity)
                }
            }
        )
    }

    private fun injectOcrButton(activity: Activity) {
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        val tag = "injected_ocr_pill"

        if (decorView.findViewWithTag<View>(tag) != null) return

        val button = Button(activity).apply {
            this.tag = tag
            text = "Copy Text"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#99000000"))

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 140
                marginEnd = 40
            }
            layoutParams = params

            setOnClickListener {
                val bitmap = captureBitmap(activity)
                if (bitmap != null) {
                    OcrManager.processBitmap(activity, bitmap)
                }
            }
        }

        decorView.post { decorView.addView(button) }
    }

    private fun captureBitmap(activity: Activity): Bitmap? {
        val decorView = activity.window?.decorView ?: return null
        val targetImageView = findTargetImageView(decorView)
        val drawable = targetImageView?.drawable

        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        return try {
            val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            decorView.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    private fun findTargetImageView(view: View): ImageView? {
        if (view is ImageView && view.isShown && view.drawable != null) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = findTargetImageView(view.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }
}