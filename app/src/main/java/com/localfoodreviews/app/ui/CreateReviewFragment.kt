package com.localfoodreviews.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.localfoodreviews.app.data.FirebaseRepository
import com.localfoodreviews.app.databinding.FragmentCreateReviewBinding
import com.localfoodreviews.app.util.ImageCodec
import com.localfoodreviews.app.util.show
import com.localfoodreviews.app.util.textValue

class CreateReviewFragment : Fragment() {
    private var _binding: FragmentCreateReviewBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseRepository()
    private var selectedPhoto: Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(it)
            selectedPhoto = it
            binding.photoPreview.setImageURI(it)
            binding.photoHint.text = "Photo selected"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.choosePhotoButton.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.postButton.setOnClickListener { postReview() }
    }

    private fun postReview() {
        val restaurant = binding.restaurantInput.textValue()
        val cuisine = binding.cuisineInput.textValue()
        val caption = binding.captionInput.textValue()
        val rating = binding.ratingBar.rating.toLong()
        listOf(binding.restaurantInput, binding.cuisineInput, binding.captionInput).forEach { it.error = null }
        when {
            restaurant.length !in 2..60 -> binding.restaurantInput.error = "Use 2–60 characters."
            cuisine.length !in 2..30 -> binding.cuisineInput.error = "Use 2–30 characters."
            rating !in 1L..5L -> binding.ratingHint.text = "Choose a rating from 1 to 5."
            caption.length !in 10..1024 -> binding.captionInput.error = "Your review must be 10–1024 characters."
            selectedPhoto == null -> binding.message.apply { text = "Choose one food photo first."; show(true) }
            else -> {
                setLoading(true)
                ImageCodec.toSmallDataUri(requireContext(), selectedPhoto!!)
                    .onSuccess { photoData ->
                        repository.addReview(restaurant, cuisine, rating, caption, photoData) { result ->
                            if (!isAdded) return@addReview
                            setLoading(false)
                            result.onSuccess {
                                binding.message.text = "Review posted!"
                                binding.message.show(true)
                                clearForm()
                                findNavController().popBackStack()
                            }.onFailure { error ->
                                binding.message.text = error.localizedMessage ?: "Could not post your review."
                                binding.message.show(true)
                            }
                        }
                    }
                    .onFailure { error ->
                        setLoading(false)
                        binding.message.text = error.localizedMessage ?: "Could not prepare this image."
                        binding.message.show(true)
                    }
            }
        }
    }

    private fun clearForm() = with(binding) {
        restaurantInput.editText?.text?.clear()
        cuisineInput.editText?.text?.clear()
        captionInput.editText?.text?.clear()
        ratingBar.rating = 0f
        selectedPhoto = null
        photoPreview.setImageResource(android.R.drawable.ic_menu_report_image)
        photoHint.text = "One photo is required"
    }

    private fun setLoading(loading: Boolean) {
        binding.postButton.isEnabled = !loading
        binding.choosePhotoButton.isEnabled = !loading
        binding.progress.show(loading)
        if (loading) binding.message.show(false)
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
