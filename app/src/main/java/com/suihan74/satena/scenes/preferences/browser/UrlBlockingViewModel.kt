package com.suihan74.satena.scenes.preferences.browser

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.browser.BlockUrlSetting
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.UrlBlockingDialog
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss

class UrlBlockingViewModel(
    val repository : BrowserRepository
) : ViewModel() {

    val blockUrls by lazy {
        repository.blockUrls
    }

    // ------ //

    /** 項目の操作メニュー */
    fun openItemMenuDialog(model: BlockUrlSetting, fragmentManager: FragmentManager) {
        val labels = listOf(
            R.string.dialog_delete
        )

        val dialog = AlertDialogFragment.Builder()
            .setTitle(model.pattern)
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(labels) { _, _ ->
                blockUrls.value = blockUrls.value?.minus(model)
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 項目の追加ダイアログ */
    fun openBlockUrlDialog(fragmentManager: FragmentManager) {
        val dialog = UrlBlockingDialog.createInstance()

        dialog.setOnCompleteListener { setting ->
            val blockList = blockUrls.value ?: emptyList()

            if (blockList.none { it.pattern == setting.pattern }) {
                blockUrls.value = blockList.plus(setting)
            }

            SatenaApplication.instance.showToast(R.string.msg_add_url_blocking_succeeded)
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }
}
