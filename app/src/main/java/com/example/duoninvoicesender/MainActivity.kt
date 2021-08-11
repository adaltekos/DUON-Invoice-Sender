package com.example.duoninvoicesender

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.payment_method.*
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
import android.app.NotificationManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts


@ExperimentalStdlibApi
class MainActivity : AppCompatActivity() {

    private var photoFile: File? = null
    private var photoURI: Uri? = null
    private lateinit var mCurrentPhotoPath : String
    private var mailTo: String = ""
    private var mailFrom : String = ""
    var mailPass : String = ""
    private var firstName: String? = null
    private var lastName: String? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var radioButton: RadioButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setPermission()
        loadData()
        setNameAndSurname()
        createNotificationChannel()

        val mailFromMailEditText  = findViewById<EditText>(R.id.mailFromEditTextEmailAddress)
        mailFromMailEditText.setText(mailFrom)
        val mailFromPassEditText  = findViewById<EditText>(R.id.mailFromEditTextPassword)
        mailFromPassEditText.setText(mailPass)
        val testMailButton = findViewById<Button>(R.id.testMailButton)
        testMailButton.setOnClickListener {
            mailFrom = mailFromMailEditText.text.toString()
            saveData("MailFrom", mailFrom)
            mailPass = mailFromPassEditText.text.toString()
            saveData("MailFromPass", mailPass)
            setNameAndSurname()
            Thread { testMail() }.start()
        }

        val takePhotoButton = findViewById<Button>(R.id.takePhotoButton)
        takePhotoButton.setOnClickListener {
            startCameraIntent()
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraIntent()
        }
    }

    private fun createNotificationChannel() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("CHANNEL_ID", "name", NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Wysyłam maila")
            .setContentText("Nie zamykaj aplikacji")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .setProgress(100,50,true)

        with(NotificationManagerCompat.from(this)) { notify(999, builder.build()) }
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(this).cancel(999)
    }

    private fun showDialogPaymentMethod() {
        val dialogPaymentMethod = Dialog(this)
        dialogPaymentMethod.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogPaymentMethod.setContentView(R.layout.payment_method)
        val imageView = dialogPaymentMethod.findViewById(R.id.imageView) as ImageView
        imageView.setImageURI(photoURI)
        val radioGroup = dialogPaymentMethod.findViewById(R.id.radioGroup) as RadioGroup
        val sendButton = dialogPaymentMethod.findViewById(R.id.button) as Button
        sendButton.setOnClickListener {
            radioButton = dialogPaymentMethod.findViewById(radioGroup.checkedRadioButtonId) as RadioButton
            dialogPaymentMethod.dismiss()
            sendMail()
        }
        dialogPaymentMethod.show()
    }

    private fun setPermission() {
        val permission = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        if (!hasPermissions(this, *permission)) ActivityCompat.requestPermissions(this, permission, 1)
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun setNameAndSurname() {
        if(mailFrom != "") {
            firstName = mailFrom.substringBefore(".")
            lastName = mailFrom.substringAfter(".").substringBefore("@")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeStamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    }

    private fun showProgressBars(){
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        sendNotification()
    }

    private fun hideProgressBars(){
        progressBar.visibility = View.INVISIBLE
        cancelNotification()
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

    private fun showDialogMailTo() {
        val dialogMailTo = Dialog(this)
        dialogMailTo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogMailTo.setCancelable(false)
        dialogMailTo.setContentView(R.layout.mail_to)
        val phoneField  = dialogMailTo.findViewById(R.id.mailToEditTextEmailAddress) as EditText
        phoneField.setText(mailTo)
        val okBtn = dialogMailTo.findViewById(R.id.okBtn) as Button
        okBtn.setOnClickListener {
            mailTo = phoneField.text.toString()
            saveData("MailTo", mailTo)
            dialogMailTo.dismiss()
            makeToast("Saved")
        }
        dialogMailTo.show()
    }

    private fun makeToast(text: String) {
        val toast = Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM,0,200)
        toast.show()
    }

    private fun saveData(value : String, string: String) {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(value, string)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        mailTo = sharedPreferences.getString("MailTo", "")!!
        mailFrom = sharedPreferences.getString("MailFrom", "")!!
        mailPass = sharedPreferences.getString("MailFromPass", "")!!
    }

    private fun startCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = createImageFile()
        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(this, "com.example.duoninvoicesender.fileprovider", photoFile!!)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startForResult.launch(cameraIntent)
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            showDialogPaymentMethod()
        }
    }

    private fun sendMail() {
        showProgressBars()
        val sendThread = Thread {
            try {
                Transport.send(plainMail())
                this@MainActivity.runOnUiThread {
                    hideProgressBars()
                    makeToast("Mail sent")
                }
            }
            catch(e: AuthenticationFailedException) {
                e.printStackTrace()
                this@MainActivity.runOnUiThread {
                    hideProgressBars()
                    makeToast("Something went wrong, check your account info")
                }
            }
            catch(e: SendFailedException) {
                e.printStackTrace()
                this@MainActivity.runOnUiThread {
                    hideProgressBars()
                    makeToast("Something went wrong, check your account info")
                }
            }
            catch(e: MessagingException) {
                e.printStackTrace()
                this@MainActivity.runOnUiThread {
                    hideProgressBars()
                    makeToast("Something went wrong, check your account info")
                }
            }
        }
        sendThread.priority = 10
        sendThread.start()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val imageFileName = "temp_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun plainMail(): MimeMessage {
        val tos = arrayListOf(mailTo) //Multiple recipients
        val from = mailFrom //Sender email
        val properties = System.getProperties()
        with(properties) {
            put("mail.smtp.host", "smtp.office365.com") //Configure smtp host
            put("mail.smtp.port", "587") //Configure port
            put("mail.smtp.starttls.enable", "true") //Enable TLS
            put("mail.smtp.auth", "true") //Enable authentication
        }
        val auth = object : Authenticator() { override fun getPasswordAuthentication() = PasswordAuthentication(from, mailPass) } //Credentials of the sender email
        val session = Session.getInstance(properties, auth)
        val message = MimeMessage(session)
        val multipart = MimeMultipart("related")
        val messageBodyPart1 = MimeBodyPart()
        val text = "W załączeniu skan faktury." + "<br>" + "<br>" + "Płatność: " + radioButton.text.toString() + "<br>" + "<br>" + "Pozdrawiam," + "<br>" + firstName?.get(0)?.uppercase() + firstName?.drop(1) + " " + lastName?.get(0)?.uppercase() + lastName?.drop(1)
        messageBodyPart1.setContent(text, "text/html; charset=UTF-8")
        multipart.addBodyPart(messageBodyPart1)
        val messageBodyPart2 = MimeBodyPart()
        val fds = FileDataSource(mCurrentPhotoPath)
        messageBodyPart2.dataHandler = DataHandler(fds)
        messageBodyPart2.fileName = lastName + firstName?.get(0) + "_FV_" + radioButton.text.toString().replace(" ", "_").replace("ż", "z").replace("ó", "o").replace("ł", "l").lowercase() + "_" + timeStamp() + ".jpg"
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
                //put("mail.smtp.host", "smtp.office365.com") //Configure smtp host
                //put("mail.smtp.port", "587") //Configure port
                //put("mail.smtp.starttls.enable", "true") //Enable TLS
                put("mail.smtp.auth", "true") //Enable authentication
            }
            val session = Session.getInstance(properties, null)
            val transport = session.getTransport("smtp")
            transport.connect("smtp.office365.com", 587, mailFrom, mailPass)
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

}