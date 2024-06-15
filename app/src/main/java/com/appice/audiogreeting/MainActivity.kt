package com.appice.audiogreeting

import VideoHandle.EpEditor
import VideoHandle.OnEditorListener
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.plcoding.audiorecorder.playback.AndroidAudioPlayer
import com.plcoding.audiorecorder.record.AndroidAudioRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val player by lazy {
        AndroidAudioPlayer(applicationContext)
    }
    private var audioFile: File? = null

    lateinit var recordButton: Button
    lateinit var stopButton: Button
    lateinit var playButton: Button
    lateinit var stopPlayButton: Button
    private var outPutFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton =  findViewById(R.id.button_record_start)
        stopButton =  findViewById(R.id.button_record_stop)
        playButton =  findViewById(R.id.button_play)
        stopPlayButton =  findViewById(R.id.button_stop_play)
        // Request RECORDING_AUDIO permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )

        // Set up button click listeners
        recordButton.setOnClickListener {
            // Implement recording logic using recorder if permission granted
            File(cacheDir, "audio.mp3").also {
                recorder.start(it)
                audioFile = it
            }
        }
        stopButton.setOnClickListener {
            // Implement recording stop logic using recorder if recording ongoing
            recorder.stop()
        }
        playButton.setOnClickListener {
            // Implement playback logic using player if audio file available
//            audioFile?.let { it1 -> player.playFile(it1) }
            createVideo()

        }
        stopPlayButton.setOnClickListener{
            player.stop()
        }
    }
    fun createVideo(){
        var inputImage = saveDrawableImageToCache(applicationContext, R.drawable.quotes_bg,"temp.png")
        val audioFile: File? = this.audioFile
        val inputAudio = if (audioFile != null) audioFile.absolutePath else null
        val outputVideoFile: File? = createVideoOutputFile(applicationContext)
        val output = if (outputVideoFile != null) outputVideoFile.absolutePath else null

        // Construct command for EpEditor
        // Construct command for EpEditor
//        val command = "ffmpeg -i $inputImage -i $inputAudio -c:v libx264 -c:a aac $output"
        val command = "-i $inputImage -i $inputAudio -c:v libx264 -c:a aac $output" // isme image visible nahi rahi all the time
//        val command = "-loop 1 -i $inputImage -i $inputAudio -c:v libx264 -c:a aac -shortest $output" // isme image visible nahi rahi all the time
//        val command = "-i $inputImage -i $$inputAudio -c:v libx264 -tune stillimage -c:a copy $$output"
        // Print debug information

        // Print debug information
        System.out.println("Input Image: $inputImage")
        println("Input Audio: $inputAudio")
        println("Output File: $output")
        println("Command: $command")
        // Execute the command with EpEditor
        val timeout = 10000L  // Replace with actual timeout if needed
        EpEditor.execCmd(command,timeout,object : OnEditorListener{
            override fun onSuccess() {
                setOutPutFile(File(output))
                shareVideo(getOutPutFile())
            }

            override fun onFailure() {
                println("Video creation failed.")
            }

            override fun onProgress(progress: Float) {
                println("Progress: $progress")
            }

        })
//        File(inputImage).delete()
    }
    fun setOutPutFile(file: File) {
        this.outPutFile = file
    }
    fun getOutPutFile(): File? {
        return outPutFile
    }
    fun shareVideo(videoFile: File?) {
        val videoUri: Uri? = if (videoFile != null) {
            FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                videoFile
            )
        } else {
            null
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            val chooserIntent = Intent.createChooser(shareIntent, "Share Video")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(chooserIntent)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }


    fun saveDrawableImageToCache(context: Context, drawableResourceId: Int, fileName: String): String? {
        requireNotNull(context) { "context" }
        requireNotNull(fileName) { "fileName" }

        val bitmap = BitmapFactory.decodeResource(context.resources, drawableResourceId)
        val cacheDir = File(context.cacheDir, "image_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val outputFile = File(cacheDir, fileName)
        return try {
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            outputFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, enable recording functionality
        } else {
            // Handle permission denied scenario appropriately
        }
    }
    fun createVideoOutputFile(context: Context): File? {
        requireNotNull(context) { "context" }

        val fileName = "video_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".mp4"
        val cacheDir = File(context.cacheDir, "videos")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        return try {
            val outputFile = File(cacheDir, fileName)
            val parentFile = outputFile.parentFile
            parentFile?.mkdirs()
            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
