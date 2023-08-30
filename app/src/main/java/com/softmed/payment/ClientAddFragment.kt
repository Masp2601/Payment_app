package com.softmed.payment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.softmed.payment.storage.ClientesContract
import org.jetbrains.anko.support.v4.act


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ClientAddFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
class ClientAddFragment : DialogFragment() {

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.fragment_client_add, null)

        builder.setView(view)

        val saveButton = view.findViewById<Button>(R.id.clientSaveButton)
        saveButton.setOnClickListener {
            val name = view.findViewById<EditText>(R.id.clientNameEdit)

            if (name.text.isNullOrEmpty()) {
                name.error = getString(R.string.error_field_required)
                name.requestFocus()
                return@setOnClickListener
            }

            val lastname = view.findViewById<EditText>(R.id.clientLastNameEdit).text.toString()
            val nit = view.findViewById<EditText>(R.id.clientNitEdit).text.toString()
            val client = ClientesContract.Cliente(
                    id = 0,
                    name = name.text.toString(),
                    lastname = lastname,
                    nit = nit
            )

            mListener?.onSaveClient(client)
            dismiss()
        }

        val cancelButton = view.findViewById<Button>(R.id.clientCancelButton)
        cancelButton.setOnClickListener {
            mListener?.onCancel()
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
        fun onSaveClient(client: ClientesContract.Cliente)
        fun onCancel()
    }
}// Required empty public constructor
