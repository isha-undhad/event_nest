package com.example.eventnest.repositories

import android.util.Log
import com.example.eventnest.models.Category
import com.example.eventnest.utils.awaitTask
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val categoriesCollection = firestore.collection(COLLECTION_CATEGORIES)

    suspend fun getAllCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = categoriesCollection.get().awaitTask()
            val categories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.apply {
                    if (categoryId.isBlank()) {
                        categoryId = doc.id
                    }
                }
            }
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCategoryById(categoryId: String): Result<Category?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = categoriesCollection.document(categoryId).get().awaitTask()
            val category = snapshot.toObject(Category::class.java)?.apply {
                if (this.categoryId.isBlank()) {
                    this.categoryId = snapshot.id
                }
            }
            Result.success(category)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching category $categoryId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createCategory(category: Category): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (category.categoryId.isNotBlank()) {
                categoriesCollection.document(category.categoryId)
            } else {
                categoriesCollection.document()
            }
            category.categoryId = docRef.id
            docRef.set(category, SetOptions.merge()).awaitTask()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating category: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = categoriesCollection.limit(1).get().awaitTask()
            if (snapshot.isEmpty) {
                Log.d(TAG, "Categories collection is empty. Seeding default categories...")
                val defaults = listOf(
                    Category("cat_music", "Music", ""),
                    Category("cat_arts", "Performing & Visual Arts", ""),
                    Category("cat_holidays", "Holidays", ""),
                    Category("cat_hobbies", "Hobbies", ""),
                    Category("cat_food", "Food & Drink", "")
                )
                for (cat in defaults) {
                    categoriesCollection.document(cat.categoryId).set(cat).awaitTask()
                }
                Log.d(TAG, "Default 5 categories seeded successfully.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed categories: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "CategoryRepository"
        const val COLLECTION_CATEGORIES = "categories"
    }
}
