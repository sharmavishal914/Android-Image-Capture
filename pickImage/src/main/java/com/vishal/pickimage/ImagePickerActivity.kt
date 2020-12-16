package com.vishal.pickimage

import android.Manifest
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.yalantis.ucrop.UCrop
import java.io.File

class ImagePickerActivity : AppCompatActivity() {

    companion object {
        const val INTENT_IMAGE_PICKER_OPTION = "image_picker_option"
        const val INTENT_ASPECT_RATIO_X = "aspect_ratio_x"
        const val INTENT_ASPECT_RATIO_Y = "aspect_ratio_Y"
        const val INTENT_LOCK_ASPECT_RATIO = "lock_aspect_ratio"
        const val INTENT_IMAGE_COMPRESSION_QUALITY = "compression_quality"
        const val INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT = "set_bitmap_max_width_height"
        const val INTENT_BITMAP_MAX_WIDTH = "max_width"
        const val INTENT_BITMAP_MAX_HEIGHT = "max_height"
        const val INTENT_SELECT_GALLERY = "select_gallery"
        const val INTENT_SELECT_CAMERA = "select_camera"
        const val REQUEST_IMAGE_CAPTURE = 0
        const val REQUEST_GALLERY_IMAGE = 1
    }

    private var setBitmapMaxWidthHeight = true
    private var lockAspectRatio = true
    private var aspectRatioX = 1
    private var aspectRatioY = 1
    private var bitmapMaxWidth = 1000
    private var bitmapMaxHeight = 1000
    private var imageCompression = 50
    private var fileName = ""
    private var title = "Pick Photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)
        supportActionBar?.title = title
        clearCache()

        aspectRatioX = intent.getIntExtra(INTENT_ASPECT_RATIO_X, aspectRatioX)
        aspectRatioY = intent.getIntExtra(INTENT_ASPECT_RATIO_Y, aspectRatioY)
        imageCompression = intent.getIntExtra(INTENT_IMAGE_COMPRESSION_QUALITY, imageCompression)
        lockAspectRatio = intent.getBooleanExtra(INTENT_LOCK_ASPECT_RATIO, lockAspectRatio)
        setBitmapMaxWidthHeight =
            intent.getBooleanExtra(INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, setBitmapMaxWidthHeight)
        bitmapMaxWidth = intent.getIntExtra(INTENT_BITMAP_MAX_WIDTH, bitmapMaxWidth)
        bitmapMaxHeight = intent.getIntExtra(INTENT_BITMAP_MAX_HEIGHT, bitmapMaxHeight)

        when {
            intent.getBooleanExtra(INTENT_SELECT_CAMERA, false) -> {
                takeCameraImage()
            }
            intent.getBooleanExtra(INTENT_SELECT_GALLERY, false) -> {
                chooseImageFromGallery()
            }
            else -> {
                showImagePickerOptions()
            }
        }
    }


    private fun showImagePickerOptions() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val options = arrayOf("Camera", "Gallery")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takeCameraImage()
                1 -> chooseImageFromGallery()
            }
        }
        val dialog = builder.create()
        dialog.show()
        dialog.setOnCancelListener {
            setResultCancelled()
        }
    }


    private fun takeCameraImage() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        fileName = System.currentTimeMillis().toString() + ".jpg"
                        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            getCacheImagePath(fileName)
                        )
                        if (takePictureIntent.resolveActivity(packageManager) != null) {
                            startActivityForResult(
                                takePictureIntent,
                                REQUEST_IMAGE_CAPTURE
                            )
                        }
                    } else if (report.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    } else if (report.deniedPermissionResponses.isNotEmpty()) {
                        takeCameraImage()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun chooseImageFromGallery() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    when {
                        report.areAllPermissionsGranted() -> {
                            val pickPhoto = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            startActivityForResult(
                                pickPhoto,
                                REQUEST_GALLERY_IMAGE
                            )
                        }
                        report.isAnyPermissionPermanentlyDenied -> {
                            showSettingsDialog()
                        }
                        report.deniedPermissionResponses.isNotEmpty() -> {
                            chooseImageFromGallery()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> if (resultCode == RESULT_OK) {
                getCacheImagePath(fileName)?.let { cropImage(it) }
            } else {
                setResultCancelled()
            }
            REQUEST_GALLERY_IMAGE -> if (resultCode == RESULT_OK) {
                val imageUri = data?.data
                if (imageUri != null) {
                    cropImage(imageUri)
                } else {
                    setResultCancelled()
                }
            } else {
                setResultCancelled()
            }
            UCrop.REQUEST_CROP -> if (resultCode == RESULT_OK) {
                handleUCropResult(data)
            } else {
                setResultCancelled()
            }
            UCrop.RESULT_ERROR -> {
                setResultCancelled()
            }
            else -> setResultCancelled()
        }
    }

    private fun cropImage(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, queryName(contentResolver, sourceUri)))
        val options = UCrop.Options()
        options.setCompressionQuality(imageCompression)

        // applying UI theme
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.colorPrimary))
        options.setToolbarWidgetColor(Color.WHITE)

        if (lockAspectRatio) options.withAspectRatio(
            aspectRatioX.toFloat(),
            aspectRatioY.toFloat()
        )

        options.setHideBottomControls(true)

        if (setBitmapMaxWidthHeight) options.withMaxResultSize(bitmapMaxWidth, bitmapMaxHeight)
        UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .start(this)
    }

    private fun handleUCropResult(data: Intent?) {
        if (data == null) {
            setResultCancelled()
            return
        }
        val resultUri = UCrop.getOutput(data)
        setResultOk(resultUri)
    }

    private fun setResultOk(imagePath: Uri?) {
        val intent = Intent()
        intent.putExtra("path", imagePath)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setResultCancelled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }


    private fun getCacheImagePath(fileName: String): Uri? {
        val path = File(externalCacheDir, "camera")
        if (!path.exists()) path.mkdirs()
        val image = File(path, fileName)
        return FileProvider.getUriForFile(this, "$packageName.provider", image)
    }


    private fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor = resolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val name = returnCursor?.getString(nameIndex ?: 0)
        returnCursor?.close()
        return name ?: ""
    }

    /**
     * Calling this will delete the images from cache directory
     * useful to clear some memory
     */
    private fun clearCache() {
        val path = File(externalCacheDir, "camera")
        if (path.exists() && path.isDirectory) {
            for (child in path.listFiles() ?: arrayOf()) {
                child.delete()
            }
        }
    }

    private fun showSettingsDialog() {
        val builder: android.app.AlertDialog.Builder =
            android.app.AlertDialog.Builder(this)
        builder.setTitle("Grant Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GOTO SETTINGS") { dialog, _ ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.cancel()
            this@ImagePickerActivity.finish()
        }
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

}