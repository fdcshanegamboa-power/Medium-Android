package com.connect.medium.ui.main.fragments.create

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.connect.medium.R
import com.connect.medium.databinding.FragmentCreatePostBinding
import com.connect.medium.utils.Resource

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
    private var selectedImageUri: Uri? = null

    private var pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){ uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivPostImage.setImageURI(it)
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
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners(){
        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString().trim()
            val imageUri = selectedImageUri

            if (imageUri == null) {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createPost(imageUri, caption)
        }
    }

    private fun observeViewModel(){
        viewModel.uploadProgress.observe(viewLifecycleOwner){ progress ->
            binding.progressUpload.visibility = View.VISIBLE
            binding.tvProgress.visibility = View.VISIBLE
            binding.progressUpload.progress = progress
            binding.tvProgress.text = "Uploading... $progress%"
        }
        viewModel.createPostState.observe(viewLifecycleOwner){ resource->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnPost.isEnabled = false
                    binding.btnPickImage.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnPost.isEnabled = true
                    binding.btnPickImage.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    clearForm()
                }
                is Resource.Error -> {
                    binding.btnPost.isEnabled = true
                    binding.btnPickImage.isEnabled = true
                    binding.progressUpload.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun clearForm() {
        selectedImageUri = null
        binding.ivPostImage.setImageDrawable(null)
        binding.etCaption.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}