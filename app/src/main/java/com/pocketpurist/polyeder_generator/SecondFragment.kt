package com.pocketpurist.polyeder_generator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.pocketpurist.polyeder_generator.databinding.FragmentSecondBinding

/** A simple [Fragment] subclass as the second destination in the navigation.
 *  TODO rename this file into "CanvasFragment".
 */
class SecondFragment : Fragment() {
    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        // Ensure that dialog layouts are hidden "on startup":
        binding.relLayoutPointInput.visibility = View.GONE
        binding.relLayoutAxisAnglesInput.visibility = View.GONE
        binding.relLayoutEdgeInputInstruction.visibility = View.GONE
        binding.linearLayoutEdgeInput.visibility = View.GONE
        // Time-delayed hiding of the "soft key" back button in the action bar:
        Handler(Looper.getMainLooper()).postDelayed({ hideActionBarBackButton() },75)  // TODO consider cleaner solution...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}