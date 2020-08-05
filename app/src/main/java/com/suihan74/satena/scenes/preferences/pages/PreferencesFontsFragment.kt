package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesFontsBinding
import com.suihan74.satena.models.FontSettings
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_fonts.view.*
import java.io.File

class PreferencesFontsFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesFontsFragment()
    }

    val viewModel : PreferencesFontsViewModel by lazy {
        val factory = PreferencesFontsViewModel.Factory(
            SafeSharedPreferences.create(context)
        )
        ViewModelProvider(this, factory)[PreferencesFontsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesFontsBinding>(
            inflater,
            R.layout.fragment_preferences_fonts,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        val view = binding.root

        val fontExt = ".ttf"
        val fontsDir : File? = File("/system/fonts/")
        for (file in fontsDir?.listFiles() ?: emptyArray()) {
            if (file.name.endsWith(fontExt)) {
                val name = file.name.substring(0, file.name.length - fontExt.length)
                Log.i("font", name)
            }
        }

        // TODO: ちゃんとフォント変更用のパーツ作ったら消す
        view.testButton.setOnClickListener {
            val cur = viewModel.entryTitle.value ?: return@setOnClickListener

            viewModel.entryTitle.value = when {
                cur.fontFamily == "sans-serif" -> FontSettings(fontFamily = "serif", size = 16f, bold = true)
                else -> FontSettings(fontFamily = "sans-serif", size = 14f, bold = true)
            }
        }

        return view
    }
}

