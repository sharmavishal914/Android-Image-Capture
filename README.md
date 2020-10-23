# Android Image Capture

This code a sample code to capture cropped image from camera & gallery.

### 3rd Party Library

- **Dexter** : is used to manage the permissions for internal & external storage
- **Ucrop** : is used to crop image.

## Getting Started

Add 3rd party library in app gradle file


        implementation "com.karumi:dexter:6.2.1"
        implementation 'com.github.yalantis:ucrop:2.2.6'


Add file_paths.xml in res/xml folder

    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <external-cache-path
            name="cache"
            path="camera" />
    </paths>

Add permissions in manifest file

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

Add queries for Android 11


        <queries>
            <intent>
                <action android:name="android.media.action.IMAGE_CAPTURE" />
            </intent>
        </queries>

Add activity & file provider in manifest

        <activity android:name=".ImagePickerActivity" />
        <!-- uCrop cropping activity -->
        <activity
            android:hardwareAccelerated="false"
            android:name="com.yalantis.ucrop.UCropActivity"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AppCompat.DayNight.NoActionBar" />

        <!-- cache directory file provider paths -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

Copy ImagePickerActivity & activity_image_picker to your source code & start capturing image by following code

        val intent = Intent(this, ImagePickerActivity::class.java)
        startActivityForResult(intent, 100)


Image URI will be received in activity result


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 100) {
                if (resultCode == RESULT_OK) {
                    val uri: Uri? = data?.getParcelableExtra<Uri>("path")
                    try {
                        // You can update this bitmap to your server
                        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        imageView.setImageBitmap(bitmap)
                        // loading profile image from local cache
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

Add progaurd rules

    -dontwarn com.yalantis.ucrop**
    -keep class com.yalantis.ucrop** { *; }
    -keep interface com.yalantis.ucrop** { *; }


