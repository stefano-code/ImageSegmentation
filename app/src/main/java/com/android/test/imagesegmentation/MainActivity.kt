package com.android.test.imagesegmentation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.view.View

import androidx.appcompat.app.AppCompatActivity

import com.android.test.imagesegmentation.ml.ImageSegmentationModelExecutor
import com.android.test.imagesegmentation.ml.Recognition
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object{
        val TAG = "MainActivity"
    }
    private val RESULT_LOAD_IMAGE = 101
    val IMAGE_CAPTURE_CODE = 102
    private val PERMISSION_CODE = 103

    var maskImg: ImageView? = null;
    var orignalImg: ImageView? = null;
    var transparentImage: ImageView? = null;
    var resultImg:ImageView? = null
    private var image_uri: Uri? = null
    var resultTv: TextView?=null;
    private var imageSegmentationModel: ImageSegmentationModelExecutor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permission =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permission, PERMISSION_CODE)


        try {
            imageSegmentationModel = ImageSegmentationModelExecutor(this)
        } catch (e: Exception) {
            Log.d(TAG, "Fail to create ImageSegmentationModelExecutor: ${e.message}")
        }

        resultTv = findViewById(R.id.textView)
        maskImg = findViewById(R.id.imageView2)
        resultImg = findViewById(R.id.imageView3)
        orignalImg = findViewById(R.id.imageView4)
        transparentImage= findViewById(R.id.imageView)
        maskImg?.setOnClickListener(View.OnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE)
        })

        maskImg?.setOnLongClickListener(View.OnLongClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    == PackageManager.PERMISSION_DENIED
                ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera()
                }
            } else {
                openCamera()
            }
            true
        })
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            image_uri = data.data
            maskImg!!.setImageURI(image_uri)
            doInference()
        }
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            maskImg!!.setImageURI(image_uri)
            doInference()
        }
    }

    fun doInference() {
        val bitmap = uriToBitmap(image_uri!!)
        if (bitmap != null) {
            var modelExecutionResult: Recognition = imageSegmentationModel?.segmentImage(bitmap)!!
            resultImg?.setImageBitmap(modelExecutionResult.bitmapResult)
            maskImg?.setImageBitmap(modelExecutionResult.bitmapMaskOnly)
            orignalImg?.setImageBitmap(modelExecutionResult.bitmapOriginal)
            transparentImage?.setImageBitmap(modelExecutionResult.transparentBitmap)
            val hashMap: Map<String, Int> = modelExecutionResult.itemsFound
            val it: Iterator<Map.Entry<String, Int>> = hashMap.entries.iterator()
            resultTv?.setText("")
            while (it.hasNext()) {
                val pair = it.next() as Map.Entry<*, *>
                resultTv?.append("${pair.key.toString()}\n");
            }
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor =
                contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        imageSegmentationModel?.close();
    }
}