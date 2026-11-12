package com.maruf.flashlightapp

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.maruf.flashlightapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var flashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraManager = getSystemService(CameraManager::class.java)

        binding.flashToggleButton.setOnClickListener {
            handleFlashToggle()
        }

        updateStatusText()
    }

    private fun handleFlashToggle() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, getString(R.string.android_requirement), Toast.LENGTH_LONG).show()
            return
        }

        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        if (!flashExists()) {
            Toast.makeText(this, getString(R.string.no_flash), Toast.LENGTH_LONG).show()
            return
        }

        if (flashOn) {
            turnOffFlash()
        } else {
            turnOnFlash()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun flashExists(): Boolean {
        val ids = cameraManager.cameraIdList
        for (id in ids) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            if (flashAvailable == true) {
                cameraId = id
                return true
            }
        }
        cameraId = null
        return false
    }

    private fun turnOnFlash() {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, true)
                flashOn = true
                binding.flashToggleButton.text = getString(R.string.flash_off)
                updateStatusText()
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to turn on flash: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun turnOffFlash() {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, false)
                flashOn = false
                binding.flashToggleButton.text = getString(R.string.flash_on)
                updateStatusText()
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to turn off flash: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateStatusText() {
        binding.statusTextView.text = if (flashOn) {
            getString(R.string.flash_status_on)
        } else {
            getString(R.string.flash_status_off)
        }
        binding.statusTextView.setTextColor(
            if (flashOn) getColor(R.color.status_on) else getColor(R.color.status_off)
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (flashExists()) {
                    turnOnFlash()
                } else {
                    Toast.makeText(this, getString(R.string.no_flash), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (flashOn) {
            turnOffFlash()
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }
}
