package com.suihan74.satena.fragments

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.FragmentContainerActivity
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HatenaAuthenticationFragment : CoroutineScopeFragment() {
    companion object {
        fun createInstance() = HatenaAuthenticationFragment().apply {
            enterTransition = TransitionSet().addTransition(Slide(Gravity.END))
            returnTransition = TransitionSet().addTransition(AutoTransition())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hatena_authentication, container, false)

        // ログイン
        val loginButton = view.findViewById<Button>(R.id.auth_button)
        loginButton.setOnClickListener {
            launch(Dispatchers.Main) {
                try {
                    val name = view.findViewById<EditText>(R.id.user_name).text.toString()
                    val password = view.findViewById<EditText>(R.id.password).text.toString()

                    val account = HatenaClient.signInAsync(name, password).await()

                    activity!!.showToast("id:${account.name} でログインしました")

                    AccountLoader.saveHatenaAccount(activity!!, name, password)
                    SatenaApplication.instance.startNotificationService()
                    backToMain()
                }
                catch (e: Exception) {
                    Log.d("FailedToSignIn", e.message)
                    activity!!.showToast("ログイン失敗")
                }
            }
        }

        // キャンセル
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            backToMain()
        }

        retainInstance = true
        return view
    }

    private fun backToMain() {
        val activity = activity as FragmentContainerActivity
        val fragmentManager = activity.supportFragmentManager

        val prefFragment = activity.getStackedFragment<PreferencesFragment>()

        if (prefFragment != null) {
            fragmentManager.popBackStack()
        }
        else {
            activity.showFragment(EntriesFragment.createInstance())
        }
    }
}
