package com.y60.originalviews

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.y60.zoomableimageview.ZoomableImageView
import kotlinx.android.synthetic.main.activity_zoomable_image.*

class ZoomableImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zoomable_image)

        pager.offscreenPageLimit = 3

        val res = IntArray(3)
        res[0]=R.drawable.louvre
        res[1]=R.drawable.eiffel
        res[2]=R.drawable.champs

        val pagerAdapter = object : PagerAdapter() {

            override fun getCount(): Int {
                return res.size
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val view = layoutInflater.inflate(R.layout.image, container,false) as ZoomableImageView
                container.addView(view)
                view.setImageResource(res[position])
                view.setZoomListener(pager)
                return view
            }

            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
                container.removeView(obj as View)
            }

            override fun isViewFromObject(view: View, obj: Any): Boolean {
                return view == obj
            }
        }

        pager.adapter = pagerAdapter
    }
}
