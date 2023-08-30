package com.softmed.payment.adapters

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.softmed.payment.BaseActivity
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ClientBirthdayListAdapter (val activity: AppCompatActivity, private val clients: JSONArray): BaseAdapter() {
    override fun getView(position: Int, convertView: View?, p2: ViewGroup?): View {
        var view: View? = convertView
        if (view == null){
            val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.layout_client_birthday_item, null)
        }

        val jsonObject: JSONObject = clients[position] as JSONObject
        val name = view?.findViewById<TextView>(R.id.clientNameText)
        name?.text = jsonObject.get(BaseActivity.BIRTHDAY_JSON_NAME).toString()

        val age = view?.findViewById<TextView>(R.id.clientAgeText)
        age?.text = jsonObject.get(BaseActivity.BIRTHDAY_JSON_AGE).toString()

        val monthDay = view?.findViewById<TextView>(R.id.clientMonthDayText)
        val monthDayText = jsonObject.get(BaseActivity.BIRTHDAY_JSON_MONTH_DAY).toString()
        monthDay?.text = monthDayText

        if (monthDayText == DateTimeHelper.dateToString(Date(), DateTimeHelper.PATTERN_BIRTHDAY_DAY_MONTH)) {
            view?.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorTeal50))
        }

        return view!!
    }

    override fun getItem(position: Int): Any = clients[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = clients.length()
}