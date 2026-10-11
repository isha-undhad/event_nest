package com.example.eventnest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.R
import com.example.eventnest.models.Category

class CategoryAdapter(
    private var categories: List<Category> = emptyList(),
    private var selectedCategoryId: String = "all",
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    fun submitList(newCategories: List<Category>, currentSelectedId: String = selectedCategoryId) {
        categories = newCategories
        selectedCategoryId = currentSelectedId
        notifyDataSetChanged()
    }

    fun setSelectedCategory(categoryId: String) {
        if (selectedCategoryId != categoryId) {
            selectedCategoryId = categoryId
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_chip, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(category: Category) {
            tvCategoryName.text = category.categoryName
            val isSelected = category.categoryId.equals(selectedCategoryId, ignoreCase = true)

            if (isSelected) {
                tvCategoryName.setBackgroundResource(R.drawable.bg_category_chip_selected)
                tvCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_on_primary))
            } else {
                tvCategoryName.setBackgroundResource(R.drawable.bg_category_chip_unselected)
                tvCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            }

            itemView.setOnClickListener {
                val current = categories[bindingAdapterPosition]
                selectedCategoryId = current.categoryId
                notifyDataSetChanged()
                onCategorySelected(current)
            }
        }
    }
}
