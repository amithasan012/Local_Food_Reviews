package com.localfoodreviews.app.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.localfoodreviews.app.R
import com.localfoodreviews.app.data.FirebaseRepository
import com.localfoodreviews.app.databinding.FragmentRegisterBinding
import com.localfoodreviews.app.util.show
import com.localfoodreviews.app.util.textValue

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseRepository()
    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.registerButton.setOnClickListener { register() }
        binding.loginLink.setOnClickListener { findNavController().navigateUp() }
    }

    private fun register() {
        val username = binding.usernameInput.textValue()
        val email = binding.emailInput.textValue()
        val password = binding.passwordInput.textValue()
        val confirm = binding.confirmPasswordInput.textValue()
        listOf(binding.usernameInput, binding.emailInput, binding.passwordInput, binding.confirmPasswordInput).forEach { it.error = null }
        when {
            !usernameRegex.matches(username) -> binding.usernameInput.error = "Use 3–20 letters, numbers, or underscore."
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.emailInput.error = "Enter a valid email address."
            password.length < 6 -> binding.passwordInput.error = "Use at least 6 characters."
            password != confirm -> binding.confirmPasswordInput.error = "Passwords do not match."
            else -> {
                setLoading(true)
                repository.createAccount(username, email, password) { result ->
                    if (!isAdded) return@createAccount
                    setLoading(false)
                    result.onSuccess {
                        findNavController().navigate(R.id.feedFragment, null,
                            androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build())
                    }.onFailure { error ->
                        binding.message.text = error.localizedMessage ?: "Could not create the account."
                        binding.message.show(true)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.registerButton.isEnabled = !loading
        binding.progress.show(loading)
        if (loading) binding.message.show(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
