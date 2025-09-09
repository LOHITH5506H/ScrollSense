package com.lohith.scrollsense.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lohith.scrollsense.adapters.LogAdapter
import com.lohith.scrollsense.databinding.FragmentLogBinding
import com.lohith.scrollsense.viewmodels.UsageLogViewModel // Changed to correct ViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    // Changed to correct ViewModel
    private val viewModel: UsageLogViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val logAdapter = LogAdapter()

        binding.recyclerViewLogs.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.allUsageSegments.observe(viewLifecycleOwner) { logs ->
            logAdapter.submitList(logs)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}