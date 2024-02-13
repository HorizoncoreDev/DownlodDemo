package com.downloaddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.downloaddemo.databinding.ActivityMainBinding

/**
* This class has 2 buttons where you can get demo for 2 methods
* 1. Single File Download
* 2. Multiple Download
* */
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**
         * Single File Download
         * */
        binding.singleDownload.setOnClickListener {
            startActivity(Intent(applicationContext, SingleDownloadActivity::class.java))
        }

        /**
         * Multiple File Download
         * */
        binding.multipleDownload.setOnClickListener {
            startActivity(Intent(applicationContext, MultipleDownloadActivity::class.java))
        }
    }
}