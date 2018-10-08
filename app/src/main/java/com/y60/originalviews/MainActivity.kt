package com.y60.originalviews

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchZoomableImage(view: View){
        val intent=Intent(this, ZoomableImageActivity::class.java)
        startActivity(intent)
    }

}
