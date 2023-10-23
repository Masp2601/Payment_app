package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.softmed.payment.helpers.CurrencyEditText
import com.softmed.payment.storage.TransactionContract
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.layoutInflater
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PaymentFragment.OnPaymentListener] interface
 * to handle interaction events.
 * Use the [PaymentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PaymentFragment : DialogFragment(), AnkoLogger {

    private var total: Double? = null
    private var turn: Double? = null
    private var cash: Double? = null

    private var mListener: OnPaymentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            total = arguments!!.getDouble(ARG_TOTAL)
            turn = arguments!!.getDouble(ARG_RETURN)
            cash = arguments!!.getDouble(ARG_CASH)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val view = context!!.layoutInflater.inflate(R.layout.fragment_payment, null)

        setSpinnerAdapter(view)

        val total = view.findViewById<TextView>(R.id.paymentTotal)
        total.text = this.total.toString()

        val rturn = view.findViewById<TextView>(R.id.paymentReturn)
        rturn.text = turn.toString()

        view.findViewById<CurrencyEditText>(R.id.paymentCreditDeposit).setText(0.0)

        setCash(view, rturn)

        builder.setView(view)
                .setPositiveButton("Aceptar") { _: DialogInterface, _: Int ->
                    val method = view.findViewById<Spinner>(R.id.paymentMethodSpinner).selectedItemPosition
                    var cash: Double? = null
                    var reference: String? = null
                    var checkNumber: String? = null
                    var checkBankName: String? = null
                    var creditDeposit: Double? = null

                    when(method) {
                        TransactionContract.PaymentMethods.Cash.ordinal -> {
                            val paymentCashTextView = view.findViewById<CurrencyEditText>(R.id.paymentCash)
                            cash = paymentCashTextView.value.toDoubleOrNull()
                            reference = null
                            checkNumber = null
                            checkBankName = null
                            creditDeposit = null
                        }
                        TransactionContract.PaymentMethods.Card.ordinal -> {
                            cash = null
                            reference = view.findViewById<EditText>(R.id.paymentReference).text.toString()
                            checkNumber = null
                            checkBankName = null
                            creditDeposit = null
                        }
                        TransactionContract.PaymentMethods.Check.ordinal -> {
                            cash = null
                            reference = null
                            checkNumber = view.findViewById<EditText>(R.id.paymentCheckNumber).text.toString()
                            checkBankName = view.findViewById<EditText>(R.id.paymentCheckBankName).text.toString()
                            creditDeposit = null
                        }
                        TransactionContract.PaymentMethods.Credit.ordinal -> {
                            val deposit = view.findViewById<CurrencyEditText>(R.id.paymentCreditDeposit).value.toDoubleOrNull() ?: 0.0
                            cash = deposit
                            reference = null
                            checkNumber = null
                            checkBankName = null
                            creditDeposit = deposit
                        }
                    }
                    mListener?.onPaymentAccepted(method,
                            cash,
                            reference,
                            checkNumber, checkBankName,
                            creditDeposit)
                }

        return builder.create()
    }

    private fun setCash(view: View, textView: TextView) {
        val cash = view.findViewById<CurrencyEditText>(R.id.paymentCash)
        cash.setText(this.cash)
        cash.setOnEditorActionListener { editorTextView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val etv = editorTextView as CurrencyEditText
                val value = etv.value.toDoubleOrNull() ?: return@setOnEditorActionListener false

                textView.text = (value - this.total!!).toString()

                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPaymentListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun setSpinnerAdapter(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.paymentMethodSpinner)
        val strings = context!!.resources.getTextArray(R.array.payment_methods_array)
        val adapter = object : ArrayAdapter<CharSequence>(context!!, android.R.layout.simple_spinner_item, strings.toMutableList()) {
            override fun isEnabled(position: Int): Boolean {
                return false
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }

            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                when(position) {
                    0 -> {
                        setCashVisibility(true, view)
                        setPaymentReferenceVisibility(false, view)
                        setPaymentCheckVisibility(false, view)
                        setPaymentCreditVisibility(false, view)
                    }
                    1 -> {
                        setCashVisibility(false, view)
                        setPaymentReferenceVisibility(true, view)
                        setPaymentCheckVisibility(false, view)
                        setPaymentCreditVisibility(false, view)
                    }
                    2 -> {
                        setCashVisibility(false, view)
                        setPaymentReferenceVisibility(false, view)
                        setPaymentCheckVisibility(true, view)
                        setPaymentCreditVisibility(false, view)
                    }
                    3 -> {
                        setCashVisibility(false, view)
                        setPaymentReferenceVisibility(false, view)
                        setPaymentCheckVisibility(false, view)
                        setPaymentCreditVisibility(true, view)
                    }
                }
            }
        }
    }

    private fun getVisibility(isVisible: Boolean) = if (isVisible) View.VISIBLE else View.GONE

    private fun setCashVisibility(isVisible: Boolean, view: View) {
        val visibility = getVisibility(isVisible)

        view.findViewById<TextView>(R.id.paymentCashText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCash).visibility = visibility
        view.findViewById<TextView>(R.id.paymentReturnText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentReturn).visibility = visibility
    }

    private fun setPaymentReferenceVisibility(isVisible: Boolean, view: View) {
        val visibility = getVisibility(isVisible)

        view.findViewById<TextView>(R.id.paymentReferenceText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentReference).visibility = visibility
    }

    private fun setPaymentCheckVisibility(isVisible: Boolean, view: View) {
        val visibility = getVisibility(isVisible)

        view.findViewById<TextView>(R.id.paymentCheckNumberText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCheckNumber).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCheckBankNameText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCheckBankName).visibility = visibility
    }

    private fun setPaymentCreditVisibility(isVisible: Boolean, view: View) {
        val visibility = getVisibility(isVisible)

        view.findViewById<TextView>(R.id.paymentCreditDepositText).visibility = visibility
        view.findViewById<EditText>(R.id.paymentCreditDeposit).visibility = visibility
    }

    interface OnPaymentListener {
        fun onPaymentAccepted(methodPayment: Int,
                              cash: Double?,
                              reference: String?,
                              checkNumber: String?, checkBankName: String?,
                              initialDeposit: Double?)
    }

    class PaymentAdapter(context: Context, resource: Int, objects: MutableList<CharSequence>) :
            ArrayAdapter<CharSequence>(context, resource, objects) {
        override fun isEnabled(position: Int): Boolean {
            if (position == 3) {
                return false;
            }
            return super.isEnabled(position)
        }
    }

    companion object {
        private val ARG_TOTAL = "total"
        private val ARG_CASH = "cash"
        private val ARG_RETURN = "return"

        fun newInstance(total: Double, cash: Double): PaymentFragment {
            val fragment = PaymentFragment()
            val args = Bundle()
            args.putDouble(ARG_TOTAL, total)
            args.putDouble(ARG_RETURN, cash - total)
            args.putDouble(ARG_CASH, cash)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
