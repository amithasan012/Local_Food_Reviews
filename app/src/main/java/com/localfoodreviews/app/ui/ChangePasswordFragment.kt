package com.localfoodreviews.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.localfoodreviews.app.databinding.FragmentChangePasswordBinding
import com.localfoodreviews.app.util.show
import com.localfoodreviews.app.util.textValue

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.updatePasswordButton.setOnClickListener { changePassword() }
    }

    private fun changePassword() {
        val old = binding.oldPasswordInput.textValue()
        val new = binding.newPasswordInput.textValue()
        val confirm = binding.confirmPasswordInput.textValue()
        listOf(binding.oldPasswordInput, binding.newPasswordInput, binding.confirmPasswordInput).forEach { it.error = null }
        when {
            old.isBlank() -> binding.oldPasswordInput.error = "Enter your current password."
            new.length < 6 -> binding.newPasswordInput.error = "Use at least 6 characters."
            new == old -> binding.newPasswordInput.error = "New password must be different."
            new != confirm -> binding.confirmPasswordInput.error = "Passwords do not match."
            else -> {
                val user = auth.currentUser
                val email = user?.email
                if (user == null || email.isNullOrBlank()) {
                    binding.message.apply { text = "Please sign in again."; show(true) }
                    return
                }
                setLoading(true)
                val credential = EmailAuthProvider.getCredential(email, old)
                user.reauthenticate(credential)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Current password is incorrect.")
                        user.updatePassword(new)
                    }
                    .addOnSuccessListener {
                        setLoading(false)
                        binding.oldPasswordInput.editText?.text?.clear()
                        binding.newPasswordInput.editText?.text?.clear()
                        binding.confirmPasswordInput.editText?.text?.clear()
                        binding.message.text = "Password updated successfully."
                        binding.message.show(true)
                    }
                    .addOnFailureListener { error ->
                        setLoading(false)
                        binding.message.text = error.localizedMessage ?: "Could not update password."
                        binding.message.show(true)
                    }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.updatePasswordButton.isEnabled = !loading
        binding.progress.show(loading)
        if (loading) binding.message.show(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
