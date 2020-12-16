# Android Image Capture

This code a sample code to capture cropped image from camera & gallery. Check MainActivity.kt file for usage.

### 3rd Party Library

- **Ucrop** : is used to crop image.

## Getting Started

* Add pick image library folder as module in your project

* Add ImagePickerActivity in your manifest file.

        <activity android:name="com.vishal.pickimage.ImagePickerActivity" />

* Start activity to capture image

            val intent = Intent(this, ImagePickerActivity::class.java)
            startActivityForResult(intent, 100)
            overridePendingTransition(0, 0)




