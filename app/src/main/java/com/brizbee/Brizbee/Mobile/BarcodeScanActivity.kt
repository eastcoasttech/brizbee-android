//
//  BarcodeScanActivity.kt
//  BRIZBEE Mobile for Android
//
//  Copyright © 2021 East Coast Technology Services, LLC
//
//  This file is part of BRIZBEE Mobile for Android.
//
//  BRIZBEE Mobile for Android is free software: you can redistribute
//  it and/or modify it under the terms of the GNU General Public
//  License as published by the Free Software Foundation, either
//  version 3 of the License, or (at your option) any later version.
//
//  BRIZBEE Mobile for Android is distributed in the hope that it will
//  be useful, but WITHOUT ANY WARRANTY; without even the implied
//  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with BRIZBEE Mobile for Android.
//  If not, see <https://www.gnu.org/licenses/>.
//

package com.brizbee.Brizbee.Mobile

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.brizbee.Brizbee.Mobile.databinding.ActivityBarcodeScanBinding
import java.util.concurrent.Executors
import android.content.Intent

private const val CAMERA_PERMISSION_REQUEST_CODE = 1

@ExperimentalGetImage
class BarcodeScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (hasCameraPermission()) bindCameraUseCases()
        else requestPermission()
    }

    // checking to see whether user has already granted permission
    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){
        // opening up dialog to ask for camera permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // user granted permissions - we can set up our scanner
            bindCameraUseCases()
        } else {
            // user did not grant permissions - we can't use the camera
            Toast.makeText(this,
                "Camera permission required",
                Toast.LENGTH_LONG
            ).show()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            // configure our MLKit BarcodeScanning client

            /* passing in our desired barcode formats - MLKit supports additional formats outside of the
            ones listed here, and you may not need to offer support for all of these. You should only
            specify the ones you need */
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_PDF417
            ).build()

            // getClient() creates a new instance of the MLKit barcode scanner with the specified options
            val scanner = BarcodeScanning.getClient(options)

            // setting up the analysis use case
            val analysisUseCase = ImageAnalysis.Builder()
                .build()

            // define the actual functionality of our analysis use case
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor()
            ) { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Log.e(TAG, illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Log.e(TAG, illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    val barcode = barcodeList.getOrNull(0)

                    // `rawValue` is the decoded value of the barcode
                    barcode?.rawValue?.let { value ->
                        val data = Intent()
                        data.putExtra("BarCodeValue", value)
                        setResult(RESULT_OK, data)
                        finish()
                    }
                }
                .addOnFailureListener {
                    // This failure will happen if the barcode scanning model
                    // fails to download from Google Play Services

                    Log.e(TAG, it.message.orEmpty())
                }.addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must
                    // call image.close() on received images when finished
                    // using them. Otherwise, new images may not be received
                    // or the camera may stall.

                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    companion object {
        val TAG: String = BarcodeScanActivity::class.java.simpleName
    }
}