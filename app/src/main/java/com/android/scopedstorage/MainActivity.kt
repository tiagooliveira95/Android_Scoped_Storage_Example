package com.android.scopedstorage

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

const val WRITE_REQUEST_CODE = 1000
const val OPEN_DIRECTORY_REQUEST_CODE = 1001

class MainActivity : AppCompatActivity() {

    var createdFile: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        button_create_file.setOnClickListener{
            val fileName = file_name_et.text.toString()
            if(fileName.isEmpty()){
                Toast.makeText(this,"Provide a file name first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            requestFileCreation(fileName)
        }

        write_to_file.setOnClickListener {

            val data = text_data.text.toString()

            if(createdFile == null){
                Toast.makeText(this,"Create a file first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(data.isEmpty()){
                Toast.makeText(this,"Write some text to write on the file", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val result = writeToFile(createdFile!!, data)
            Toast.makeText(this, "Was write successful:  $result",Toast.LENGTH_LONG).show()
        }

        dump_text.setOnClickListener {
            recycler.visibility = View.GONE
            textData.visibility = View.VISIBLE

            if(createdFile == null){
                Toast.makeText(this,"Create a file first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            textData.text = readFile(createdFile!!)
        }


        request_dir_permission.setOnClickListener {
            requestDirectoryPermission()
        }

        read_files_of_dir.setOnClickListener {
            val defaultUri = PreferenceManager.getDefaultSharedPreferences(this).getString("dir","")!!

            if(defaultUri.isEmpty()){
                Toast.makeText(this,"Default dir is unknown", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val uri = Uri.parse(defaultUri)

            val data = getFilesFromParentUri(uri)
            populateRecycler(data)
        }
    }

    private fun requestFileCreation(filename: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, filename)
        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    private fun writeToFile(fileUri: Uri, text: String): Boolean {
        val p: ParcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w") ?: return false

        val outStream =  OutputStreamWriter(FileOutputStream(p.fileDescriptor))
        outStream.write(text)
        outStream.close()

        return true
    }

    private fun readFile(fileUri: Uri): String? {
        val p: ParcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "r") ?: return null

        val reader = BufferedReader(InputStreamReader(FileInputStream(p.fileDescriptor)))

        val stringBuilder = StringBuilder()

        var line: String? = reader.readLine()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = readLine()
        }

        reader.close()
        return stringBuilder.toString()
    }

    private fun requestDirectoryPermission(){
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }

        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
    }

    private fun getFilesFromParentUri(uri:Uri): List<FileData>{
        val contentResolver: ContentResolver = contentResolver
        var childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))

        val dirNodes: LinkedList<Uri> = LinkedList()
        dirNodes.add(childrenUri)

        val list = arrayListOf<FileData>()

        while (dirNodes.isNotEmpty()) {
            childrenUri = dirNodes.removeAt(0) // get the item from top

            @SuppressLint("Recycle") // Cursor is closed on finally
            val c: Cursor = contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null) ?: throw IOException()
            try {
                while (c.moveToNext()) {
                    val docId: String = c.getString(0)
                    val mime: String = c.getString(1)
                    val displayName = c.getString(2)


                    if (isDirectory(mime)) {
                        val newNode = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
                        dirNodes.add(newNode)
                        list.add(FileData(newNode, displayName,"",true))
                    } else {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId)
                        list.add(FileData(fileUri, displayName,mime,false))
                    }
                }

            } finally {
                closeQuietly(c)
            }
        }
        return list
    }

    private fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (re: RuntimeException) {
                throw re
            } catch (ignore: Exception) {
                // ignore exception
            }
        }
    }

    private fun isDirectory(mimeType: String): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR == mimeType
    }

    private fun populateRecycler(data:List<FileData>) {
        recycler.visibility = View.VISIBLE
        textData.visibility = View.GONE

        val adapter = RecyclerAdapter()
        recycler.adapter = adapter
        adapter.data = data
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == WRITE_REQUEST_CODE) {
                createdFile = data.data!!
            } else if (requestCode == OPEN_DIRECTORY_REQUEST_CODE) {

                data.also { uri ->
                    //Save Director Uri
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString("dir",uri.data.toString())
                        .apply()

                    val contentResolver = applicationContext.contentResolver

                    // Request persistence
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    // Check for the freshest data.
                    contentResolver.takePersistableUriPermission(uri.data!!, takeFlags)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}