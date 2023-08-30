package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.TextView
import com.softmed.payment.helpers.CurrencyEditText


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ServiceEditFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ServiceEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ServiceEditFragment : DialogFragment() {

    interface ServiceEditDialogListener {
        fun onDialogPositiveClick(price: Double, discount: Double, amount: Double)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }
    private var listener: ServiceEditDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments!!.getString(TITLE)
        val priceSaved = arguments!!.getString(PRICE, "0.0")
        val discountSaved = arguments!!.getString(DISCOUNT, "0.0")
        val amountSaved = arguments!!.getString(AMOUNT, "1")

        val builder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.fragment_service_edit, null)
        view?.findViewById<TextView>(R.id.dialogTitle)?.text = title
        val priceE = view?.findViewById<CurrencyEditText>(R.id.servicePriceEdit)
        priceE?.setText(priceSaved.toDoubleOrNull())
        val discountE = view?.findViewById<EditText>(R.id.serviceDiscountEdit)
        discountE?.setText(discountSaved)
        val amountE = view?.findViewById<EditText>(R.id.serviceAmountEdit)
        amountE?.setText(amountSaved)

        builder.setView(view)
                .setPositiveButton(R.string.new_invoice_update_item) { _: DialogInterface, _: Int ->
                    val price = priceE?.value?.toDoubleOrNull()
                    val discount = discountE?.text.toString().toDoubleOrNull()
                    val amount = amountE?.text.toString().toDoubleOrNull()

                    val requiredText = getString(R.string.error_field_required)
                    var valid = true

                    if (price == null) {
                        priceE?.error = requiredText
                        valid = false
                    }
                    if (discount == null) {
                        discountE?.error = requiredText
                        valid = false
                    }
                    if (amount == null) {
                        amountE?.error = requiredText
                        valid = false
                    }
                    if (!valid) {
                        return@setPositiveButton
                    }

                    listener?.onDialogPositiveClick(price!!, discount!!, amount!!)
                }
                .setNegativeButton(R.string.new_invoice_cancel_item) { _: DialogInterface, _: Int ->
                    listener?.onDialogNegativeClick(this)
                }
        return builder.create()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            listener = context as ServiceEditDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("Activity must implement ServiceEditDialogListener")
        }
    }

    companion object {
        private val TITLE = "title"
        private val PRICE = "price"
        private val DISCOUNT = "discount"
        private val AMOUNT = "amount"
        fun newInstance(title: String, price: Double?, discount: Double?, amount: Double = 1.0): ServiceEditFragment {
            val frag = ServiceEditFragment()
            val args = Bundle()
            args.putString(TITLE, title)
            args.putString(PRICE, price?.toString())
            args.putString(DISCOUNT, discount?.toString())
            args.putString(AMOUNT, amount.toString())
            frag.arguments = args
            return frag
        }
    }

}// Required empty public constructor
