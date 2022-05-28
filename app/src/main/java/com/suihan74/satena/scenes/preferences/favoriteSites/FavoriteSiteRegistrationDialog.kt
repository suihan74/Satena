package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogFavoriteSiteRegistrationBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.favoriteSite.FavoriteSite
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.EmptyException
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteSiteRegistrationDialog : DialogFragment() {
    /** 処理の実行モード */
    enum class Mode (val titleId: Int) {
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

    // ------ //

    private val viewModel by lazyProvideViewModel {
        val args = requireArguments()
        val mode = args.getEnum<Mode>(ARG_MODE)!!
        val site = args.getObject<FavoriteSite>(ARG_TARGET_SITE)
        val repo = SatenaApplication.instance.favoriteSitesRepository

        DialogViewModel(mode, site, repo)
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val binding = DialogFavoriteSiteRegistrationBinding.inflate(inflater, null, false).also {
            it.vm = viewModel
            it.lifecycleOwner = this
        }

        return createBuilder()
            .setTitle(viewModel.mode.titleId)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_register, null)
            .setView(binding.root)
            .show()
            .also { dialog ->
                val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)

                viewModel.waiting.observe(this) {
                    positiveButton?.isEnabled = !it
                    negativeButton?.isEnabled = !it
                }

                dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                    viewModel.waiting.value = true
                    lifecycleScope.launch {
                        val result = runCatching {
                            viewModel.invokePositiveAction()
                        }

                        onCompleted(result.exceptionOrNull())
                    }
                }
            }
    }

    private suspend fun onCompleted(e: Throwable?) = withContext(Dispatchers.Main) {
        val c = SatenaApplication.instance
        when (e) {
            null -> {
                c.showToast(R.string.msg_favorite_site_registration_succeeded)
                dismissAllowingStateLoss()
            }

            is AlreadyExistedException -> {
                c.showToast(R.string.msg_favorite_site_already_existed)
                viewModel.waiting.value = false
            }

            is EmptyException -> {
                c.showToast(R.string.msg_favorite_site_invalid_title)
                viewModel.waiting.value = false
            }

            is InvalidUrlException -> {
                c.showToast(R.string.msg_favorite_site_invalid_url)
                viewModel.waiting.value = false
            }

            else -> {
                c.showToast(R.string.msg_favorite_site_registration_failed)
                Log.w("favoriteSite", Log.getStackTraceString(e))
                viewModel.waiting.value = false
            }
        }
    }

    // ------ //

    class DialogViewModel(
        val mode : Mode,
        private val targetSite : FavoriteSite?,
        private val repo : FavoriteSitesRepository
    ) : ViewModel() {

        val title = MutableLiveData(targetSite?.title ?: "")

        val faviconUrl = MutableLiveData(targetSite?.faviconUrl ?: "")

        val url = MutableLiveData(targetSite?.url.orEmpty()).also {
            it.observeForever { u ->
                faviconUrl.value = Uri.parse(u).faviconUrl
            }
        }

        val waiting = MutableLiveData(false)

        // ------ //

        /**
         * 登録処理を実行する
         *
         * @throws NullPointerException 入力内容が取得できない
         * @throws AlreadyExistedException 既に登録されているURL
         * @throws InvalidUrlException URLが不正・入力されていない
         * @throws EmptyException タイトルが入力されていない
         * @throws Throwable onRegister, onModify内で起きたエラー
         */
        suspend fun invokePositiveAction() = withContext(Dispatchers.Default) {
            repo.favoritePage(
                url = url.value!!,
                title = title.value!!,
                faviconUrl = faviconUrl.value!!,
                isEnabled = targetSite?.isEnabled ?: false,
                modify = (mode == Mode.MODIFY),
                id = targetSite?.id ?: 0L
            )
        }
    }
}
