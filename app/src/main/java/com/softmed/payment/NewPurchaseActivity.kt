package com.softmed.payment

import android.os.Bundle
import android.view.View
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.ExpenseContract
import com.softmed.payment.storage.ExpensesDbHelper
import kotlinx.android.synthetic.main.activity_new_purchase.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class NewPurchaseActivity : BaseActivity() {

    private var expense: ExpenseContract.Expense? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_purchase)

        if (intent.getParcelableExtra<ExpenseContract.Expense>(PurchaseActivity.PURCHASE_EXTRA) != null) {
            expense = intent.getParcelableExtra(PurchaseActivity.PURCHASE_EXTRA)
            purchaseOrderNumber.setText(expense!!.orderNumber.toString())
            purchaseProvider.setText(expense!!.providerName)
            purchaseObservation.setText(expense!!.observation)
            purchaseTotal.setText(expense!!.totalOrder.toString())
        }

        btnSave.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            if (!validData()) {
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            doAsync {
                savePurchase(expense?.id ?: 0L)

                uiThread {
                    progressBar.visibility = View.GONE
                    terminate()
                }
            }
        }
    }

    private fun savePurchase(id: Long) {
        val date = Date()
        val dateS = DateTimeHelper.dateToString(date)
        val purchase = ExpenseContract.Expense(
                id = id,
                providerName = purchaseProvider.text.toString(),
                observation = purchaseObservation.text.toString(),
                orderNumber = purchaseOrderNumber.text.toString().toLong(),
                totalOrder = purchaseTotal.text.toString().toDouble(),
                totalPaid = purchaseTotal.text.toString().toDouble(),
                date = dateS,
                timeMS = date.time
        )

        val db = ExpensesDbHelper.getInstance(applicationContext)
        if (id == 0L) {
            db.insert(purchase)
            // Add the total purchased to the day
            addPurchaseToTotalPaidToday(purchase.totalOrder)
        }
        else {
            db.update(id, purchase)
            // if the new total is less than the older, the total for the day will be less
            val totalToUpdate = purchase.totalOrder - this.expense!!.totalOrder
            addPurchaseToTotalPaidToday(totalToUpdate)
        }
    }

    private fun validData(): Boolean {
        var isValid = true;
        val fieldRequiredError = getString(R.string.error_field_required)
        val fieldMustBeNumberError = getString(R.string.error_field_must_be_number)

        if (purchaseOrderNumber.text.isEmpty()) {
            purchaseOrderNumber.error = fieldRequiredError
            isValid = false
        }
        if (purchaseProvider.text.isEmpty()) {
            purchaseProvider.error = fieldRequiredError
            isValid = false
        }
        if (purchaseTotal.text.isEmpty()) {
            purchaseTotal.error = fieldRequiredError
            isValid = false
        }

        val total = purchaseTotal.text.toString().toDoubleOrNull()
        if (total == null) {
            purchaseTotal.error = fieldMustBeNumberError
            isValid = false
        }

        val order = purchaseOrderNumber.text.toString().toLongOrNull()
        if (order == null) {
            purchaseOrderNumber.error = fieldMustBeNumberError
            isValid = false
        }

        return isValid
    }

    private fun terminate() {
        finish()
    }
}
