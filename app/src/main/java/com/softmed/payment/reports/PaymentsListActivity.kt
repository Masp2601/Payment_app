package com.softmed.payment.reports

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.softmed.payment.adapters.PaymentsResumeListAdapter
import com.softmed.payment.R
import com.softmed.payment.adapters.PaymentsResumeRVAdapter
import com.softmed.payment.helpers.DividerItemDecoration
import com.softmed.payment.storage.TransactionContract
import kotlinx.android.synthetic.main.activity_payments_list.*

class PaymentsListActivity : AppCompatActivity() {

    private var payments: List<TransactionContract.Transaction>? = null
    private lateinit var mAdapter: PaymentsResumeRVAdapter
    private lateinit var mLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments_list)
        payments = intent.getParcelableArrayListExtra<TransactionContract.Transaction>(EXTRA_PAYMENTS)

        mAdapter = PaymentsResumeRVAdapter()
        mLayoutManager = LinearLayoutManager(this)

        rvTransaction.adapter = mAdapter
        rvTransaction.layoutManager = mLayoutManager
        rvTransaction.addItemDecoration(DividerItemDecoration(this))
        
        mAdapter.update(payments)
        rvTransaction.scheduleLayoutAnimation()
    }

    companion object {
        val EXTRA_PAYMENTS = "paymentsParcelableListArray"
    }
}
