package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.base.BaseActivity
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * INHERITANCE — extends [BaseActivity] which provides:
 *   • FirebaseAuth initialisation
 *   • Edge-to-edge + window-insets setup
 *
 * Full-screen camera activity that uses CameraX + ML Kit Barcode Scanning
 * to detect shipping barcodes and QR codes. On successful detection,
 * returns the scanned value via [RESULT_OK] intent extra "barcode_value".
 */
class BarcodeScannerActivity : BaseActivity() {

    companion object {
        private const val TAG = "BarcodeScanner"
        const val EXTRA_BARCODE_VALUE = "barcode_value"
    }

    override fun getLayoutResId(): Int = R.layout.activity_barcode_scanner

    private lateinit var previewView: PreviewView
    private var cameraControl: CameraControl? = null
    private var isFlashOn = false

    /** Flag to prevent duplicate results once a barcode is detected. */
    private var barcodeDetected = false

    /** Single-thread executor for image analysis off the main thread. */
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = findViewById(R.id.previewView)
        val btnClose = findViewById<ImageView>(R.id.btnClose)
        val btnFlash = findViewById<ImageView>(R.id.btnFlash)

        btnClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            cameraControl?.enableTorch(isFlashOn)
        }

        startCamera()
    }

    /**
     * Binds CameraX Preview + ImageAnalysis use cases to the lifecycle.
     * The camera is automatically released when this activity is destroyed.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            // Image analysis use case with ML Kit barcode scanner
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, BarcodeAnalyzer()) }

            // Use back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * CameraX [ImageAnalysis.Analyzer] that passes each frame to ML Kit's
     * barcode scanner. Configured for common shipping barcode formats:
     * Code 128, Code 39, QR Code, EAN-13, and EAN-8.
     */
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {

        private val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8
            )
            .build()

        private val scanner = BarcodeScanning.getClient(scannerOptions)

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            // Skip if we already have a result
            if (barcodeDetected) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && !barcodeDetected) {
                        // Take the first detected barcode
                        val barcodeValue = barcodes[0].rawValue
                        if (!barcodeValue.isNullOrBlank()) {
                            barcodeDetected = true
                            returnResult(barcodeValue)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode analysis failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    /**
     * Sends the scanned barcode value back to the calling activity
     * and finishes this scanner screen.
     */
    private fun returnResult(barcodeValue: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_BARCODE_VALUE, barcodeValue)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }
}
