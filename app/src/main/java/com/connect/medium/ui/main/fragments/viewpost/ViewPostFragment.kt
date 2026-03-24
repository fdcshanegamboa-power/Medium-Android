package com.connect.medium.ui.main.fragments.viewpost

import android.graphics.Typeface
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Post
import com.connect.medium.databinding.FragmentViewPostBinding
import com.connect.medium.ui.main.adapters.CommentAdapter
import com.connect.medium.ui.main.adapters.PostMediaAdapter
import com.connect.medium.ui.main.fragments.comments.CommentsFragmentDirections
import com.connect.medium.utils.Resource

@UnstableApi
class ViewPostFragment : Fragment() {

    private var _binding: FragmentViewPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ViewPostViewModel by viewModels {
        ViewPostViewModelFactory(requireActivity().application)
    }

    private val args: ViewPostFragmentArgs by navArgs()
    private lateinit var commentAdapter: CommentAdapter
    private var currentPost: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupCommentList()
        observeViewModel()

        viewModel.loadPost(args.postId)
        viewModel.loadComments(args.postId)

        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            val post = currentPost ?: return@setOnClickListener
            if (text.isNotEmpty()) {
                viewModel.addComment(post.postId, post.authorUid, text)
            }
        }
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.foreground)
        )
        binding.postContent.tvCommentCount.visibility = View.GONE
        binding.postContent.tvUsername.setOnClickListener {
            currentPost?.let { post ->
                val action = ViewPostFragmentDirections.actionViewPostToProfile(post.authorUid)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupCommentList() {
        commentAdapter = CommentAdapter(
            onUsernameClick = { uid ->
                if (uid == viewModel.currentUid) {
                    findNavController().popBackStack()
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottom_nav
                    ).selectedItemId = R.id.profileFragment
                } else {
                    val action = ViewPostFragmentDirections.actionViewPostToProfile(uid)
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
                    val action = ViewPostFragmentDirections.actionViewPostToProfile(comment.authorUid)
                    findNavController().navigate(action)
                }
            }
        )
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.postState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    currentPost = resource.data
                    bindPost(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                if (resource.data.isEmpty()) {
                    binding.tvNoComments.visibility = View.VISIBLE
                    binding.rvComments.visibility = View.GONE
                } else {
                    binding.tvNoComments.visibility = View.GONE
                    binding.rvComments.visibility = View.VISIBLE
                    commentAdapter.submitList(resource.data)
                }
            }
        }

        viewModel.isLiked.observe(viewLifecycleOwner) { isLiked ->
            binding.postContent.btnLike.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
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

    private fun bindPost(post: Post) {
        val postBinding = binding.postContent

        postBinding.tvUsername.text = post.authorUsername
        postBinding.tvTimestamp.text = getRelativeTime(post.createdAt)

        val captionText = SpannableStringBuilder()
        val boldSpan = StyleSpan(Typeface.BOLD)
        captionText.append(post.authorUsername)
        captionText.setSpan(boldSpan, 0, post.authorUsername.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (post.caption.isNotEmpty()) captionText.append("  ${post.caption}")
        postBinding.tvCaption.text = captionText

        postBinding.tvLikeCount.text = when {
            post.likeCount == 0 -> "Be the first to like this"
            post.likeCount == 1 -> "1 like"
            else -> "${post.likeCount} likes"
        }

        postBinding.tvCommentCount.text = when {
            post.commentCount == 0 -> ""
            post.commentCount == 1 -> "View 1 comment"
            else -> "View all ${post.commentCount} comments"
        }

        if (post.authorProfileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(post.authorProfileImageUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(postBinding.ivProfileImage)
        } else {
            postBinding.ivProfileImage.setImageResource(R.drawable.ic_profile)
        }

        val mediaAdapter = PostMediaAdapter(post.mediaUrls, post.mediaTypes)
        postBinding.viewPagerMedia.adapter = mediaAdapter

        if (post.mediaUrls.size > 1) {
            postBinding.dotsIndicator.visibility = View.VISIBLE
            postBinding.dotsIndicator.attachTo(postBinding.viewPagerMedia)
        } else {
            postBinding.dotsIndicator.visibility = View.GONE
        }

        postBinding.btnLike.setOnClickListener { viewModel.toggleLike(post) }
        postBinding.btnComment.setOnClickListener {
            binding.etComment.requestFocus()
        }
        postBinding.ivProfileImage.setOnClickListener {
            val action = ViewPostFragmentDirections.actionViewPostToProfile(post.authorUid)
            findNavController().navigate(action)
        }
        postBinding.tvUsername.setOnClickListener {
            val action = ViewPostFragmentDirections.actionViewPostToProfile(post.authorUid)
            findNavController().navigate(action)
        }
        postBinding.btnMore.setOnClickListener {
            // TODO: post options
        }
    }

    private fun getRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}