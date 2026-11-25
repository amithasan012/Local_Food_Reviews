package com.localfoodreviews.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.localfoodreviews.app.data.FirebaseRepository
import com.localfoodreviews.app.data.Review
import com.localfoodreviews.app.databinding.FragmentMyReviewsBinding
import com.localfoodreviews.app.util.ImageCodec
import com.localfoodreviews.app.util.show
import com.localfoodreviews.app.util.textValue

/** Editing happens in the top panel of this page, never on a separate screen. */
class MyReviewsFragment : Fragment() {
    private var _binding: FragmentMyReviewsBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseRepository()
    private val adapter = ReviewAdapter(::openEditor)
    private var listener: ListenerRegistration? = null
    private var editingReview: Review? = null
    private var replacementPhoto: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(it)
            replacementPhoto = it
            binding.editPhotoPreview.setImageURI(it)
            binding.editPhotoHint.text = "New photo selected"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.reviewsList.layoutManager = LinearLayoutManager(requireContext())
        binding.reviewsList.adapter = adapter
        binding.changePhotoButton.setOnClickListener { imagePicker.launch(arrayOf("image/*")) }
        binding.saveButton.setOnClickListener { saveChanges() }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.cancelEditButton.setOnClickListener { closeEditor() }
        startListening()
    }

    private fun startListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        listener?.remove()
        binding.progress.show(true)
        listener = repository.listenToMyReviews(uid,
            onChanged = { reviews ->
                if (!isAdded) return@listenToMyReviews
                binding.progress.show(false)
                adapter.submitList(reviews)
                binding.emptyMessage.show(reviews.isEmpty())
            },
            onError = { error ->
                if (!isAdded) return@listenToMyReviews
                binding.progress.show(false)
                binding.errorMessage.text = error.localizedMessage ?: "Could not load your reviews."
                binding.errorMessage.show(true)
            }
        )
    }

    private fun openEditor(review: Review) = with(binding) {
        editingReview = review
        replacementPhoto = null
        editPanel.show(true)
        editRestaurantInput.editText?.setText(review.restaurantName)
        editCuisineInput.editText?.setText(review.cuisine)
        editCaptionInput.editText?.setText(review.caption)
        editRatingBar.rating = review.rating.toFloat()
        ImageCodec.loadDataUri(editPhotoPreview, review.photoBase64)
        editPhotoHint.text = "Keep current photo or choose a replacement"
        editPanel.requestFocus()
        scrollContainer.smoothScrollTo(0, 0)
    }

    private fun saveChanges() {
        val review = editingReview ?: return
        val restaurant = binding.editRestaurantInput.textValue()
        val cuisine = binding.editCuisineInput.textValue()
        val caption = binding.editCaptionInput.textValue()
        val rating = binding.editRatingBar.rating.toLong()
        listOf(binding.editRestaurantInput, binding.editCuisineInput, binding.editCaptionInput).forEach { it.error = null }
        when {
            restaurant.length !in 2..60 -> binding.editRestaurantInput.error = "Use 2–60 characters."
            cuisine.length !in 2..30 -> binding.editCuisineInput.error = "Use 2–30 characters."
            rating !in 1L..5L -> binding.editRatingHint.text = "Choose a rating from 1 to 5."
            caption.length !in 10..1024 -> binding.editCaptionInput.error = "Your review must be 10–1024 characters."
            else -> {
                setLoading(true)
                val imageResult = replacementPhoto?.let { ImageCodec.toSmallDataUri(requireContext(), it) }
                    ?: Result.success(review.photoBase64)
                imageResult.onSuccess { image ->
                    repository.updateReview(review, restaurant, cuisine, rating, caption, image) { result ->
                        if (!isAdded) return@updateReview
                        setLoading(false)
                        result.onSuccess { closeEditor() }
                            .onFailure { showEditError(it.localizedMessage ?: "Could not save your changes.") }
                    }
                }.onFailure {
                    setLoading(false)
                    showEditError(it.localizedMessage ?: "Could not prepare this image.")
                }
            }
        }
    }

    private fun confirmDelete() {
        val review = editingReview ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete review?")
            .setMessage("This will permanently remove your review and its photo.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                setLoading(true)
                repository.deleteReview(review.id) { result ->
                    if (!isAdded) return@deleteReview
                    setLoading(false)
                    result.onSuccess { closeEditor() }
                        .onFailure { showEditError(it.localizedMessage ?: "Could not delete this review.") }
                }
            }.show()
    }

    private fun showEditError(message: String) {
        binding.editMessage.text = message
        binding.editMessage.show(true)
    }

    private fun closeEditor() = with(binding) {
        editingReview = null
        replacementPhoto = null
        editPanel.show(false)
        editMessage.show(false)
    }

    private fun setLoading(loading: Boolean) {
        binding.saveButton.isEnabled = !loading
        binding.deleteButton.isEnabled = !loading
        binding.changePhotoButton.isEnabled = !loading
        binding.progress.show(loading)
        if (loading) binding.editMessage.show(false)
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    private fun persistReadPermission(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not support persistence; the immediate read permission still works.
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
