package com.softmed.payment

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.softmed.payment.adapters.PurchaseListAdapter
import com.softmed.payment.storage.ExpenseContract
import com.softmed.payment.storage.ExpensesDbHelper
import kotlinx.android.synthetic.main.activity_purchase.*
import kotlinx.android.synthetic.main.content_purchase.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class PurchaseActivity : BaseActivity() {

    private var expenses: List<ExpenseContract.Expense>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { view ->
            val intent = Intent(this, NewPurchaseActivity::class.java)
            startActivity(intent)
        }

        refreshList()

        expenseList.setOnItemClickListener { _, _, i, _ ->
            val selected = expenses?.get(i)
            val intent = Intent(this, NewPurchaseActivity::class.java)
            intent.putExtra(PURCHASE_EXTRA, selected)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        refreshList()
    }

    private fun refreshList() {
        showProgressBar()
        doAsync {
            val list = ExpensesDbHelper.getInstance(applicationContext).getAll()

            uiThread {
                if (list != null) {
                    expenseList.adapter = PurchaseListAdapter(this@PurchaseActivity, list)
                }
            }

            expenses = list
            hideProgressBar()
        }
    }

    private fun showProgressBar() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
    }

    companion object {
        val PURCHASE_EXTRA = "purchase_extra"
    }
}
