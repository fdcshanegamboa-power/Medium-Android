package com.connect.medium.ui.main.fragments.editprofile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.databinding.FragmentEditProfileBinding
import com.connect.medium.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels {
        EditProfileViewModelFactory(requireActivity().application)
    }

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.ivProfileImage)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnChangePhoto.setOnClickListener {
            showPhotoOptions()
        }

        binding.ivProfileImage.setOnClickListener {
            showPhotoOptions()
        }

        binding.btnSave.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            if (displayName.isEmpty()) {
                binding.tilDisplayName.error = "Display name cannot be empty"
                return@setOnClickListener
            }
            binding.tilDisplayName.error = null
            viewModel.updateProfile(displayName, bio, selectedImageUri)
        }
    }

    private fun showPhotoOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Profile Photo")
            .setItems(arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")) { _, which ->
                when (which) {
                    0 -> checkCameraAndLaunch()
                    1 -> pickImageLauncher.launch(arrayOf("image/*"))
                    2 -> removeProfilePhoto()
                }
            }
            .show()
    }

    private fun checkCameraAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "profile_${System.currentTimeMillis()}",
            ".jpg",
            requireContext().externalCacheDir
        )
        selectedImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(selectedImageUri!!)
    }

    private fun removeProfilePhoto() {
        selectedImageUri = null
        binding.ivProfileImage.setImageResource(R.drawable.ic_profile)
        // save with empty profileImageUrl
        val displayName = binding.etDisplayName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        viewModel.updateProfile(displayName, bio, null)
    }

    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val user = resource.data
                binding.etDisplayName.setText(user.displayName)
                binding.etBio.setText(user.bio)

                Glide.with(this)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            }
        }

        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressUpload.visibility = View.VISIBLE
            binding.progressUpload.progress = progress
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSave.isEnabled = false
                    binding.btnChangePhoto.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnSave.isEnabled = true
                    binding.btnChangePhoto.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.btnChangePhoto.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}