package com.udaychugh.kalam.ui.Base


import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import com.google.firebase.messaging.FirebaseMessaging
import com.mikepenz.ionicons_typeface_library.Ionicons
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject


open class BaseFragment : Fragment() {
    lateinit var progressDialog: ProgressDialog
    val sessionManager: SessionManager by inject()

    // Toast a message
    fun toast(message: String) {
        activity?.toast(message)
    }

    // Toast a message
    fun longToast(message: String) {
        activity?.longToast(message)
    }

    // Get user ID
    fun getUid(): String = sessionManager.getUserId()

    // Show a progress dialog
    fun showLoading(message: String) {
        progressDialog = ProgressDialog(activity)
        progressDialog.setCancelable(false)
        progressDialog.setMessage(message)
        progressDialog.show()
    }

    // Dismiss progress dialog
    fun hideLoading() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    /**
     * Proceed to MainActivity after successful login / registration
     */
    fun proceedToMainActivity(user: User, button: CircularProgressButton) {
        // Set progress icon status
        val successIcon = AppUtils.getDrawable(activity!!, Ionicons.Icon.ion_checkmark_round, R.color.white, 25)
        val successfulButtonIcon: Bitmap = AppUtils.drawableToBitmap(successIcon)
        button.doneLoadingAnimation(AppUtils.getColor(activity!!, R.color.color_favorite), successfulButtonIcon)

        hideLoading()
        sessionManager.saveUser(user)

        // Subscribe to Firebase
        subscribe(Constants.TOPIC_GLOBAL)
        if (user.hasAdminStatus) subscribe(Constants.TOPIC_ADMIN)

        runDelayed(400) {
            longToast("Welcome ${user.userName} \uD83D\uDE03")

            startActivity(Intent(activity!!, MainActivity::class.java))
            AppUtils.slideRight(activity!!)
            activity!!.finish()
        }
    }

    /**
     * Subscribe to Firebase topic
     */
    private fun subscribe(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
    }

}
