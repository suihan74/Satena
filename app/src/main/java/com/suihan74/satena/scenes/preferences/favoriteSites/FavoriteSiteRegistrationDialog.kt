package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogFavoriteSiteRegistrationBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.Listener
import com.suihan74.utilities.Switcher
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteSiteRegistrationDialog : DialogFragment() {
    /** 処理の実行モード */
    enum class Mode(
        val titleId: Int
    ) {
        /** 追加 */
        ADD(R.string.dialog_title_favorite_site_registration),
        /** 修正 */
        MODIFY(R.string.dialog_title_favorite_site_modification)
    }

    companion object {
        /** 編集 */
        fun createModificationInstance(
            site: FavoriteSite
        ) = FavoriteSiteRegistrationDialog().withArguments {
            putEnum(ARG_MODE, Mode.MODIFY)
            putObject(ARG_TARGET_SITE, site)
        }

        /** 追加 */
        fun createRegistrationInstance(
            site: FavoriteSite? = null
        ) = FavoriteSiteRegistrationDialog().withArguments {
            putEnum(ARG_MODE, Mode.ADD)
            putObject(ARG_TARGET_SITE, site)
        }

        private const val ARG_MODE = "ARG_MODE"
        private const val ARG_TARGET_SITE = "ARG_TARGET_SITE"
    }

    private val viewModel by lazy {
        val args = requireArguments()
        val mode = args.getEnum<Mode>(ARG_MODE)!!
        val site = args.getObject<FavoriteSite>(ARG_TARGET_SITE)

        provideViewModel(this) {
            DialogViewModel(mode, site)
        }
    }

    private var binding : DialogFavoriteSiteRegistrationBinding? = null
    private var dialog: AlertDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<DialogFavoriteSiteRegistrationBinding>(
            inflater,
            R.layout.dialog_favorite_site_registration,
            null,
            false
        ).also {
            it.vm = viewModel
        }
        this.binding = binding

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(viewModel.mode.titleId)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_register, null)
            .setView(binding.root)
            .show()
            .also { dialog ->
                this.dialog = dialog
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                    viewModel.waiting.value = true
                    lifecycleScope.launch(Dispatchers.Default) {
                        val result = viewModel.invokePositiveAction()
                        withContext(Dispatchers.Main) {
                            val c = SatenaApplication.instance
                            if (result) {
                                c.showToast(R.string.msg_favorite_site_registration_succeeded)
                                dismissAllowingStateLoss()
                            }
                            else {
                                c.showToast(R.string.msg_favorite_site_already_existed)
                                viewModel.waiting.value = false
                            }
                        }
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.lifecycleOwner = viewLifecycleOwner
        dialog?.let { dialog ->
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            viewModel.waiting.observe(viewLifecycleOwner) {
                positiveButton?.isEnabled = !it
                negativeButton?.isEnabled = !it
            }
        }
    }

    // ------ //

    /** 登録時に呼ばれるリスナをセットする */
    fun setOnRegisterListener(listener: Listener<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.onRegister = listener
    }

    /** 編集完了時に呼ばれるリスナをセットする */
    fun setOnModifyListener(listener: Listener<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.onModify = listener
    }

    /** 重複確認処理をセットする */
    fun setDuplicationChecker(switcher: Switcher<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.duplicationChecker = switcher
    }

    // ------ //

    class DialogViewModel(
        val mode: Mode,
        val targetSite: FavoriteSite?
    ) : ViewModel() {

        val url by lazy {
            MutableLiveData(targetSite?.url ?: "")
        }

        val title by lazy {
            MutableLiveData(targetSite?.title ?: "")
        }

        val faviconUrl by lazy {
            MutableLiveData(targetSite?.faviconUrl ?: "")
        }

        val waiting by lazy {
            MutableLiveData(false)
        }

        // ------ //

        suspend fun invokePositiveAction() : Boolean {
            val site = FavoriteSite(
                url = url.value!!,
                title = title.value!!,
                faviconUrl = faviconUrl.value!!,
                isEnabled = false
            )

            val result = duplicationChecker?.invoke(site) ?: false

            if (result) {
                withContext(Dispatchers.Main) {
                    when (mode) {
                        Mode.ADD -> onRegister?.invoke(site)
                        Mode.MODIFY -> onModify?.invoke(site)
                    }
                }
            }

            return result
        }

        // ------ //

        var onRegister : Listener<FavoriteSite>? = null

        var onModify : Listener<FavoriteSite>? = null

        var duplicationChecker : Switcher<FavoriteSite>? = null
    }
}
