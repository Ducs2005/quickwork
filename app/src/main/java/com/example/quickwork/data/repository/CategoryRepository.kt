package com.example.quickwork.data.repository

import android.util.Log
import com.example.quickwork.data.models.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getCategories(): List<Category> {
        return try {
            val querySnapshot = firestore.collection("category").get().await()
            querySnapshot.documents.mapNotNull { doc ->
                try {
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("CategoryRepository", "Error parsing category ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Failed to load categories", e)
            emptyList()
        }
    }
}