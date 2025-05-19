package com.example.quickwork.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageRepository {
    suspend fun uploadImageToCloudinary(context: Context, imageUri: Uri): String? {
        val cloudName = "dytggtwgy"
        val uploadPreset = "quickworks"
        val TAG = "CloudinaryUpload"

        return try {
            withContext(Dispatchers.IO) {
                val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val conn = url.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = DataOutputStream(conn.outputStream)
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val fileBytes = inputStream?.readBytes() ?: return@withContext null
                inputStream.close()

                Log.d(TAG, "Writing upload_preset...")
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                outputStream.writeBytes("$uploadPreset\r\n")

                Log.d(TAG, "Writing file data...")
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n")
                outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                outputStream.write(fileBytes)
                outputStream.writeBytes("\r\n--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Upload success. Response: $response")
                    val imageUrl = Regex("\"url\":\"(.*?)\"").find(response)?.groupValues?.get(1)?.replace("\\/", "/")
                    imageUrl?.replace("http://", "https://")
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "Upload failed. Error response: $errorResponse")
                    null
                }.also {
                    conn.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload: ${e.message}", e)
            null
        }
    }
}