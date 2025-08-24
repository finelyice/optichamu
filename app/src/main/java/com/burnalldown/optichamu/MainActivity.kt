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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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
    private var currentBatch by mutableStateOf(0)
    private var totalBatches by mutableStateOf(0)

    @Preview
    @Composable
    fun MainScreen() {
        var showDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var formatFilterEnabled by remember { mutableStateOf(false) }
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
                    text = "optichamu",
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
                    scope.launch { compressImages(formatFilterEnabled) { showDialog = it; isFolderSelected = false } }

                },
                    enabled = isFolderSelected) {
                    Text(stringResource(id = R.string.compress))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formatFilterEnabled,
                        onCheckedChange = { formatFilterEnabled = it }
                    )
                    Text(stringResource(id = R.string.format_filter))
                }
            }
            if (showDialog) {
                ProgressDialog(currentBatch, totalBatches)
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
                Toast.makeText(this, getString(R.string.folder_selected), Toast.LENGTH_SHORT).show()
            } else {
                isFolderSelected = false // 更新状态
                Toast.makeText(this, getString(R.string.folder_hint), Toast.LENGTH_SHORT).show()
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
    fun ProgressDialog(currentBatch: Int, totalBatches: Int) {
        Dialog(onDismissRequest = { }) {
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
                        if(currentBatch == 0) Text(stringResource(id = R.string.loading))
                        else{
                            Text(stringResource(id = R.string.compressing))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$currentBatch / $totalBatches")
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
    private suspend fun compressImages(formatFilterEnabled: Boolean, onShowDialogChange: (Boolean) -> Unit) {
        val folderUri = selectedFolderUri ?: run {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.folder_hint), Toast.LENGTH_SHORT).show()
            }
            return
        }

        onShowDialogChange(true)

        withContext(Dispatchers.IO) {
            val documentFile = DocumentFile.fromTreeUri(this@MainActivity, folderUri)
            if (documentFile != null && documentFile.isDirectory) {
                val imageFiles = if (formatFilterEnabled) {
                    documentFile.listFiles().filter { file ->
                        file.isFile && file.type?.startsWith("image/") == true && file.type != "image/gif"
                    }
                } else {
                    documentFile.listFiles().filter { file ->
                        file.isFile
                    }
                }
                totalBatches = imageFiles.size
                val limitedContext = Dispatchers.IO.limitedParallelism(Runtime.getRuntime().availableProcessors())
                val completedCount = AtomicInteger(0)
                val jobs = imageFiles.map { file ->
                    async(limitedContext) {
                        compressImage(file)
                        val current = completedCount.incrementAndGet()
                        withContext(Dispatchers.Main) {
                            currentBatch = current // 更新已完成的计数
                        }
                    }
                }
                jobs.awaitAll()
            } else {
                Log.e("Compression", "Invalid folder selection")
            }
        }

        onShowDialogChange(false)

        runOnUiThread {
            Toast.makeText(this, getString(R.string.compress_success), Toast.LENGTH_SHORT).show()
        }

        currentBatch = 0
        totalBatches = 0
    }

    private suspend fun compressImage(file: DocumentFile) {
        // 获取文件类型并确定压缩格式
//        val format = when (file.type?.substringAfterLast("/")) {
//            "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
//            "png" -> Bitmap.CompressFormat.PNG
//            "webp" -> Bitmap.CompressFormat.WEBP
//            else -> return  // 不支持的类型直接跳过
//        }

        // 解码图片
        val bitmap = contentResolver.openInputStream(file.uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: run {
            Log.e("Compression", "Failed to decode: ${file.name}")
            return
        }

        // 直接覆盖原文件
        try {
            contentResolver.openOutputStream(file.uri, "wt")?.use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)) {
                    Log.e("Compression", "Compression failed: ${file.name}")
                }
            } ?: Log.e("Compression", "Can't open output stream for: ${file.name}")
        } catch (e: SecurityException) {
            Log.e("Compression", "Permission denied for: ${file.name}", e)
        } catch (e: Exception) {
            Log.e("Compression", "Error processing: ${file.name}", e)
        }
        // 释放资源
        bitmap.recycle()
    }
}
