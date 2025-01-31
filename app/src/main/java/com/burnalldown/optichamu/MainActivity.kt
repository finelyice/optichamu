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
    private lateinit var pickFolderLauncher: ActivityResultLauncher<Intent>
    private var selectedFolderUri: Uri? = null
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
                        Button(onClick = { pickFolder() }) {
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

        pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedFolderUri = result.data?.data
                Log.i("666", "selectedFolderUri: $selectedFolderUri")
                Toast.makeText(this, "Selected folder: $selectedFolderUri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        pickFolderLauncher.launch(intent)
    }

    //TODO:修改时间，资源释放
    private fun compressImages() {
        val folderUri = selectedFolderUri ?: run {
            Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show()
            return
        }

        val documentFile = DocumentFile.fromTreeUri(this, folderUri)
        if (documentFile != null && documentFile.isDirectory) {
            documentFile.listFiles().forEach { file ->
                if (file.isFile && file.type?.startsWith("image/") == true) {
                    val fileName = "${file.name?.substringBeforeLast('.')}.webp"
                    //已经存在则跳过
                    if (documentFile.findFile(fileName) != null) {
                        return@forEach
                    }
                    val inputStream = contentResolver.openInputStream(file.uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {

                        val compressedFile = documentFile.createFile("image/webp", fileName)
                        if (compressedFile != null) {
                            contentResolver.openOutputStream(compressedFile.uri)?.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                            }
                            Log.i("777", "compressedFile: ${compressedFile.uri}")
                            // Delete the original file
                            val filename=file.name
                            if (file.delete()) {
                                if (filename != null) {
                                    compressedFile.renameTo(filename)
                                }
                            }
                        } else {
                            Log.e("777", "Failed to create compressed file")
                        }
                    } else {
                        Log.e("777", "Failed to decode bitmap from Uri")
                    }
                }
            }
            Toast.makeText(this, "Compressed images in the selected folder", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("777", "Selected folder is not valid")
        }
    }
}
