package com.burnalldown.optichamu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.burnalldown.optichamu.ui.theme.OptichamuTheme

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.burnalldown.optichamu.ui.theme.OptichamuTheme
import java.io.File
import java.io.FileOutputStream

import android.graphics.BitmapFactory

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.PreviewParameter

import android.os.Environment

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
/*
图片压缩app
 */
class MainActivity : ComponentActivity() {
    private var selectedImages by mutableStateOf<List<Uri>>(emptyList())
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUEST_CODE_STORAGE_PERMISSION = 101
    private val REQUIRED_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var pickImagesLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("123", "onCreate")
        setContent {
            OptichamuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    color = MaterialTheme.colorScheme.background,

                    ) {
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(onClick = { pickImages() }) {
                            Text("选择图片")
                        }
                        Button(onClick = { compressImages() }) {
                            Text("压缩")
                        }
                    }
                }
            }
        }

        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(this, "Permissions granted by the user!!!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            }
        }

        if (allPermissionsGranted()) {
            Toast.makeText(this, "Permissions already granted by the user!!!", Toast.LENGTH_SHORT).show()
            Log.i("222", "Permissions: ${REQUIRED_PERMISSIONS.joinToString()}")
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.i("111", "result: $result")
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val clipData = data?.clipData
                val uris = mutableListOf<Uri>()
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    data?.data?.let { uris.add(it) }
                }
                selectedImages = uris
                for (i in selectedImages) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    try {
                        contentResolver.takePersistableUriPermission(i, flags)
                    } catch (e: SecurityException) {
                        Log.e("Error", "Failed to take persistable URI permission for $i", e)
                    }
                }
                Log.i("666", "selectedImages: $selectedImages")
                Toast.makeText(this, "Selected ${selectedImages.size} images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun pickImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        //intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"))
    }

    private fun compressImages() {
        val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Optichamu")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
            Log.i("777","outputDir: ${outputDir.absolutePath} existed: ${outputDir.exists()}")
        }

        selectedImages.forEach { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                val fileName = "${DocumentFile.fromSingleUri(this, uri)?.name?.substringBeforeLast('.')}.webp"
                val compressedFile = File(outputDir, fileName)
                FileOutputStream(compressedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
                Log.i("777", "compressedFile: ${compressedFile.absolutePath}")
            } else {
                Log.e("777", "Failed to decode bitmap from Uri")
            }
        }
        Toast.makeText(this, "Compressed ${selectedImages.size} images", Toast.LENGTH_SHORT).show()
    }
}