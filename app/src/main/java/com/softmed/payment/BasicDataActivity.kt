package com.softmed.payment

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.InternetHelper
import kotlinx.android.synthetic.main.content_basic_data.*
import org.jetbrains.anko.AnkoLogger
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class BasicDataActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private val queue: InternetHelper by lazy { InternetHelper.getInstance(applicationContext) }

    private val url = "${BaseActivity.LICENSE_URL}/api/licensee/basic_data"
    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    private lateinit var countries: ArrayList<String>
    private lateinit var countryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_data)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        countries = getCountries()

        btnSave.setOnClickListener(onSave())

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefDisplayName.setText(preferences.getString(getString(R.string.pref_key_display_name), ""))
        prefNit.setText(preferences.getString(getString(R.string.pref_key_nit), ""))
        prefPhoneNumber.setText(preferences.getString(getString(R.string.pref_key_telephone_number), ""))
        prefEmail.setText(preferences.getString(getString(R.string.pref_key_email), ""))
        prefDirection.setText(preferences.getString(getString(R.string.pref_key_direction), ""))

        val currentCountry = Locale.getDefault().displayCountry
        val countrySaved = preferences.getString(getString(R.string.pref_key_country), currentCountry)

        countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        spinnerCountry.adapter = countryAdapter
        spinnerCountry.setSelection(countryAdapter.getPosition(countrySaved))

        getBasicData()
    }

    private fun onSave() = View.OnClickListener {
        var cancel = false

        val name = prefDisplayName.text.toString()
        val nit = prefNit.text.toString()
        val phoneNumber = prefPhoneNumber.text.toString()
        val email = prefEmail.text.toString()
        val direction = prefDirection.text.toString()
        val country = countries[spinnerCountry.selectedItemPosition]

        val fieldRequired = getString(R.string.error_field_required)
        if (TextUtils.isEmpty(name)) {
            prefDisplayName.error = fieldRequired
            cancel = true
        }
        /*
        if(TextUtils.isEmpty(nit)) {
            prefNit.error = fieldRequired
            cancel = true
        }
        */
        if(TextUtils.isEmpty(phoneNumber)) {
            prefPhoneNumber.error = fieldRequired
            cancel = true
        }
        if(TextUtils.isEmpty(email)) {
            prefEmail.error = fieldRequired
            cancel = true
        }
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            prefEmail.error = getString(R.string.error_invalid_email)
            cancel = true
        }
        if(TextUtils.isEmpty(direction)) {
            prefDirection.error = fieldRequired
            cancel = true
        }

        if (cancel) return@OnClickListener

        val manager = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val managerEditor = manager.edit()
        managerEditor.putString(getString(R.string.pref_key_display_name), name)
        managerEditor.putString(getString(R.string.pref_key_nit), nit)
        managerEditor.putString(getString(R.string.pref_key_telephone_number), phoneNumber)
        managerEditor.putString(getString(R.string.pref_key_email), email)
        managerEditor.putString(getString(R.string.pref_key_direction), direction)
        // managerEditor.putString(getString(R.string.pref_key_country), country)

        managerEditor.apply()

        val preference = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val editor = preference.edit()
        editor.putBoolean(getString(R.string.pref_value_is_application_registered), true)

        editor.apply()

        saveData()
    }

    private fun getBasicData() {
        val preferences = getPreferences()
        val mail = preferences.getString(getString(R.string.pref_value_email_registered), "")
        val request = JsonObjectRequest(
                Request.Method.GET,
                "$url?username=$mail",
                null,
                requestSuccess,
                requestError
        )
        queue.executeRequest(request)
    }

    private fun gotoMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun saveData() {
        progressBar.visibility = View.VISIBLE
        layoutBasicData.visibility = View.GONE

        val name = prefDisplayName.text.toString()
        val nit = prefNit.text.toString()
        val phoneNumber = prefPhoneNumber.text.toString()
        val email = prefEmail.text.toString()
        val direction = prefDirection.text.toString()
        val country = countries[spinnerCountry.selectedItemPosition]

        val preferences = getPreferences()
        val mail = preferences.getString(getString(R.string.pref_value_email_registered), "")

        val basicDataJSON = JSONObject()
        basicDataJSON.put("DisplayName", name)
        basicDataJSON.put("Nit", nit)
        basicDataJSON.put("Email", email)
        basicDataJSON.put("PhoneNumber", phoneNumber)
        basicDataJSON.put("Direction", direction)
        basicDataJSON.put("Country", country)

        val parameters = JSONObject()
        parameters.put("Email", mail)
        parameters.put("BasicData", basicDataJSON)

        val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                parameters,
                saveSuccess,
                Response.ErrorListener { _ -> gotoMain()}
        )

        queue.executeRequest(request)
    }

    private val requestSuccess = Response.Listener<JSONObject> { response: JSONObject? ->
        try {
            val success = response?.getBoolean("Success")

            if (success == true) {
                val data = response.getJSONObject("Data")
                prefDisplayName.setText(data.getString("DisplayName"))
                prefNit.setText(data.getString("Nit"))
                prefPhoneNumber.setText(data.getString("PhoneNumber"))
                prefEmail.setText(data.getString("Email"))
                prefDirection.setText(data.getString("Direction"))
                spinnerCountry.setSelection(countryAdapter.getPosition(data.getString("Country")))
            }
        }
        catch (err: Exception) { error(err) }
        finally {
            progressBar.visibility = View.GONE
            layoutBasicData.visibility = View.VISIBLE
        }
        val bundle = Bundle()
        bundle.putString("basic_data_response", response?.toString())
        mFirebaseAnalytics.logEvent("basic_data_attempt", bundle)

    }

    private val saveSuccess = Response.Listener<JSONObject> { response: JSONObject? ->
        try {
            val success = response?.getBoolean("Success")

            if (success == true) {
                val preference = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
                val editor = preference.edit()
                editor.putBoolean(getString(R.string.pref_value_is_application_basic_data_backed_up), true)
                editor.apply()
            }
        }
        catch (err: Exception) { error(err) }
        finally {
            val bundle = Bundle()
            bundle.putString("basic_data_save_response", response?.toString())
            mFirebaseAnalytics.logEvent("basic_data_save_attempt", bundle)

            gotoMain()
        }
    }

    private val requestError = Response.ErrorListener { err ->
        val bundle = Bundle()
        bundle.putString("basic_data_response", err?.message)
        mFirebaseAnalytics.logEvent("basic_data_response_error", bundle)

        if (err is com.android.volley.TimeoutError) {
            Toast.makeText(this, getString(R.string.error_request_timeout), Toast.LENGTH_LONG).show()
        }
        error(err)
    }

    private fun getCountries(): ArrayList<String> {
        val locale = Locale.getAvailableLocales()
        val countries = arrayListOf<String>()
        for (loc in locale) {
            val country = loc.displayCountry
            if (country.isNotEmpty() && !countries.contains(country)) {
                countries.add(country)
            }
        }

        Collections.sort(countries, String.CASE_INSENSITIVE_ORDER)

        return countries
    }
}
