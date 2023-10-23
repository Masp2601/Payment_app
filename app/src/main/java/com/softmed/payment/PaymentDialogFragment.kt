package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.softmed.payment.helpers.CurrencyEditText
import com.softmed.payment.helpers.CurrencyTextView
import com.softmed.payment.storage.TransactionContract
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.support.v4.defaultSharedPreferences

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PaymentDialogFragment.OnPaymentListener] interface
 * to handle interaction events.
 * Use the [PaymentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PaymentDialogFragment: DialogFragment(), AnkoLogger {

    private lateinit var mPaymentData: TransactionContract.PaymentDialogData
    private lateinit var mMethodPayList: ArrayList<String>
    private lateinit var mListener: OnPaymentListener
    private lateinit var alertView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mPaymentData = arguments!!.getParcelable(ARG_PAYMENT_BUNDLE)!!
            mMethodPayList = arguments!!.getStringArrayList(ARG_PAYMENT_METHOD_LIST) as ArrayList<String>
        }
    }

    override fun onResume() {
        super.onResume()

        val alertDialog = dialog as AlertDialog
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
            _ ->
            val method = alertView.findViewById<Spinner>(R.id.paymentMethodSpinner).selectedItemPosition

            val paymentCash = alertView.findViewById<CurrencyEditText>(R.id.paymentCash)
            val paymentTotal = paymentCash.value.toDoubleOrNull() ?: 0.0
            val paymentData = when(method) {
                TransactionContract.PaymentMethods.Cash.ordinal -> {
                    if (paymentTotal < mPaymentData.invoiceTotal) {
                        paymentCash.error = getString(R.string.payment_cash_invalid_value_error)
                        return@setOnClickListener
                    }
                    mPaymentData.copy(paymentTotal = paymentTotal,
                            paymentMethod = TransactionContract.PaymentMethods.Cash)
                }
                TransactionContract.PaymentMethods.Card.ordinal -> {
                    val paymentReference = alertView.findViewById<EditText>(R.id.paymentReference)
                    val reference = "${paymentReference.text}"

                    if (reference.isEmpty()) {
                        paymentReference.error = getString(R.string.payment_card_reference_number_empty_error)
                        return@setOnClickListener
                    }
                    mPaymentData.copy(paymentTotal = paymentTotal,
                            cardReference = reference,
                            paymentMethod = TransactionContract.PaymentMethods.Card)
                }
                TransactionContract.PaymentMethods.Check.ordinal -> {
                    val checkNumberTV = alertView.findViewById<EditText>(R.id.paymentCheckNumber)
                    val bankNameTV = alertView.findViewById<EditText>(R.id.paymentCheckBankName)
                    var valid = true

                    if (checkNumberTV.text.isEmpty()) {
                        checkNumberTV.error = getString(R.string.payment_check_number_empty_error)
                        valid = false
                    }
                    if (bankNameTV.text.isEmpty()){
                        bankNameTV.error = getString(R.string.payment_check_bank_name_empty_error)
                        valid = false
                    }

                    if (!valid) {
                        return@setOnClickListener
                    }

                    mPaymentData.copy(paymentTotal = paymentTotal,
                            paymentMethod = TransactionContract.PaymentMethods.Check,
                            checkNumber = "${checkNumberTV.text}",
                            checkBankName = "${bankNameTV.text}")
                }
                TransactionContract.PaymentMethods.Credit.ordinal -> {
                    val creditDeposit = alertView.findViewById<CurrencyEditText>(R.id.paymentCreditDeposit)
                            .value.toDoubleOrNull() ?: 0.0
                    mPaymentData.copy(paymentTotal = creditDeposit,
                            paymentMethod = TransactionContract.PaymentMethods.Credit)
                }
                else -> {
                    mPaymentData.copy()
                }
            }

            mListener.paymentAccepted(paymentData)
            alertDialog.dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val view = context!!.layoutInflater.inflate(R.layout.fragment_payment, null)

        view.findViewById<CurrencyTextView>(R.id.paymentTotal).setText(mPaymentData.invoiceTotal)
        view.findViewById<CurrencyTextView>(R.id.paymentReturn).setText(mPaymentData.invoiceTotal - mPaymentData.paymentTotal)

        val paymentCash = view.findViewById<CurrencyEditText>(R.id.paymentCash)
        paymentCash.setText(mPaymentData.paymentTotal)

        val paymentReturn = view.findViewById<CurrencyTextView>(R.id.paymentReturn)

        addCashListener(view, paymentReturn)
        populateSpinner(view)
        builder.setView(view)
                .setPositiveButton(R.string.alert_dialog_button_accept, {
                    _, _ ->

                })

        alertView = view
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPaymentListener) {
            mListener = context
        }
        else {
            throw RuntimeException(context!!.toString() + " must implement " +
                    "PaymentDialogFragment.OnPaymentListener") as Throwable
        }

    }

    private fun addCashListener(view: View, textView: TextView) {
        val cash = view.findViewById<CurrencyEditText>(R.id.paymentCash)
        cash.setOnEditorActionListener { editorTextView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val etv = editorTextView as CurrencyEditText
                val value = etv.value.toDoubleOrNull() ?: return@setOnEditorActionListener false

                textView.text = (value - mPaymentData.invoiceTotal).toString()

                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }

    private fun populateSpinner(view: View) {
        val preference = context!!.getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val isLimited = preference.getBoolean(
                getString(R.string.pref_value_application_is_limited),
                true
        )

        val adapter = object : ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_item, mMethodPayList) {
            override fun isEnabled(position: Int): Boolean {
                if (position == 3 && isLimited) {
                    return false
                }
                return true
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)

                val textview = view as TextView

                if (position == 3 && isLimited) {
                    textview.setTextColor(Color.GRAY)
                } else {
                    textview.setTextColor(Color.BLACK)
                }

                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner = view.findViewById<Spinner>(R.id.paymentMethodSpinner)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {
                return
            }

            override fun onItemSelected(parent: AdapterView<*>, p1: View, position: Int, id: Long) {
                when(position) {
                    0 -> {
                        setCashVisibility(View.VISIBLE, view)
                        setPaymentReferenceVisibility(View.GONE, view)
                        setPaymentCheckVisibility(View.GONE, view)
                        setPaymentCreditVisibility(View.GONE, view)

                        view.findViewById<TextView>(R.id.paymentCash).requestFocus()
                    }
                    1 -> {
                        setCashVisibility(View.GONE, view)
                        setPaymentReferenceVisibility(View.VISIBLE, view)
                        setPaymentCheckVisibility(View.GONE, view)
                        setPaymentCreditVisibility(View.GONE, view)

                        view.findViewById<EditText>(R.id.paymentReference).requestFocus()
                    }
                    2 -> {
                        setCashVisibility(View.GONE, view)
                        setPaymentReferenceVisibility(View.GONE, view)
                        setPaymentCheckVisibility(View.VISIBLE, view)
                        setPaymentCreditVisibility(View.GONE, view)

                        view.findViewById<EditText>(R.id.paymentCheckNumber).requestFocus()
                    }
                    3 -> {
                        setCashVisibility(View.GONE, view)
                        setPaymentReferenceVisibility(View.GONE, view)
                        setPaymentCheckVisibility(View.GONE, view)
                        setPaymentCreditVisibility(View.VISIBLE, view)

                        view.findViewById<CurrencyEditText>(R.id.paymentCreditDeposit).setText(0.0)
                        view.findViewById<EditText>(R.id.paymentCreditDeposit).requestFocus()
                    }
                }
            }
        }
    }

    private fun setCashVisibility(visibility: Int, view: View) {
        view.findViewById<TextView>(R.id.paymentCashText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCash).visibility = visibility
        view.findViewById<TextView>(R.id.paymentReturnText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentReturn).visibility = visibility
    }

    private fun setPaymentCheckVisibility(visibility: Int, view: View) {
        view.findViewById<TextView>(R.id.paymentCheckNumberText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCheckNumber).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCheckBankNameText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCheckBankName).visibility = visibility
    }

    private fun setPaymentCreditVisibility(visibility: Int, view: View) {
        view.findViewById<TextView>(R.id.paymentCreditDepositText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCreditDeposit).visibility = visibility
    }

    private fun setPaymentReferenceVisibility(visibility: Int, view: View) {
        view.findViewById<TextView>(R.id.paymentReferenceText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentReference).visibility = visibility
    }

    interface OnPaymentListener {
        fun paymentAccepted(paymentDialogData: TransactionContract.PaymentDialogData)
    }

    companion object {
        private const val ARG_PAYMENT_BUNDLE = "payment_dialog_bundle"
        private const val ARG_PAYMENT_METHOD_LIST = "payment_method_list"

        fun newInstance(payment: TransactionContract.PaymentDialogData,
                        paymentMethods: ArrayList<String>): PaymentDialogFragment {
            val args = Bundle()

            val paymentBundle = if (payment.paymentTotal == 0.0) {
                payment.copy(paymentTotal = payment.invoiceTotal)
            } else {
                payment
            }

            args.putParcelable(ARG_PAYMENT_BUNDLE, paymentBundle)
            args.putStringArrayList(ARG_PAYMENT_METHOD_LIST, paymentMethods)

            val fragment = PaymentDialogFragment()
            fragment.arguments = args

            return fragment
        }
    }
}