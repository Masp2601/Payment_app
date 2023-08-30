package com.softmed.payment.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.softmed.payment.R
import org.json.JSONObject

class InternetHelper private constructor(private val ctx: Context) {
    companion object {
        val URL_BASE = "http://licenses.softmedsas.com"
        val LICENSE_EXPIRED_URL = "api/licensee/hasExpired"
        private var instance: InternetHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): InternetHelper {
            if (instance == null) {
                instance = InternetHelper(ctx)
            }

            return instance!!
        }
    }

    private val queue: RequestQueue = Volley.newRequestQueue(ctx)

    fun deviceHasInternetConnection(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    fun executeRequest(request: JsonObjectRequest) {
        request.retryPolicy = DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }

    fun checkIfLicenseIsValid(function: (isValid: Boolean) -> Unit, callbackError: Response.ErrorListener) {
        val preference = PreferenceManager.getDefaultSharedPreferences(ctx)
        val username = preference.getString(ctx.getString(R.string.pref_value_email_registered), "")
        val request = JsonObjectRequest(
                Request.Method.GET,
                "$URL_BASE/$LICENSE_EXPIRED_URL?username=$username",
                null,
                Response.Listener { response: JSONObject ->
                    val success = response.getBoolean("Success")
                    if (success) {
                        val data = response.getJSONObject("Data")
                        val hasExpired = data.getBoolean("HasExpired")
                        function(hasExpired)
                    } else {
                        val message = response.getString("Message")
                        callbackError.onErrorResponse(VolleyError(message))
                    }
                },
                callbackError
        )

        executeRequest(request)
    }

}