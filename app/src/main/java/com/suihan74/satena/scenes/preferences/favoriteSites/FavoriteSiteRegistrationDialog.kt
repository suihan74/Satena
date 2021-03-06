package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.DialogFavoriteSiteRegistrationBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.utilities.Listener
import com.suihan74.utilities.OnSuccess
import com.suihan74.utilities.Switcher
import com.suihan74.utilities.exceptions.DuplicateException
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

        DialogViewModel(mode, site)
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
                    lifecycleScope.launch(Dispatchers.Default) {
                        val result = runCatching {
                            viewModel.invokePositiveAction()
                        }

                        onCompleted(result.getOrNull(), result.exceptionOrNull())
                    }
                }
            }
    }

    private suspend fun onCompleted(
        site: FavoriteSite?,
        e: Throwable?
    ) = withContext(Dispatchers.Main) {
        val c = SatenaApplication.instance
        when (e) {
            null -> {
                c.showToast(R.string.msg_favorite_site_registration_succeeded)
                viewModel.onSuccess?.invoke(site!!)
                dismissAllowingStateLoss()
            }

            is DuplicateException -> {
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

    /** 登録時に呼ばれるリスナをセットする */
    fun setOnRegisterListener(listener: Listener<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.onRegister = listener
    }

    /** 編集完了時に呼ばれるリスナをセットする */
    fun setOnModifyListener(listener: Listener<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.onModify = listener
    }

    /**
     *  重複確認処理をセットする
     *
     *  TODO: 外部から渡す必要をなくす
     *
     *  @return true:既に登録されている false:登録可能
     */
    fun setDuplicationChecker(switcher: Switcher<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.duplicationChecker = switcher
    }

    /** 登録成功後の処理をセットする */
    fun setOnSuccessListener(listener: OnSuccess<FavoriteSite>?) = lifecycleScope.launchWhenCreated {
        viewModel.onSuccess = listener
    }

    // ------ //

    class DialogViewModel(
        val mode: Mode,
        private val targetSite: FavoriteSite?
    ) : ViewModel() {

        val url by lazy {
            MutableLiveData(targetSite?.url ?: "").also {
                it.observeForever { u ->
                    faviconUrl.value = Uri.parse(u).faviconUrl
                }
            }
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

        /**
         * 登録処理を実行する
         *
         * @throws InvalidUrlException URLが不正・入力されていない
         * @throws EmptyException タイトルが入力されていない
         * @throws DuplicateException 他の項目と重複している
         * @throws NullPointerException 重複確認処理が登録されていない場合
         * @throws Throwable onRegister, onModify内で起きたエラー
         */
        suspend fun invokePositiveAction() : FavoriteSite {
            val site = FavoriteSite(
                url = url.value!!,
                title = title.value!!,
                faviconUrl = faviconUrl.value!!,
                isEnabled = targetSite?.isEnabled ?: false
            )

            if (!URLUtil.isValidUrl(site.url)
                || !URLUtil.isHttpsUrl(site.url)
                || !URLUtil.isHttpsUrl(site.url)
                || Uri.parse(site.url).host.isNullOrBlank()
            ) {
                throw InvalidUrlException(site.url)
            }

            if (site.title.isBlank()) {
                throw EmptyException()
            }

            withContext(Dispatchers.Main) {
                val duplicated = duplicationChecker!!.invoke(site)
                if (duplicated) {
                    throw DuplicateException()
                }

                when (mode) {
                    Mode.ADD -> onRegister?.invoke(site)
                    Mode.MODIFY -> onModify?.invoke(site)
                }
            }

            return site
        }

        // ------ //

        var onRegister : Listener<FavoriteSite>? = null

        var onModify : Listener<FavoriteSite>? = null

        var duplicationChecker : Switcher<FavoriteSite>? = null

        var onSuccess : OnSuccess<FavoriteSite>? = null
    }
}
