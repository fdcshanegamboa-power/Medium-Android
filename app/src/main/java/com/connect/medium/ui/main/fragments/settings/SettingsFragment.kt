package com.connect.medium.ui.main.fragments.settings

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.connect.medium.R
import com.connect.medium.databinding.FragmentSettingsBinding
import com.connect.medium.ui.auth.AuthActivity
import com.connect.medium.ui.auth.AuthViewModel
import com.connect.medium.ui.auth.AuthViewModelFactory
import com.connect.medium.utils.ThemePreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set navigation icon tint to adapt to theme
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.foreground)
        )

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.itemLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        setupDarkMode()
    }

    private fun setupDarkMode() {
        // enable the switch
        binding.switchDarkMode.isEnabled = true

        // load saved preference and set switch state
        lifecycleScope.launch {
            ThemePreferences.getDarkMode(requireContext())
                .collect { isDark ->
                    // remove listener before setting checked to avoid loop
                    binding.switchDarkMode.setOnCheckedChangeListener(null)
                    binding.switchDarkMode.isChecked = isDark

                    // re-attach listener after setting value
                    binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                        lifecycleScope.launch {
                            ThemePreferences.setDarkMode(requireContext(), isChecked)
                            AppCompatDelegate.setDefaultNightMode(
                                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }
                    }
                }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        requireActivity().finish()
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}