package com.connect.medium.ui.main.fragments.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.User
import com.connect.medium.databinding.FragmentProfileBinding
import com.connect.medium.ui.main.adapters.PostGridAdapter
import com.connect.medium.utils.Resource

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels{
        ProfileViewModelFactory(requireActivity().application)
    }

    private val args: ProfileFragmentArgs by navArgs()
    private lateinit var targetUid: String
    private lateinit var postGridAdapter: PostGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUid = if(args.uid.isNullOrEmpty()) viewModel.currentUid else args.uid!!

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        viewModel.loadUser(targetUid)
        viewModel.loadUserPosts(targetUid)

        if (targetUid != viewModel.currentUid) {
            viewModel.observeIsFollowing(targetUid)
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
            binding.toolbar.setNavigationOnClickListener{
                findNavController().popBackStack()
            }
        } else {
            binding.toolbar.inflateMenu(R.menu.menu_profile)
            binding.toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_profile_to_settings)
                        true
                    }
                    else -> false
                }
            }
        }

        binding.btnCreateFirstPost.setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }

    }

    private fun setupRecyclerView() {
        postGridAdapter = PostGridAdapter { post ->
            // TODO: navigate to post details
        }
        binding.rvPosts.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = postGridAdapter
        }
    }
    private fun setupClickListeners() {
        binding.btnFollow.setOnClickListener {
            if (viewModel.isFollowing.value == true) {
                viewModel.unfollowUser(targetUid)
            } else {
                viewModel.followUser(targetUid)
            }
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        }
    }

    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> bindUser(resource.data)
                is Resource.Error -> Toast.makeText(
                    requireContext(), resource.message, Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.postsState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                postGridAdapter.submitList(resource.data)
                binding.tvPostCount.text = resource.data.size.toString()

                if (resource.data.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvPosts.visibility = View.GONE

                    // show create button only on own profile
                    if (targetUid == viewModel.currentUid) {
                        binding.btnCreateFirstPost.visibility = View.VISIBLE
                        binding.tvEmptySubtitle.text = "Share your first photo or video"
                    } else {
                        binding.btnCreateFirstPost.visibility = View.GONE
                        binding.tvEmptySubtitle.text = "No posts yet"
                    }
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvPosts.visibility = View.VISIBLE
                }
            }
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            binding.btnFollow.text = if (isFollowing) "Unfollow" else "Follow"
        }
    }

    private fun bindUser(user: User) {
        binding.tvUsername.text = "@${user.username}"
        binding.tvDisplayName.text = user.displayName
        binding.tvBio.text = user.bio
        binding.tvFollowerCount.text = user.followerCount.toString()
        binding.tvFollowingCount.text = user.followingCount.toString()

        if (user.bio.isNullOrEmpty()) {
            binding.tvBio.visibility = View.GONE
        } else {
            binding.tvBio.visibility = View.VISIBLE
        }

        Glide.with(this)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_profile)
            .into(binding.ivProfileImage)

        if (targetUid == viewModel.currentUid) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnFollow.visibility = View.GONE
        } else {
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnEditProfile.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}