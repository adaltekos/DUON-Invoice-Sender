package com.example.duoninvoicesender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val picId = 123
    private val requestCamera: Int = 2
    private val requestWrite: Int = 2
    var mCurrentPhotoPath: String? = null
    var photoFile: File? = null
    var photoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),requestCamera)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),requestWrite)
        }

        val buttonShot: Button = findViewById<Button>(R.id.buttonShot)
        buttonShot?.setOnClickListener(){
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = createImageFile()
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.duoninvoicesender.fileprovider", photoFile!!)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(cameraIntent, picId)
            }
        }

        val buttonSend: Button = findViewById<Button>(R.id.buttonSend)
        buttonSend?.setOnClickListener(){
            if (photoURI != null) {
                sendEmail("adaltekos@gmail.com" as String, "Faktura" as String, photoURI as Uri)
            }
            else{
                val toast = Toast.makeText(applicationContext, "Take photo first", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM,0,200)
                toast.show()
            }
        }
    }

    private fun sendEmail(mail: String, title: String, image: Uri) {
        val mailIntent = Intent(Intent.ACTION_SEND)
        mailIntent.data = Uri.parse("mailto:")
        mailIntent.type = "message/rfc822"
        val addressees = arrayOf(mail)
        mailIntent.putExtra(Intent.EXTRA_EMAIL, addressees)
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        mailIntent.putExtra(Intent.EXTRA_STREAM, image)
        startActivity(Intent.createChooser(mailIntent, "Wybierz klienta poczty..."))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dane: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, dane)
        if (requestCode == picId && resultCode == RESULT_OK && dane!= null) {
            val file = File(mCurrentPhotoPath)
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
            val imageViewCamera: ImageView = findViewById<ImageView>(R.id.imageViewCamera)
            imageViewCamera.setImageBitmap(bitmap)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        mCurrentPhotoPath = image.absolutePath
        return image
    }
}