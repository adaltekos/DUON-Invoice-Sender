package com.example.duoninvoicesender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private val picId = 123
    private lateinit var photo: Bitmap
    private val requestCamera: Int = 2
    private val requestWrite: Int = 2
    var photoTaken = false

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
            startActivityForResult(cameraIntent, picId)
        }

        val buttonSend: Button = findViewById<Button>(R.id.buttonSend)
        buttonSend?.setOnClickListener(){
            if (photoTaken) {
                sendEmail("faktury@duon.pl" as String, "Faktura" as String, photo as Bitmap)
            }
            else{
                val toast = Toast.makeText(applicationContext, "Take photo first", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM,0,200)
                toast.show()
            }
        }
    }

    private fun sendEmail(mail: String, title: String, photo: Bitmap) {
        val mailIntent = Intent(Intent.ACTION_SEND)
        mailIntent.data = Uri.parse("mailto:")
        mailIntent.type = "message/rfc822"
        val addressees = arrayOf(mail)
        val photoUri = getImageUriFromBitmap(applicationContext, photo) as Uri
        mailIntent.putExtra(Intent.EXTRA_EMAIL, addressees)
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        mailIntent.putExtra(Intent.EXTRA_STREAM, photoUri)
        startActivity(Intent.createChooser(mailIntent, "Wybierz klienta poczty..."))
    }

    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri{
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)!! as String
        return Uri.parse(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == picId && resultCode == RESULT_OK && data!= null) {
            photo = data.extras?.get("data") as Bitmap
            photoTaken = true
            val imageViewCamera: ImageView = findViewById<ImageView>(R.id.imageViewCamera)
            imageViewCamera.setImageBitmap(photo)
        }
    }
}