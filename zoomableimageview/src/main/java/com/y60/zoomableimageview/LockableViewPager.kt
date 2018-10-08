package com.y60.zoomableimageview

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class LockableViewPager : ViewPager {
    private var locked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setLocked(locked: Boolean) {
        this.locked = locked
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (locked)
            false
        else
            super.onInterceptTouchEvent(ev)
    }
}
