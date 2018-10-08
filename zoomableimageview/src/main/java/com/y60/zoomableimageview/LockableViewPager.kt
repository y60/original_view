package com.y60.zoomableimageview

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class LockableViewPager : ViewPager,ZoomListener {

    private var locked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onZoomStateChanged(zooming:Boolean) {
        this.locked = zooming
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (locked)
            false
        else
            super.onInterceptTouchEvent(ev)
    }
}
