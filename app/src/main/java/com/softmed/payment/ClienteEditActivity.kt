package com.softmed.payment

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.reports.DatePickerFragment
import com.softmed.payment.storage.ClientesContract
import com.softmed.payment.storage.ClientesDbHelper
import kotlinx.android.synthetic.main.activity_cliente_edit.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ClienteEditActivity : AppCompatActivity(), AnkoLogger {

    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private val locale: Locale by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            application.resources.configuration.locales.get(0)
        } else {
            application.resources.configuration.locale
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente_edit)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val client = intent.getParcelableExtra<ClientesContract.Cliente?>(ClientesActivity.ClienteParcelableName)
        setFieldsText(client)

        clientBirthDay.isFocusable = false
        clientBirthDay.keyListener = null
        clientBirthDay.setOnClickListener {
            val picker = DatePickerFragment.getInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            picker.setOnDataSetListener(onDateStartSelected)
            picker.show(fragmentManager, "DatePickerStart")
        }


        clientSaveButton.setOnClickListener(){
            if (clientNameEdit.text.isNullOrEmpty()){
                clientNameEdit.error = getString(R.string.error_field_required)
                clientNameEdit.requestFocus()
                return@setOnClickListener
            }
            val email = clientEmailEdit.text
            if (email!!.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                clientEmailEdit.error = getString(R.string.error_invalid_email)
                clientEmailEdit.requestFocus()
                return@setOnClickListener
            }
            doAsync {
                if (client == null) {
                    insertRecord()
                } else {
                    updateRecord(client.id)
                }
            }
            onBackPressed()
        }

        clientPhoneEdit.addTextChangedListener(PhoneNumberFormattingTextWatcher())
    }

    private fun insertRecord() {
        val name = clientNameEdit.text.toString()
        val lastname = clientLastNameEdit.text.toString()

        ClientesDbHelper.getInstance(applicationContext).insert(
                name = name,
                lastname = lastname,
                email = clientEmailEdit.text.toString(),
                phoneNumber = PhoneNumberUtils.normalizeNumber(clientPhoneEdit.text.toString()),
                direction = clientDirectionEdit.text.toString(),
                nit = clientNitEdit.text.toString(),
                birthday = getBirthdayTime(),
                birthday_month_day = getBirthdayMonthDay()
        )

        checkIsBirthdayIsInRange(name, lastname)
    }

    private fun checkIsBirthdayIsInRange(name: String, lastname: String) {
        doAsync {
            val clients = ClientesDbHelper.getInstance(applicationContext).getBirthdaysFor3Days()
            var jsonString = "[]"

            if (clients != null && clients.isNotEmpty()) {
                val json = JSONArray()
                clients.forEach {
                    val fullName = getString(R.string.cliente_fullname, it.name, it.lastname)
                    val today = Calendar.getInstance()
                    val birthday = Calendar.getInstance()
                    birthday.timeInMillis = it.birthday
                    val age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)
                    val birthdayString = DateTimeHelper.dateToString(birthday.time, DateTimeHelper.PATTERN_BIRTHDAY_DAY_MONTH)
                    val jsonObject = JSONObject()
                            .put(BaseActivity.BIRTHDAY_JSON_NAME, fullName)
                            .put(BaseActivity.BIRTHDAY_JSON_AGE, age)
                            .put(BaseActivity.BIRTHDAY_JSON_MONTH_DAY, birthdayString)
                    json.put(jsonObject)
                }

                jsonString = json.toString()
            }

            val manager = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
            val editor = manager.edit()
            editor.putString(getString(R.string.pref_value_birthday_of_the_day), jsonString)
            editor.apply()
        }
    }

    private fun getBirthdayTime(): Long {
        val birthdayText = clientBirthDay.text.toString()
        return if (birthdayText.isEmpty()) 0L else calendar.timeInMillis
    }

    private fun getBirthdayMonthDay(): String {
        val birthdayText = clientBirthDay.text.toString()
        return if (birthdayText.isEmpty()) "" else DateTimeHelper.dateToString(calendar.time, DateTimeHelper.PATTERN_BIRTHDAY_MONTH_DAY)
    }

    private fun updateRecord(id: Long) {
        val name = clientNameEdit.text.toString()
        val lastname = clientLastNameEdit.text.toString()
        val client = ClientesContract.Cliente(
                id = id,
                name = name,
                lastname = lastname,
                email = clientEmailEdit.text.toString(),
                phoneNumber = PhoneNumberUtils.normalizeNumber(clientPhoneEdit.text.toString()),
                direction = clientDirectionEdit.text.toString(),
                nit = clientNitEdit.text.toString(),
                birthday = getBirthdayTime(),
                birthday_month_day = getBirthdayMonthDay()
        )
        ClientesDbHelper.getInstance(applicationContext).update(id, client)

        checkIsBirthdayIsInRange(name, lastname)
    }

    private fun setFieldsText(client: ClientesContract.Cliente?) {
        clientNameEdit.setText(client?.name)
        clientLastNameEdit.setText(client?.lastname)
        clientNitEdit.setText(client?.nit)
        clientEmailEdit.setText(client?.email)
        clientDirectionEdit.setText(client?.direction)

        if (client != null) {
            clientPhoneEdit.setText(PhoneNumberUtils.formatNumber(client.phoneNumber, locale.country))
        }

        if (client != null && client.birthday != 0L) {
            calendar.timeInMillis = client.birthday
            clientBirthDay.setText(DateTimeHelper.dateToString(calendar.time, DateTimeHelper.PATTERN_LONG))
        }
    }

    private fun addBirthdayOfTheDay(name: String, age: Int, monthDay: String) {
        val preferenceManager = getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val str = preferenceManager.getString(getString(R.string.pref_value_birthday_of_the_day), "[]")
        val birthdays: JSONArray = JSONArray(str)
        val addJson = JSONObject().put(BaseActivity.BIRTHDAY_JSON_NAME, name)
                .put(BaseActivity.BIRTHDAY_JSON_AGE, age)
                .put(BaseActivity.BIRTHDAY_JSON_MONTH_DAY, monthDay)
        birthdays.put(addJson)

        val editor = preferenceManager.edit()
        editor.putString(getString(R.string.pref_value_birthday_of_the_day), birthdays.toString())
        editor.apply()
    }

    private val onDateStartSelected = object : DatePickerFragment.DatePickerListener {
        override fun onDateSet(year: Int, month: Int, day: Int) {
            val date: Date = DateTimeHelper.parseStringToDate("$year.${month + 1}.$day", DateTimeHelper.PATTERN_SHORT)
            clientBirthDay.setText(DateTimeHelper.dateToString(date, DateTimeHelper.PATTERN_LONG))
            calendar.time = date
        }

    }
}
