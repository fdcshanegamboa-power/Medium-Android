package com.connect.medium.ui.main.fragments.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.connect.medium.R
import com.connect.medium.databinding.FragmentCommentsBinding
import com.connect.medium.ui.main.adapters.CommentAdapter
import com.connect.medium.utils.Resource

class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by viewModels {
        CommentsViewModelFactory(requireActivity().application)
    }

    private val args: CommentsFragmentArgs by navArgs()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        viewModel.loadComments(args.postId)
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            onUsernameClick = { uid ->
                if (uid == viewModel.currentUid) {
                    findNavController().popBackStack()
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottom_nav
                    ).selectedItemId = R.id.profileFragment
                } else {
                    val action = CommentsFragmentDirections.actionCommentsToUserProfile(uid)
                    findNavController().navigate(action)
                }
            },
            onUserProfileClick = { comment ->
                if (comment.authorUid == viewModel.currentUid) {
                    findNavController().popBackStack()
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottom_nav
                    ).selectedItemId = R.id.profileFragment
                } else {
                    val action = CommentsFragmentDirections.actionCommentsToUserProfile(comment.authorUid)
                    findNavController().navigate(action)
                }
            }
        )
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.addComment(args.postId, args.postAuthorUid, text)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                if (resource.data.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvComments.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvComments.visibility = View.VISIBLE
                    commentAdapter.submitList(resource.data)
                    binding.rvComments.scrollToPosition(resource.data.size - 1)
                }
            }
        }

        viewModel.addCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.btnSend.isEnabled = false
                is Resource.Success -> {
                    binding.btnSend.isEnabled = true
                    binding.etComment.text?.clear()
                }
                is Resource.Error -> {
                    binding.btnSend.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}