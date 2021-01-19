package com.knowtech.scanassist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.knowtech.scanassist.databinding.ActivityMainBinding
import com.knowtech.scanassist.utils.CURRENT_IMAGE_TYPE
import com.knowtech.scanassist.utils.FILE_PROVIDER_AUTHORITY
import com.knowtech.scanassist.utils.REQUEST_DOCUMENT_IMAGE_CAPTURE
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding
    private lateinit var mTempImagePath: String
    private lateinit var raw_bitmap: Bitmap
    private lateinit var mat : Mat
    private val baseLoaderCallback by lazy {

        object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS ->  mat = Mat()
                    else -> super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraBtn.setOnClickListener{ openCamera()  }
        binding.processBtn.setOnClickListener { processImage(raw_bitmap) }

    }
    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
            Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show()
            OpenCVLoader.initAsync(
                    OpenCVLoader.OPENCV_VERSION,
                    this,
                    baseLoaderCallback
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==REQUEST_DOCUMENT_IMAGE_CAPTURE && resultCode== RESULT_OK){
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            raw_bitmap= BitmapFactory.decodeFile(mTempImagePath, options)
            binding.documentImg.setImageBitmap(raw_bitmap)
        }
    }

    private fun openCamera(){
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(pictureIntent.resolveActivity(packageManager)!= null){
            var photoFile : File? = null
            try{
                photoFile = createTempImageFile()
            }catch (ex: IOException){
                ex.printStackTrace()
            }
            if(photoFile!=null){
                mTempImagePath =photoFile.absolutePath
                val pictureUri : Uri = FileProvider.getUriForFile(
                    this,
                    FILE_PROVIDER_AUTHORITY,
                    photoFile
                )
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)
                startActivityForResult(pictureIntent, REQUEST_DOCUMENT_IMAGE_CAPTURE)
            }

        }

    }

    private fun createTempImageFile() : File {
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
            Date()
        )
        val imageFileName = "JPEG_$timeStamp"
        val storageDir : File? =externalCacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }


    private fun processImage (bitmap: Bitmap){
        val mat = Mat()
        var processed_bitmap: Bitmap? = raw_bitmap
        Utils.bitmapToMat(bitmap,mat)
        Imgproc.cvtColor(mat,mat, CURRENT_IMAGE_TYPE)
        Utils.matToBitmap(mat,processed_bitmap)
        binding.documentImg.setImageBitmap(processed_bitmap)

    }
}