package com.localfoodreviews.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.localfoodreviews.app.data.FirebaseRepository
import com.localfoodreviews.app.data.Review
import com.localfoodreviews.app.databinding.FragmentFeedBinding
import com.localfoodreviews.app.util.show

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseRepository()
    private val adapter = ReviewAdapter()
    private var listener: ListenerRegistration? = null
    private var allReviews: List<Review> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.reviewsList.layoutManager = LinearLayoutManager(requireContext())
        binding.reviewsList.adapter = adapter
        binding.searchInput.editText?.addTextChangedListener { filter(it?.toString().orEmpty()) }
        binding.swipeRefresh.setOnRefreshListener { startListening() }
        startListening()
    }

    private fun startListening() {
        listener?.remove()
        binding.progress.show(true)
        binding.errorMessage.show(false)
        listener = repository.listenToFeed(
            onChanged = { reviews ->
                if (!isAdded) return@listenToFeed
                binding.progress.show(false)
                binding.swipeRefresh.isRefreshing = false
                allReviews = reviews
                filter(binding.searchInput.editText?.text?.toString().orEmpty())
            },
            onError = { error ->
                if (!isAdded) return@listenToFeed
                binding.progress.show(false)
                binding.swipeRefresh.isRefreshing = false
                binding.errorMessage.text = error.localizedMessage ?: "Could not load reviews. Check your connection."
                binding.errorMessage.show(true)
                binding.emptyMessage.show(allReviews.isEmpty())
            }
        )
    }

    // Case-insensitive, live client-side search avoids an extra query/index for this small free-tier project.
    private fun filter(query: String) {
        val normalized = query.trim().lowercase()
        val visible = if (normalized.isBlank()) allReviews else allReviews.filter {
            it.restaurantNameLowercase.contains(normalized) || it.cuisineLowercase.contains(normalized)
        }
        adapter.submitList(visible)
        binding.emptyMessage.text = if (normalized.isBlank()) "No reviews yet. Add the first one!" else "No matching restaurant or cuisine."
        binding.emptyMessage.show(visible.isEmpty())
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
