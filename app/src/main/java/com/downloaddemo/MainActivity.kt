package com.downloaddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.downloaddemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.singleDownload.setOnClickListener {
            startActivity(Intent(applicationContext, SingleDownloadActivity::class.java))
        }

        binding.multipleDownload.setOnClickListener {
            startActivity(Intent(applicationContext, MultipleDownloadActivity::class.java))
        }
    }
}