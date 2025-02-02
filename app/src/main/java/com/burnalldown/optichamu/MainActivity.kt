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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.withContext

/*
图片压缩app
 */
class MainActivity : ComponentActivity() {
    private lateinit var pickFolderLauncher: ActivityResultLauncher<Intent>
    private var selectedFolderUri: Uri? = null
    private val REQUIRED_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private val context = this
    private var isFolderSelected by mutableStateOf(false)

    @Preview
    @Composable
    fun MainScreen() {
        var showDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            HelpButton(onAboutClick = { showAboutDialog = true })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "optichamu!",
                    style = TextStyle(
                        fontWeight = FontWeight.W900, //设置字体粗细
                        fontSize = 64.sp,
                        letterSpacing = 4.sp
                    )
                )
                Spacer(modifier = Modifier.height(64.dp))
                Button(onClick = { scope.launch { pickFolder() } }) {
                    Text(stringResource(id = R.string.select_image))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch { compressImages { showDialog = it; isFolderSelected = false } }

                },
                    enabled = isFolderSelected) {
                    Text(stringResource(id = R.string.compress))
                }
            }
            if (showDialog) {
                ProgressDialog()
            }
            if (showAboutDialog) {
                AboutDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("123", "onCreate")

        setContent {
            OptichamuTheme {
                MainScreen()
            }
        }

        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.i("222", "Permissions: ${REQUIRED_PERMISSIONS.joinToString()}")
            } else {
                Log.i("222", "Permissions not granted by the user!!!")
            }
        }

        if (allPermissionsGranted()) {
            Log.i("222", "Permissions: ${REQUIRED_PERMISSIONS.joinToString()}")
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedFolderUri = result.data?.data
                Log.i("666", "selectedFolderUri: $selectedFolderUri")
                isFolderSelected = true // 更新状态
            } else {
                isFolderSelected = false // 更新状态
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    @Composable
    fun HelpButton(onAboutClick: () -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .padding(16.dp)
                .wrapContentSize(Alignment.TopStart)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    onAboutClick()
                }, text = { Text(stringResource(id = R.string.about)) })
            }
        }
    }

    @Composable
    fun AboutDialog(onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.about),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(id = R.string.about_text))
                    Text(text= stringResource(id =R.string.about_source))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClickableText(
                        text = AnnotatedString(
                            text = stringResource(id = R.string.about_source_url),
                            spanStyle = SpanStyle(textDecoration = TextDecoration.Underline, color = Color.Blue)
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_source_url)))
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        }
    }
    @Composable
    fun ProgressDialog() {
        Dialog(onDismissRequest = {  }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.compressing))
                    }
                }
            }
        }
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        pickFolderLauncher.launch(intent)
    }

    //TODO:修改时间，性能优化
    private suspend fun compressImages(onShowDialogChange: (Boolean) -> Unit) {
        val folderUri = selectedFolderUri ?: run {
            runOnUiThread { // Use runOnUiThread for Toast
                Toast.makeText(this, getString(R.string.folder_hint), Toast.LENGTH_SHORT).show()
            }
            return
        }
        onShowDialogChange(true)
        withContext(Dispatchers.IO){
            val documentFile = DocumentFile.fromTreeUri(this@MainActivity, folderUri)
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
            } else {
                Log.e("777", "Selected folder is not valid")
            }
        }

        onShowDialogChange(false)

        runOnUiThread { // Use runOnUiThread for Toast
            Toast.makeText(this, getString(R.string.compress_success), Toast.LENGTH_SHORT).show()
        }
    }
}
