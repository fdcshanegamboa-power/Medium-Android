package com.connect.medium.ui.main.fragments.create

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connect.medium.R
import com.connect.medium.databinding.FragmentCreatePostBinding
import com.connect.medium.ui.main.adapters.MediaPreviewAdapter
import com.connect.medium.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.FlowPreview
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreatePostFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels {
        CreatePostViewModelFactory(requireActivity().application)
    }
    private val selectedMedia = mutableListOf<Pair<Uri, String>>()
    private lateinit var mediaPreviewAdapter: MediaPreviewAdapter
    private var isToolbarExpanded = false

    private var cameraImageUri: Uri? = null
    private var cameraVideoUri: Uri? = null

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            val type = requireContext().contentResolver.getType(uri) ?: ""
            val mediaType = if (type.startsWith("video")) "video" else "image"

            if (mediaType == "video") {
                val fileSize = requireContext().contentResolver
                    .openFileDescriptor(uri, "r")?.statSize ?: 0
                if (fileSize > 50 * 1024 * 1024L) {
                    Toast.makeText(
                        requireContext(),
                        "Video exceeds 50MB limit: ${fileSize / (1024 * 1024)}MB",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@forEach
                }
            }
            selectedMedia.add(Pair(uri, mediaType))
        }
        mediaPreviewAdapter.submitList(selectedMedia.toList())
        updateMediaCount()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                selectedMedia.add(Pair(uri, "image"))
                mediaPreviewAdapter.submitList(selectedMedia.toList())
                updateMediaCount()
            }
        }
    }

    private val takeVideoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->

        if(!success) return@registerForActivityResult
        cameraVideoUri?.let { uri ->
            val fileSize = requireContext().contentResolver
                .openFileDescriptor(uri, "r")?.statSize ?: 0
            if (fileSize > 50 * 1024 * 1024L) {
                Toast.makeText(
                    requireContext(),
                    "Video exceeds 50MB limit: ${fileSize / (1024 * 1024)}MB",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }
            selectedMedia.add(Pair(uri, "video"))
            mediaPreviewAdapter.submitList(selectedMedia.toList())
            updateMediaCount()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (cameraGranted) {
            showCameraOptions()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMediaPreview()
        setupClickListeners()
        observeViewModel()


    }

    private fun setupMediaPreview() {
        mediaPreviewAdapter = MediaPreviewAdapter { position ->
            // remove item on X click
            selectedMedia.removeAt(position)
            mediaPreviewAdapter.submitList(selectedMedia.toList())
            updateMediaCount()
        }

        binding.rvMediaPreview.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = mediaPreviewAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnClose.drawable?.setColorFilter(
            ContextCompat.getColor(binding.root.context, R.color.foreground),
            PorterDuff.Mode.SRC_IN
        )

        binding.btnPickMedia.setOnClickListener {
            pickMediaLauncher.launch(arrayOf("image/*", "video/*"))
        }
        binding.btnPickMedia.setColorFilter(
            ContextCompat.getColor(binding.root.context, R.color.foreground),
            PorterDuff.Mode.SRC_IN
        )

        binding.btnCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
        binding.btnCamera.setColorFilter(
            ContextCompat.getColor(binding.root.context, R.color.foreground),
            PorterDuff.Mode.SRC_IN
        )

        binding.btnExpand.setOnClickListener {
            toggleToolbar()
        }
        binding.btnExpand.setColorFilter(
            ContextCompat.getColor(binding.root.context, R.color.foreground),
            PorterDuff.Mode.SRC_IN
        )

        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString().trim()
            if (selectedMedia.isEmpty()) {
                Toast.makeText(requireContext(), "Please select media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.createPost(selectedMedia.toList(), caption)
        }
    }
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                showCameraOptions()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera Permission")
                    .setMessage("Camera access is needed to take photos and videos for your post.")
                    .setPositiveButton("Grant") { _, _ ->
                        cameraPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                cameraPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        }
    }

    private fun showCameraOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Camera")
            .setItems(arrayOf("Take Photo", "Record Video")) { _, which ->
                when (which) {
                    0 -> launchCameraPhoto()
                    1 -> launchCameraVideo()
                }
            }
            .show()
    }

    private fun launchCameraPhoto() {
        val photoFile = File.createTempFile(
            "photo_${System.currentTimeMillis()}",
            ".jpg",
            requireContext().externalCacheDir
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(cameraImageUri!!)
    }

    private fun launchCameraVideo() {
        val videoFile = File.createTempFile(
            "video_${System.currentTimeMillis()}",
            ".mp4",
            requireContext().externalCacheDir
        )
        cameraVideoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            videoFile
        )
        takeVideoLauncher.launch(cameraVideoUri!!)
    }

    private fun toggleToolbar() {
        isToolbarExpanded = !isToolbarExpanded
        binding.expandedOptions.visibility =
            if (isToolbarExpanded) View.VISIBLE else View.GONE
        binding.btnExpand.setImageResource(
            if (isToolbarExpanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less
        )
    }

    private fun updateMediaCount() {
        binding.tvMediaCount.text = "${selectedMedia.size} item(s) selected"
        binding.tvMediaCount.visibility =
            if (selectedMedia.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun observeViewModel() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressUpload.visibility = View.VISIBLE
            binding.progressUpload.progress = progress
        }

        viewModel.uploadStatus.observe(viewLifecycleOwner) { status ->
            binding.tvProgress.visibility = View.VISIBLE
            binding.tvProgress.text = status
        }

        viewModel.createPostState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnPost.isEnabled = false
                    binding.btnPickMedia.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnPost.isEnabled = true
                    binding.btnPickMedia.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Post shared!", Toast.LENGTH_SHORT).show()
                    clearForm()
                }
                is Resource.Error -> {
                    binding.btnPost.isEnabled = true
                    binding.btnPickMedia.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun clearForm() {
        selectedMedia.clear()
        mediaPreviewAdapter.submitList(emptyList())
        binding.etCaption.text?.clear()
        binding.tvMediaCount.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}