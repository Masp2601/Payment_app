package com.softmed.payment

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.ClientDebtsAdapter
import com.softmed.payment.storage.InvoiceDbHelper
import kotlinx.android.synthetic.main.activity_client_debts.*
import kotlinx.android.synthetic.main.content_client_debts.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class ClientDebtsActivity : BaseActivity(), ClientDebtsAdapter.OnItemsActions {
    override fun onPayAllSuccess() {
        refresh()
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAdapter: ClientDebtsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_debts)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Client Debts Main", null)

        linearLayoutManager =
            LinearLayoutManager(this)
        gridLayoutManager =
            GridLayoutManager(this, 2)
        clientDebtsRV.layoutManager = linearLayoutManager

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        progressBarVisibility(View.VISIBLE)
        doAsync {
            val list = InvoiceDbHelper.getInstance(applicationContext).getClientDebts()

            if (list != null) {
                mAdapter = ClientDebtsAdapter(list, this@ClientDebtsActivity)

                uiThread {
                    clientDebtsRV.adapter = mAdapter
                }
            }

            progressBarVisibility(View.GONE)
        }
    }

    private fun progressBarVisibility(visibility: Int) {
        runOnUiThread {
            progressBar.visibility = visibility
        }
    }
}
