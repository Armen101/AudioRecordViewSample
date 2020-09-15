package com.example.audiorecordviewsample

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.visualizer.amplitude.AudioRecordView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.*


open class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private var timer: Timer? = null
    private var recorder: MediaRecorder? = null

    private var audioFile: File? = null

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var audioRecordView: AudioRecordView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById(R.id.startRecording)
        stopButton = findViewById(R.id.stopRecording)
        audioRecordView = findViewById(R.id.audioRecordView)
        setSwitchListeners()
    }

    fun startRecording(view: View) {
        if (!permissionsIsGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 200)
            return
        }

        startButton.isEnabled = false
        stopButton.isEnabled = true
        //Creating file
        try {
            audioFile = File.createTempFile("audio", "tmp", cacheDir)
        } catch (e: IOException) {
            Log.e(MainActivity::class.simpleName, e.message ?: e.toString())
            return
        }
        //Creating MediaRecorder and specifying audio source, output format, encoder & output format
        recorder = MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            setAudioSamplingRate(48000)
            setAudioEncodingBitRate(48000)
            prepare()
            start()
        }

        startDrawing()
    }

    fun stopRecording(view: View) {
        startButton.isEnabled = true
        stopButton.isEnabled = false
        //stopping recorder
        recorder?.apply {
            stop()
            release()
        }
        stopDrawing()
    }

    private fun setSwitchListeners() {
        switchAlignTo.setOnCheckedChangeListener { _, isChecked ->
            audioRecordView.chunkAlignTo = if (isChecked) {
                AudioRecordView.AlignTo.CENTER
            } else {
                AudioRecordView.AlignTo.BOTTOM
            }
        }
        switchRoundedCorners.setOnCheckedChangeListener { _, isChecked ->
            audioRecordView.chunkRoundedCorners = isChecked
        }
        switchSoftTransition.setOnCheckedChangeListener { _, isChecked ->
            audioRecordView.chunkSoftTransition = isChecked
        }
    }

    private fun startDrawing() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val currentMaxAmplitude = recorder?.maxAmplitude
                audioRecordView.update(currentMaxAmplitude ?: 0) //redraw view
            }
        }, 0, 100)
    }

    private fun stopDrawing() {
        timer?.cancel()
        audioRecordView.recreate()
    }

    private fun permissionsIsGranted(perms: Array<String>): Boolean {
        for (perm in perms) {
            val checkVal: Int = checkCallingOrSelfPermission(perm)
            if (checkVal != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        startRecording(View(this))
    }
}
