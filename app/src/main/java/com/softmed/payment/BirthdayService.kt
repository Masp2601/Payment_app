package com.softmed.payment

import com.firebase.jobdispatcher.*
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.ClientesDbHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class BirthdayService: JobService(), AnkoLogger {
    companion object {
        val BIRTHDAY_TAG = "birthday_of_the_day_tag"
    }
    override fun onStopJob(job: com.firebase.jobdispatcher.JobParameters?): Boolean {
        info("Job Finished")

        createJob()

        info("new job created")
        return false
    }

    override fun onStartJob(job: com.firebase.jobdispatcher.JobParameters?): Boolean {
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

            val manager = applicationContext.getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
            val editor = manager.edit()
            editor.putString(getString(R.string.pref_value_birthday_of_the_day), jsonString)
            editor.apply()

            jobFinished(job!!, false)

            createJob()
        }

        return true
    }

    private fun createJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(applicationContext))

        val now = Calendar.getInstance()
        val start = DateTimeHelper.secondsUntilTomorrow(now)
        val end = start + 60
        val newJob = dispatcher.newJobBuilder()
                .setService(this::class.java)
                .setTag(BIRTHDAY_TAG)
                .setRecurring(false)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(start.toInt(), end.toInt()))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .build()

        dispatcher.schedule(newJob)

    }
}
