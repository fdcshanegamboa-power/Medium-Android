package com.connect.medium.ui.main.fragments.comments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connect.medium.R
import com.connect.medium.databinding.FragmentCommentsBottomSheetBinding
import com.connect.medium.ui.main.adapters.CommentAdapter
import com.connect.medium.ui.main.fragments.home.HomeFragmentDirections
import com.connect.medium.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


/**
 * A simple [Fragment] subclass.
 * Use the [CommentsBottomSheet.newInstance] factory method to
 * create an instance of this fragment.
 */
class CommentsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by viewModels {
        CommentsViewModelFactory(requireActivity().application)
    }

    private lateinit var commentAdapter: CommentAdapter
    private lateinit var postId: String
    private lateinit var postAuthorUid: String

    companion object {
        const val TAG = "CommentsBottomSheet"
        private const val ARG_POST_ID = "post_id"
        private const val ARG_POST_AUTHOR_UID = "post_author_uid"

        fun newInstance(postId: String, postAuthorUid: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_POST_AUTHOR_UID, postAuthorUid)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString(ARG_POST_ID) ?: return
        postAuthorUid = arguments?.getString(ARG_POST_AUTHOR_UID) ?: return

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        viewModel.loadComments(postId)
    }

    // make bottom sheet taller — 90% of screen height
    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val screenHeight = resources.displayMetrics.heightPixels
                sheet.layoutParams.height = (screenHeight * 0.9).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            onUsernameClick = { uid ->
                dismiss()
                if (uid == viewModel.currentUid) {
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottom_nav
                    ).selectedItemId = R.id.profileFragment
                } else {
                    val action = HomeFragmentDirections.actionHomeToProfile(uid)
                    requireActivity().supportFragmentManager
                        .findFragmentById(R.id.main_nav_host)
                        ?.findNavController()
                        ?.navigate(action)
                }
            },
            onUserProfileClick = { comment ->
                if (comment.authorUid == viewModel.currentUid) {
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottom_nav
                    ).selectedItemId = R.id.profileFragment
                } else {
                    val action = HomeFragmentDirections.actionHomeToProfile(comment.authorUid)
                    requireActivity().supportFragmentManager
                        .findFragmentById(R.id.main_nav_host)
                        ?.findNavController()
                        ?.navigate(action)
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
                viewModel.addComment(postId, postAuthorUid, text)
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