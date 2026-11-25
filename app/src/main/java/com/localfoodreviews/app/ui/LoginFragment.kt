package com.localfoodreviews.app.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.localfoodreviews.app.R
import com.localfoodreviews.app.databinding.FragmentLoginBinding
import com.localfoodreviews.app.util.show
import com.localfoodreviews.app.util.textValue

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (auth.currentUser != null) {
            goToFeed()
            return
        }
        binding.loginButton.setOnClickListener { login() }
        binding.createAccountLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.forgotPasswordLink.setOnClickListener { showEmailResetDialog() }
    }

    private fun login() {
        val email = binding.emailInput.textValue()
        val password = binding.passwordInput.textValue()
        binding.emailInput.error = null
        binding.passwordInput.error = null
        when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.emailInput.error = "Enter a valid email address."
            password.isBlank() -> binding.passwordInput.error = "Password is required."
            else -> {
                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { goToFeed() }
                    .addOnFailureListener { error ->
                        setLoading(false)
                        binding.message.text = error.localizedMessage ?: "Sign-in failed. Please try again."
                        binding.message.show(true)
                    }
            }
        }
    }

    private fun showEmailResetDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.email)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(56, 16, 56, 8)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.send_reset_email)
            .setMessage(R.string.reset_email_description)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.send) { _, _ ->
                val email = input.text.toString().trim()
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener { binding.message.apply { text = getString(R.string.reset_email_sent); show(true) } }
                        .addOnFailureListener { binding.message.apply { text = it.localizedMessage ?: getString(R.string.request_failed); show(true) } }
                } else {
                    binding.message.apply { text = getString(R.string.valid_email_required); show(true) }
                }
            }
            .show()
    }

    private fun goToFeed() {
        if (!isAdded) return
        findNavController().navigate(R.id.feedFragment, null,
            androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build())
    }

    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        binding.progress.show(loading)
        if (loading) binding.message.show(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
