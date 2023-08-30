package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.softmed.payment.storage.ServiciosContract


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ServiceAddFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ServiceAddFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ServiceAddFragment : DialogFragment() {

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.fragment_service_add, null)

        builder.setView(view)

        val saveButton = view.findViewById<Button>(R.id.serviceSaveButton)
        saveButton.setOnClickListener { _: View ->
            val serviceName = view.findViewById<EditText>(R.id.serviceNameEdit)

            if (serviceName.text.toString().isEmpty()) {
                serviceName.error = getString(R.string.error_field_required)
                serviceName.requestFocus()
                return@setOnClickListener
            }

            val servicePrice = view.findViewById<com.softmed.payment.helpers.CurrencyEditText>(R.id.servicePriceEdit)
            val serviceIva = view.findViewById<EditText>(R.id.serviceIvaEdit)
            val serviceDiscount = view.findViewById<EditText>(R.id.serviceDiscountEdit)
            val service = ServiciosContract.Service(
                    id = 0,
                    name = serviceName.text.toString(),
                    price = servicePrice.value.toDouble(),
                    iva = serviceIva.text.toString().toDoubleOrNull() ?: 0.0,
                    discount = serviceDiscount.text.toString().toDoubleOrNull() ?: 0.0
            )

            mListener?.onSaveService(service)

            dismiss()
        }
        val cancelButton = view.findViewById<Button>(R.id.serviceCancelButton)
        cancelButton.setOnClickListener { _: View ->
            mListener?.onCancelService()
            dismiss()
        }

        return builder.create()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {

        fun onSaveService(service: ServiciosContract.Service)
        fun onCancelService()
    }
}// Required empty public constructor
