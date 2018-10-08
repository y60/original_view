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

        pager.offscreenPageLimit = 4

        val res = IntArray(4)
        res[0]=R.drawable.ic_launcher_background
        res[1]=R.drawable.ic_launcher_foreground
        res[2]=R.drawable.ic_launcher_background
        res[3]=R.drawable.ic_launcher_background

        val pagerAdapter = object : PagerAdapter() {

            override fun getCount(): Int {
                return res.size
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val view = layoutInflater.inflate(R.layout.image, container,false) as ZoomableImageView
                container.addView(view)
                view.setImageResource(res[position])
                view.setPager(pager)
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
