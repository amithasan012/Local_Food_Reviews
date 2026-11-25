package com.localfoodreviews.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.localfoodreviews.app.R
import com.localfoodreviews.app.data.Review
import com.localfoodreviews.app.databinding.ItemReviewBinding
import com.localfoodreviews.app.util.ImageCodec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter(private val onEditClick: ((Review) -> Unit)? = null) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private val items = mutableListOf<Review>()

    fun submitList(newItems: List<Review>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) = with(binding) {
            restaurantName.text = review.restaurantName
            cuisine.text = root.context.getString(R.string.cuisine_format, review.cuisine)
            rating.text = root.context.getString(R.string.rating_format, review.rating)
            usernameAndDate.text = root.context.getString(
                R.string.by_user_on_date,
                review.username,
                review.createdAt?.toDate()?.let(::formatDate) ?: root.context.getString(R.string.just_now)
            )
            caption.text = review.caption
            if (review.photoBase64.isNotBlank()) ImageCodec.loadDataUri(photo, review.photoBase64)
            else photo.setImageResource(android.R.drawable.ic_menu_report_image)
            editButton.isVisible = onEditClick != null
            editButton.setOnClickListener { onEditClick?.invoke(review) }
        }
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(date)
}
