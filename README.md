# Android Scoped Storage 


### How can I request premission to acess a folder ?

```kotlin
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }

startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
```  


### How can I obtain an InputStream from a file Uri ?

```kotlin

 val p: ParcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "r") ?: return null

 val reader = BufferedReader(InputStreamReader(FileInputStream(p.fileDescriptor)))

```
