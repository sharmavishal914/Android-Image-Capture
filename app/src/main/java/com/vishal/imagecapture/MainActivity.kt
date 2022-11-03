package com.vishal.imagecapture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vishal.pickimage.ImagePickerActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val intent = Intent(this, ImagePickerActivity::class.java)
//        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_COMPRESSION_QUALITY, 30)
//        resultLauncher.launch(intent)
//        overridePendingTransition(0, 0)

        button.setOnClickListener {
            val intent = Intent(this, ImagePickerActivity::class.java)
            intent.putExtra(ImagePickerActivity.INTENT_IMAGE_COMPRESSION_QUALITY, 30)
            resultLauncher.launch(intent)
            overridePendingTransition(0, 0)
        }
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val uri: Uri? = result.data?.getParcelableExtra("path")
                    val bitmap = getBitmap(this, uri)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    private fun getBitmap(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) {
            return null
        }
        var bitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= 29) {
            val source: ImageDecoder.Source =
                ImageDecoder.createSource(context.contentResolver, uri)
            try {
                bitmap = ImageDecoder.decodeBitmap(source)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    uri
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

}   