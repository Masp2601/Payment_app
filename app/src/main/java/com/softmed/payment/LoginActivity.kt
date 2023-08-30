package com.softmed.payment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.iid.InstanceID
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.helpers.InternetHelper
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.mediaProjectionManager
import org.json.JSONObject
import java.util.*


class LoginActivity : BaseActivity(), AnkoLogger {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Login", null)

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener {
            if (checkForInternetConnection()) {
                attemptLogin()
            }
        }

        checkForPermissions()
        showPrivacyLicenseDialog()
    }

    private fun showPrivacyLicenseDialog() {
        val alert = AlertDialog.Builder(this)
        val view = this.layoutInflater.inflate(R.layout.layout_whats_new, null)

        alert.setPositiveButton("Ok"){
            d, _ ->
            d.dismiss()
        }

        val webView = view.findViewById<WebView>(R.id.whatsNewWV)
        webView.webChromeClient = object: WebChromeClient() {
            override fun onProgressChanged(webView: WebView?, newProgress: Int) {
                view.findViewById<ProgressBar>(R.id.progressBar).progress = newProgress

                if (newProgress == 100) {
                    view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(webView: WebView?, request: WebResourceRequest?, error: WebResourceError) {
                Toast.makeText(this@LoginActivity,
                        getString(R.string.error_no_internet_connection),
                        Toast.LENGTH_LONG).show()
            }
        }

        val language = getCurrentLanguage()
        webView.loadUrl("http://softmedsas.com/terminosycondiciones.$language.html")

        alert.setView(view)
        alert.show()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            registerUser(emailStr)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return !TextUtils.isEmpty(email) &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    private fun registerUser(username: String) {
        val queue = InternetHelper.getInstance(applicationContext)
        val url = "${BaseActivity.LICENSE_URL}/api/licensee/device/register/"
        val iid = InstanceID.getInstance(applicationContext).id
        val promotionCode = promotionCode.text
        val parameters = JSONObject()
        parameters.put("Username", username)
        parameters.put("DeviceUniqueIdentifier", iid)
        parameters.put("PromotionCode", promotionCode)
        val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                parameters,
                responseSuccess,
                responseError
        )
        queue.executeRequest(request)
    }

    private val responseSuccess = Response.Listener<JSONObject> {
        val success = it.getBoolean("Success")
        val responseCode = it.getInt("Code")
        if (success) {
            val data = it.getJSONObject("Data")
            val date = DateTimeHelper.parseStringToDate(data.getString("ExpirationDate"), DateTimeHelper.PATTERN_REQUEST_RESPONSE)
            val licenseType = data.getInt("LicenseTypeId")
            val today = Date()

            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = preference.edit()
            editor.putString(getString(R.string.pref_value_email_registered), email.text.toString())
            editor.putLong(getString(R.string.pref_value_license_expiration_date), date.time)
            editor.putInt(getString(R.string.pref_value_license_type_id), licenseType)
            editor.putBoolean(getString(R.string.pref_value_application_is_limited), date.after(today))
            editor.apply()

                if (responseCode == 200) {
                Toast.makeText(this, getString(R.string.license_promotion_code_success), Toast.LENGTH_LONG).show()
            }
            if (responseCode == 201) {
                Toast.makeText(this, getString(R.string.license_promotion_code_denied), Toast.LENGTH_LONG).show()
            }

            val intent = Intent(this@LoginActivity, BasicDataActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        } else {
            val code = it.getInt("Code")
            val message = if (code == 10) getString(R.string.error_incorrect_email) else it.getString("Message")
            email.error = message
            email.requestFocus()
            showProgress(false)
        }

        val bundle = Bundle()
        bundle.putString("login_response", it.toString())
        mFirebaseAnalytics.logEvent("login_attempt", bundle)
    }

    private val responseError = Response.ErrorListener { err: VolleyError? ->
        error(err)
        showProgress(false)

        val bundle = Bundle()
        bundle.putString("login_response", err?.message)
        mFirebaseAnalytics.logEvent("login_response_error", bundle)

        if (err is com.android.volley.TimeoutError) {
            Toast.makeText(this, getString(R.string.error_request_timeout), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, err?.message, Toast.LENGTH_LONG).show()
        }
        email.requestFocus()
    }

    private fun checkForInternetConnection(): Boolean {
        val hasInternetConnection = InternetHelper.getInstance(applicationContext).deviceHasInternetConnection()
        if (!hasInternetConnection) {
            Toast.makeText(this, getString(R.string.error_no_internet_connection), Toast.LENGTH_LONG).show()
        }

        return hasInternetConnection;
    }

    private fun checkForPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            val arrayOfPermission = arrayOf(Manifest.permission.ACCESS_NETWORK_STATE)
            ActivityCompat.requestPermissions(this, arrayOfPermission, PERMISSION_CODE)
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    companion object {
        private const val PERMISSION_CODE = 100
    }
}
