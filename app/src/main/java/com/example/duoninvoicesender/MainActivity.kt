package com.example.duoninvoicesender

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import org.w3c.dom.Text
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


class MainActivity : AppCompatActivity() {

    private val picId = 123
    var photoFile: File? = null
    var photoURI: Uri? = null
    lateinit var mCurrentPhotoPath : String
    lateinit var mCurrentPhotoName : String
    private val SHARED_PREFS = "sharedPrefs"
    private val TEXTMailTo = "MailTo"
    private val TEXTMailFrom = "MailFrom"
    private val TEXTMailPass = "MailFromPass"
    var mailTo: String = ""
    var mailFromMailString : String = ""
    var mailPassString : String = ""
    var firstLetterOfName: Char? = null
    var firstLetterOfSurname: Char? = null
    private val PERMISSION_ALL = 1
    lateinit var log: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }

        loadData()
        log =findViewById(R.id.progressBar)
        if(mailFromMailString != "")firstLetterOfName = mailFromMailString.get(0).toUpperCase()
        if(mailFromMailString != "")firstLetterOfSurname = mailFromMailString.get(mailFromMailString.indexOf(".")+1).toUpperCase()

        val mailFromMailEditText  = findViewById<EditText>(R.id.mailFromEditTextEmailAddress)
        mailFromMailEditText.setText(mailFromMailString)
        val mailFromPassEditText  = findViewById<EditText>(R.id.mailFromEditTextPassword)
        mailFromPassEditText.setText(mailPassString)
        val testMailButton = findViewById<Button>(R.id.testMailButton)
        testMailButton.setOnClickListener {
            mailFromMailString = mailFromMailEditText.text.toString()
            saveData(TEXTMailFrom, mailFromMailString)
            mailPassString = mailFromPassEditText.text.toString()
            saveData(TEXTMailPass, mailPassString)
            if(mailFromMailString != "")firstLetterOfName = mailFromMailString.get(0).toUpperCase()
            if(mailFromMailString != "")firstLetterOfSurname = mailFromMailString.get(mailFromMailString.indexOf(".")+1).toUpperCase()
            Thread({testMail()}).start()
        }

        val takePhotoButton = findViewById<Button>(R.id.takePhotoButton)
        takePhotoButton.setOnClickListener {
            startCameraIntent()
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraIntent()
        }

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mailToMenuButton -> {
                showDialogMailTo()
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun showDialogMailTo() {
        val dialogMailTo = Dialog(this)
        dialogMailTo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogMailTo.setCancelable(false)
        dialogMailTo.setContentView(R.layout.mail_to)
        val phoneField  = dialogMailTo.findViewById(R.id.mailToEditTextEmailAddress) as EditText
        phoneField.setText(mailTo)
        val okBtn = dialogMailTo.findViewById(R.id.okBtn) as Button
        okBtn.setOnClickListener {
            mailTo = phoneField.text.toString()
            saveData(TEXTMailTo, mailTo)
            dialogMailTo.dismiss()
            val toast = Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.BOTTOM,0,200)
            toast.show()
        }
        dialogMailTo.show()
    }

    fun saveData(value : String, string: String) {
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(value, string)
        editor.apply()
    }

    fun loadData() {
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        mailTo = sharedPreferences.getString(TEXTMailTo, "")!!
        mailFromMailString = sharedPreferences.getString(TEXTMailFrom, "")!!
        mailPassString = sharedPreferences.getString(TEXTMailPass, "")!!
    }

    private fun startCameraIntent(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = createImageFile()
        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(this, "com.example.duoninvoicesender.fileprovider", photoFile!!)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, picId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dane: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, dane)
        if (requestCode == picId && resultCode == RESULT_OK) {
            log.visibility = View.VISIBLE
            val sendThread = Thread {
                try {
                    Transport.send(plainMail())
                    this@MainActivity.runOnUiThread {
                        log.visibility = View.INVISIBLE
                        val toast = Toast.makeText(this@MainActivity, "Mail sent", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.BOTTOM, 0, 200)
                        toast.show()
                    }
                }
                catch(e: AuthenticationFailedException) {
                    e.printStackTrace()
                    this@MainActivity.runOnUiThread {
                        val toast = Toast.makeText(this@MainActivity, "Something went wrong, check your account info", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.BOTTOM,0,200)
                        toast.show()
                    }
                }
                catch(e: SendFailedException) {
                    e.printStackTrace()
                    this@MainActivity.runOnUiThread {
                        val toast = Toast.makeText(this@MainActivity, "Something went wrong, check your account info", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.BOTTOM, 0, 200)
                        toast.show()
                    }
                }
                catch(e: MessagingException) {
                    e.printStackTrace()
                    this@MainActivity.runOnUiThread {
                        val toast = Toast.makeText(this@MainActivity, "Something went wrong, check your account info", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.BOTTOM,0,200)
                        toast.show()
                    }
                }
            }
            sendThread.priority = 10
            sendThread.start()
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = firstLetterOfName.toString() + firstLetterOfSurname.toString() + "_FV_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        mCurrentPhotoPath = image.absolutePath
        mCurrentPhotoName = image.name
        return image
    }

    private fun plainMail(): MimeMessage {
            val tos = arrayListOf(mailTo) //Multiple recipients
            val from = mailFromMailString //Sender email
            val properties = System.getProperties()
            with(properties) {
                put("mail.smtp.host", "mail.duon.biz") //Configure smtp host
                put("mail.smtp.port", "587") //Configure port
                //put("mail.smtp.starttls.enable", "false") //Enable TLS
                put("mail.smtp.auth", "true") //Enable authentication
            }
            val auth = object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(from, mailPassString) //Credentials of the sender email
            }
            val session = Session.getInstance(properties, auth)
            val message = MimeMessage(session)
            val multipart = MimeMultipart("related")
            val messageBodyPart1 = MimeBodyPart()
            val Text = "W załączeniu skan faktury. - " + firstLetterOfName.toString() + firstLetterOfSurname.toString()
            messageBodyPart1.setContent(Text, "text/html; charset=UTF-8")
            multipart.addBodyPart(messageBodyPart1)
            val messageBodyPart2 = MimeBodyPart()
            val fds = FileDataSource(mCurrentPhotoPath)
            messageBodyPart2.dataHandler = DataHandler(fds)
            messageBodyPart2.fileName = mCurrentPhotoName
            multipart.addBodyPart(messageBodyPart2)
            with(message) {
                setFrom(InternetAddress(from))
                for (to in tos) {
                    addRecipient(Message.RecipientType.TO, InternetAddress(to))
                    subject = "Skan_Faktury" //Email subject
                    setContent(multipart)
                }
            }
        return message
    }

    private fun testMail() {
        try {
            val properties = System.getProperties()
            with(properties) {
                //put("mail.smtp.host", "mail.duon.biz") //Configure smtp host
                //put("mail.smtp.port", "587") //Configure port
                //put("mail.smtp.starttls.enable", "false") //Enable TLS
                put("mail.smtp.auth", "true") //Enable authentication
            }
            val session = Session.getInstance(properties, null)
            val transport = session.getTransport("smtp")
            transport.connect("mail.duon.biz", 587, mailFromMailString, mailPassString)
            transport.close()
            this@MainActivity.runOnUiThread {
                val toast = Toast.makeText(this@MainActivity, "Saved and mail test passed", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM,0,200)
                toast.show()
            }
        }
        catch(e: AuthenticationFailedException) {
            e.printStackTrace()
            this@MainActivity.runOnUiThread {
                val toast = Toast.makeText(this@MainActivity, "Something went wrong, check your account info", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM,0,200)
                toast.show()
            }

        }
        catch(e: MessagingException) {
            e.printStackTrace()
            this@MainActivity.runOnUiThread {
                val toast = Toast.makeText(this@MainActivity, "Something went wrong, check your account info", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.BOTTOM,0,200)
                toast.show()
            }
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

}