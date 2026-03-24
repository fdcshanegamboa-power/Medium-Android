package com.connect.medium.ui.main.fragments.notifications

import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connect.medium.R
import com.connect.medium.data.model.NotificationType
import com.connect.medium.databinding.FragmentNotificationsBinding
import com.connect.medium.ui.main.adapters.NotificationAdapter
import com.connect.medium.utils.Resource
import com.google.android.material.bottomnavigation.BottomNavigationView

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels {
        NotificationsViewModelFactory(requireActivity().application)
    }

    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()

        val badge = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            .getBadge(R.id.notificationsFragment)
        badge?.isVisible = false

        viewModel.markAllAsRead()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            when (notification.type) {
                NotificationType.FOLLOW -> {
                    val action = NotificationsFragmentDirections
                        .actionNotificationsToProfile(notification.fromUid)
                    findNavController().navigate(action)
                }
                NotificationType.LIKE, NotificationType.COMMENT -> {
                    if (notification.postId.isNotEmpty()) {
                        val action = NotificationsFragmentDirections
                            .actionNotificationsToViewPost(notification.postId)
                        findNavController().navigate(action)
                    }
                }
            }
        }
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    private fun observeViewModel() {
        viewModel.notificationsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvNotifications.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvNotifications.visibility = View.VISIBLE
                        notificationAdapter.submitList(resource.data)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
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