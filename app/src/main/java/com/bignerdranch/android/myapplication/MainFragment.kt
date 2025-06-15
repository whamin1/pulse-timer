package com.bignerdranch.android.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class MainFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        with (view){
            findViewById<Button>(R.id.btn_tabata).setOnClickListener {
                navigateToSettings(TimerMode.TABATA)
            }

            findViewById<Button>(R.id.btn_emom).setOnClickListener {
                navigateToSettings(TimerMode.EMOM)
            }

            findViewById<Button>(R.id.btn_amrap).setOnClickListener {
                navigateToSettings(TimerMode.AMRAP)
            }

            findViewById<Button>(R.id.btn_for_time).setOnClickListener {
                navigateToSettings(TimerMode.FOR_TIME)
            }

            findViewById<Button>(R.id.btn_pulse).setOnClickListener {
                navigateToSettings(TimerMode.PULSE)
            }

            findViewById<Button>(R.id.btn_view_record).setOnClickListener {
                startActivity(Intent(requireContext(), RecordActivity::class.java))
            }
        }
        return view
    }

    private fun navigateToSettings(mode: TimerMode) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, SettingsFragment.newInstance(mode))
            .addToBackStack(null)
            .commit()
    }
}