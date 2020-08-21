package com.example.duoninvoicesender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val picId = 123
    private lateinit var photo: Bitmap
    private val requestSendSms: Int = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),requestSendSms)
        }

        val buttonShot: Button = findViewById<Button>(R.id.buttonShot)
        buttonShot?.setOnClickListener(){
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, picId)
        }

        val buttonSend: Button = findViewById<Button>(R.id.buttonSend)
        buttonSend?.setOnClickListener(){
            sendEmail("faktury@duon.pl", "Faktura", photo)
        }

    }

    private fun sendEmail(mail: String, title: String, photo: Bitmap) {
        val mailIntent = Intent(Intent.ACTION_SEND)
        mailIntent.data = Uri.parse("mailto:")
        mailIntent.type = "text/plain"
        mailIntent.putExtra(Intent.EXTRA_EMAIL, mail)
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        mailIntent.putExtra(Intent.EXTRA_TEXT, photo)
        startActivity(Intent.createChooser(mailIntent, "Wybierz klienta poczty..."))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == picId && resultCode == RESULT_OK && data!= null) {
            photo = data.extras?.get("data") as Bitmap
            val imageViewCamera: ImageView = findViewById<ImageView>(R.id.imageViewCamera)
            imageViewCamera.setImageBitmap(photo)
        }
    }
}