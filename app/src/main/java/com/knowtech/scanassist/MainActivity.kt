package com.knowtech.scanassist

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.util.Log
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.knowtech.scanassist.databinding.ActivityMainBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var binding: ActivityMainBinding
    private lateinit var spinner : Spinner
    private val baseLoaderCallback by lazy {
        object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> binding.cameraView.enableView()
                    else -> super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraView.setCameraPermissionGranted()
        binding.cameraView.setCvCameraViewListener(this)



    }

    private var mRgba: Mat? = null
    private var mRgbaT: Mat? = null
    private var dst: Mat? = null
    private var tempMat: Mat? = null
    private var rotatedMat: Mat? = null
    val list = mutableListOf<MatOfPoint>()

    val newMat by lazy { MatOfPoint2f() }
    val approx by lazy { MatOfPoint2f() }
    var area = 0.0
    var maxArea = 0.0
    var peri = 0.0
    var scalar = Scalar(0.0, 255.0, 0.0)
    var kSize = Size(5.0, 5.0)
    var rect: Rect? = null

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mRgbaT = Mat()
        dst = Mat()
    }

    override fun onCameraViewStopped() {
        mRgba?.release()
        mRgbaT?.release()
        dst?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        inputFrame?.let {
            mRgba = inputFrame.rgba()
            tempMat = mRgba?.t()
            Core.flip(tempMat, mRgbaT, 1)
            Imgproc.resize(mRgbaT, dst, mRgba?.size())
            rotatedMat = dst?.clone()
            Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2GRAY)
            Imgproc.GaussianBlur(dst, dst, kSize, 0.0)
            Imgproc.Canny(dst, dst, 75.0, 200.0)
            list.clear()
            Imgproc.findContours(dst, list, dst, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            findBiggestContour()?.let {
                Imgproc.drawContours(rotatedMat, listOf(it), -1, scalar, 2)
                it.release()
                maxArea = 0.0
            }
            tempMat?.release()
            dst?.release()
            return rotatedMat!!
        }
        return Mat()
    }

    private fun findBiggestContour(): MatOfPoint? {
        list.forEach { mat ->
            mat.convertTo(newMat, CvType.CV_32F)
            area = Imgproc.contourArea(mat)
            if (area > 200) {
                peri = Imgproc.arcLength(newMat, true)
                Imgproc.approxPolyDP(newMat, approx, 0.02 * peri, true)
                Log.d("bytescanValues", approx.toArray().toString())
                if (area > maxArea && approx.toList().size == 4) {
                    rect = Imgproc.boundingRect(approx)
                    rect?.let {
                        maxArea = area
                        newMat.release()
                        approx.convertTo(mat, CvType.CV_32S)
                        approx.release()
                        return mat
                    }
                }
            }
            newMat.release()
            approx.release()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cameraView?.disableView()
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

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND, ComponentCallbacks2.TRIM_MEMORY_MODERATE, ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
            }

            else -> {
            }
        }
    }
}