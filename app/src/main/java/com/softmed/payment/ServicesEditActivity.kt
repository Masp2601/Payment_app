package com.softmed.payment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.storage.ServiciosContract
import com.softmed.payment.storage.ServiciosDbHelper
import kotlinx.android.synthetic.main.activity_services_edit.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class ServicesEditActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services_edit)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val service = intent.getParcelableExtra<ServiciosContract.Service>(ServicesActivity.ServiceParcelableName)
        setFieldsText(service)

        serviceSaveButton.setOnClickListener({
            if (serviceNameEdit.text.isNullOrEmpty()) {
                serviceNameEdit.error = getString(R.string.error_field_required)
                return@setOnClickListener
            }

            doAsync {
                val data = getData()
                if (service == null){
                    insertService(data)
                } else {
                    updateService(service.id, data)
                }
                uiThread {
                    onBackPressed()
                }
            }

        })
    }

    private fun getData() : ServiciosContract.Service {
        val price = servicePriceEdit.value.toDoubleOrNull() ?: 0.0
        val discount = serviceDiscountEdit.text.toString().toDoubleOrNull() ?: 0.0
        val iva = serviceIvaEdit.text.toString().toDoubleOrNull() ?: 0.0

        return ServiciosContract.Service(
                id = 0,
                name = serviceNameEdit.text.toString(),
                price = price,
                discount = discount,
                iva = iva
        )
    }

    private fun insertService(service: ServiciosContract.Service) {
        val db = ServiciosDbHelper.getInstance(applicationContext)
        db.insert(service)
    }

    private fun updateService(id: Long, service: ServiciosContract.Service){
        val db =  ServiciosDbHelper.getInstance(applicationContext)
        db.update(id, service)
    }

    private fun setFieldsText(service: ServiciosContract.Service?) {
        val price = service?.price ?: 0.0
        val discount = service?.discount ?: 0.0
        val iva = service?.iva ?: 0.0
        serviceNameEdit.setText(service?.name)
        servicePriceEdit.setText(price)
        serviceDiscountEdit.setText(discount.toString())
        serviceIvaEdit.setText(iva.toString())
    }
}
