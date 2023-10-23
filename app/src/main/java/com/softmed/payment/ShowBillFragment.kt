package com.softmed.payment

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.softmed.payment.adapters.SalesItemsAdapterRV
import com.softmed.payment.helpers.CurrencyTextView
import com.softmed.payment.helpers.FilesHelper
import com.softmed.payment.helpers.PdfHelper
import com.softmed.payment.storage.ClientesContract
import com.softmed.payment.storage.InvoiceItemsContract
import com.softmed.payment.storage.ServiciosContract
import com.softmed.payment.storage.TransactionContract
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import java.io.File
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ShowBillFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ShowBillFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ShowBillFragment : DialogFragment(), AnkoLogger {

    private var client: ClientesContract.Cliente? = null
    private var services: ArrayList<ServiciosContract.Service>? = null
    private var subtotal: Double? = null
    private var iva: Double? = null
    private var total: Double? = null
    private var invoiceNumber: Long? = null
    private var transaction: TransactionContract.Transaction? = null
    private var language: String? = null

    private var mListener: OnFragmentInteractionListener? = null

    private var ticketFile: File? = null

    private lateinit var mLayout: LinearLayoutManager
    private lateinit var mItemsAdapter: SalesItemsAdapterRV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            client = arguments!!.getParcelable(ARG_CLIENT)
            services = arguments!!.getParcelableArrayList(ARG_SERVICES)
            subtotal = arguments!!.getDouble(ARG_SUBTOTAL)
            iva = arguments!!.getDouble(ARG_IVA)
            total = arguments!!.getDouble(ARG_TOTAL)
            invoiceNumber = arguments!!.getLong(ARG_INVOICE_NUMBER)
            transaction = arguments!!.getParcelable(ARG_TRANSACTION)
            language = arguments!!.getString(ARG_LANGUAGE)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context!!.resources.configuration.locales.get(0)
        } else {
            context!!.resources.configuration.locale
        }

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bill, container, false)
        var resource: Resources? = null

        if (language != null && locale.language != language) {
            resource = getCustomResourcesFromLanguage(language!!)
            setTextLanguage(view, resource)
            mItemsAdapter = SalesItemsAdapterRV(null, language)
        } else {
            mItemsAdapter = SalesItemsAdapterRV(null)
        }

        mLayout = LinearLayoutManager(this.context)
        val rv = view.findViewById<RecyclerView>(R.id.rvNewSaleItems)
        rv.layoutManager = mLayout
        rv.adapter = mItemsAdapter
        mItemsAdapter.update(services!!.toList())

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val companyName = view.findViewById<TextView>(R.id.companyName)
        companyName.text = preferenceManager.getString(getString(R.string.pref_key_display_name), "")
        val companyNIT = view.findViewById<TextView>(R.id.companyNIT)
        companyNIT.text = preferenceManager.getString(getString(R.string.pref_key_nit), "")
        val companyDirection = view.findViewById<TextView>(R.id.companyDirection)
        companyDirection.text = preferenceManager.getString(getString(R.string.pref_key_direction), "")
        val companyPhone = view.findViewById<TextView>(R.id.companyPhone)
        companyPhone.text = PhoneNumberUtils.formatNumber(preferenceManager.getString(getString(R.string.pref_key_telephone_number), ""), locale.country)
        val companyEmail = view.findViewById<TextView>(R.id.companyEmail)
        companyEmail.text = preferenceManager.getString(getString(R.string.pref_key_email), "")
        val clientName = view.findViewById<TextView>(R.id.clientName)
        clientName.text = getString(R.string.cliente_fullname, client?.name ?: "", client?.lastname ?: "")
        val clientNit = view.findViewById<TextView>(R.id.clientNit)
        clientNit.text = client?.nit

        if (client != null) {
            val clientPhone = view.findViewById<TextView>(R.id.clientPhone)
            clientPhone.text = PhoneNumberUtils.formatNumber(client?.phoneNumber, locale.country)
        }

        val subtotal = view.findViewById<CurrencyTextView>(R.id.billSubTotal)
        subtotal.text = this.subtotal?.toString() ?: "0.0"
        val iva = view.findViewById<CurrencyTextView>(R.id.billIva)
        iva.text = this.iva?.toString() ?: "0.0"
        val total = view.findViewById<CurrencyTextView>(R.id.billTotal)
        total.text = this.total?.toString() ?: "0.0"

        val number = view.findViewById<TextView>(R.id.billInvoiceNumber)
        val formatted = String.format("%010d", invoiceNumber)
        number.text = formatted

        val preference = context!!.getSharedPreferences(BaseActivity.SHARED_PREFERENCE_FILE, 0)
        val isLimited = preference.getBoolean(
                getString(R.string.pref_value_application_is_limited),
                true
        )

        view.findViewById<Button>(R.id.btnSendTicketFile).isEnabled = !isLimited
        view.findViewById<Button>(R.id.btnSendTicketFile).setOnClickListener { sendFile(resource) }
        view.findViewById<TextView>(R.id.ticketDate).text = transaction?.paymentDate

        showTransactionInfo(view)

        return view
    }

    override fun onAttach(context: Context) {
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

    interface OnFragmentInteractionListener {
        fun onSendFileIntent(intent: Intent?, file: File)
    }

    private fun setCashVisibility(view: View, visibility: Int) {
        view.findViewById<TextView>(R.id.paymentCashText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentChangeText).visibility = visibility
        view.findViewById<CurrencyTextView>(R.id.billCash).visibility = visibility
        view.findViewById<CurrencyTextView>(R.id.billChange).visibility = visibility
    }

    private fun setCardVisibility(view: View, visibility: Int) {
        view.findViewById<TextView>(R.id.paymentReferenceText).visibility = visibility
        view.findViewById<TextView>(R.id.billCardReference).visibility = visibility
    }

    private fun setCheckVisibility(view: View, visibility: Int) {
        view.findViewById<TextView>(R.id.paymentCheckNumberText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCheckNumber).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCheckBankNameText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCheckBankName).visibility = visibility
    }

    private fun setCreditVisibility(view: View, visibility: Int) {
        view.findViewById<TextView>(R.id.paymentCreditDepositText).visibility = visibility
        view.findViewById<TextView>(R.id.paymentCreditDeposit).visibility = visibility
    }

    private fun showTransactionInfo(view: View) {
        when (transaction?.paymentType) {
            TransactionContract.PaymentMethods.Cash.ordinal -> {
                view.findViewById<TextView>(R.id.billPayMethod).text = getString(R.string.payment_method_cash)
                setCardVisibility(view, View.GONE)
                setCheckVisibility(view, View.GONE)
                setCreditVisibility(view, View.GONE)
                setCashVisibility(view, View.VISIBLE)

                view.findViewById<CurrencyTextView>(R.id.billCash).text = transaction?.paymentTotal.toString()
                view.findViewById<CurrencyTextView>(R.id.billChange).text = (transaction!!.paymentTotal - transaction!!.invoiceTotal).toString()

            }
            TransactionContract.PaymentMethods.Card.ordinal -> {
                view.findViewById<TextView>(R.id.billPayMethod).text = getString(R.string.payment_method_card)
                setCashVisibility(view, View.GONE)
                setCheckVisibility(view, View.GONE)
                setCreditVisibility(view, View.GONE)
                setCardVisibility(view, View.VISIBLE)

                view.findViewById<TextView>(R.id.billCardReference).text = transaction?.paymentCardReference
            }
            TransactionContract.PaymentMethods.Credit.ordinal -> {
                view.findViewById<TextView>(R.id.billPayMethod).text = getString(R.string.payment_method_credit)
                setCashVisibility(view, View.GONE)
                setCheckVisibility(view, View.GONE)
                setCardVisibility(view, View.GONE)
                setCreditVisibility(view, View.VISIBLE)

                view.findViewById<TextView>(R.id.paymentCreditDeposit).text = transaction?.paymentCreditDeposit.toString()
            }
            TransactionContract.PaymentMethods.Check.ordinal -> {
                setCashVisibility(view, View.GONE)
                setCardVisibility(view, View.GONE)
                setCreditVisibility(view, View.GONE)
                setCheckVisibility(view, View.VISIBLE)

                view.findViewById<TextView>(R.id.paymentCheckNumber).text = transaction?.paymentCheckNumber
                view.findViewById<TextView>(R.id.paymentCheckBankName).text = transaction?.paymentCheckBankName
            }
        }
    }

    private fun sendFile(resource: Resources?) {
        doAsync {
            val document = PdfHelper(context!!.applicationContext)
                    .createBillTickerPdf(
                            subtotal!!,
                            iva!!,
                            total!!,
                            InvoiceItemsContract.convertServicesToItems(services!!),
                            transaction!!,
                            client,
                            resource)

            val fileHelper = FilesHelper(context!!.applicationContext)
            val cacheFile = fileHelper.getCacheFile("ticket.pdf")
            if (cacheFile != null) {
                ticketFile = cacheFile
                val cacheStream = fileHelper.getCacheStream(cacheFile) ?: return@doAsync
                document.writeTo(cacheStream)
                document.close()
                cacheStream.close()

                val intent = fileHelper.intentFileToSend(cacheFile)
                mListener?.onSendFileIntent(intent, cacheFile)
            }
        }
    }

    private fun getCustomResourcesFromLanguage(language: String): Resources {
        val configuration = Configuration(context!!.resources.configuration)
        configuration.setLocale(Locale(language))
        return context!!.createConfigurationContext(configuration).resources
    }

    private fun setTextLanguage(view: View, resource: Resources) {
        with(view) {
            findViewById<TextView>(R.id.ticketText).text = resource.getString(R.string.bill_ticket)
            findViewById<TextView>(R.id.billTicketNumberText).text = resource.getString(R.string.bill_ticket_bill_number)
            findViewById<TextView>(R.id.billTicketDateText).text = resource.getString(R.string.bill_ticket_bill_date)
            findViewById<TextView>(R.id.billTicketClientNameText).text = resource.getString(R.string.bills_client_name)
            findViewById<TextView>(R.id.billTicketIdText).text = resource.getString(R.string.bill_ticket_client_nit)
            findViewById<TextView>(R.id.billTicketPhoneText).text = resource.getString(R.string.bill_ticket_client_phone)

            findViewById<TextView>(R.id.totalText).text = resource.getString(R.string.bills_invoice_total)
            findViewById<TextView>(R.id.ivaText).text = resource.getString(R.string.new_invoice_resume_iva)
            findViewById<TextView>(R.id.subtotalText).text = resource.getString(R.string.new_invoice_resume_subtotal)

            findViewById<TextView>(R.id.paymentMethodText).text = resource.getString(R.string.new_invoice_pay_method)
            findViewById<TextView>(R.id.paymentCashText).text = resource.getString(R.string.new_invoice_method_cash)
            findViewById<TextView>(R.id.paymentChangeText).text = resource.getString(R.string.new_invoice_change)
            findViewById<TextView>(R.id.paymentReferenceText).text = resource.getString(R.string.new_invoice_card_reference)
            findViewById<TextView>(R.id.paymentCheckNumberText).text = resource.getString(R.string.payment_check_number)
            findViewById<TextView>(R.id.paymentCheckBankNameText).text = resource.getString(R.string.payment_check_bank_name)
            findViewById<TextView>(R.id.paymentCreditDepositText).text = resource.getString(R.string.payment_credit_deposit)

            findViewById<TextView>(R.id.btnSendTicketFile).text = resource.getString(R.string.bills_ticket_send_file)
        }
    }

    companion object {
        private const val ARG_CLIENT = "client"
        private const val ARG_SERVICES = "services"
        private const val ARG_SUBTOTAL = "Subtotal"
        private const val ARG_IVA = "Iva"
        private const val ARG_TOTAL = "Total"
        private const val ARG_INVOICE_NUMBER = "invoice_number"
        private const val ARG_TRANSACTION = "transaction"
        private const val ARG_LANGUAGE = "language"

        fun newInstance(client: ClientesContract.Cliente?,
                        services: java.util.ArrayList<ServiciosContract.Service>,
                        subtotal: Double,
                        iva: Double,
                        total: Double,
                        invoiceNumber: Long,
                        transaction: TransactionContract.Transaction,
                        language: String? = null): ShowBillFragment {
            val fragment = ShowBillFragment()
            val args = Bundle()
            args.putParcelable(ARG_CLIENT, client)
            args.putParcelableArrayList(ARG_SERVICES, services)
            args.putDouble(ARG_SUBTOTAL, subtotal)
            args.putDouble(ARG_IVA, iva)
            args.putDouble(ARG_TOTAL, total)
            args.putLong(ARG_INVOICE_NUMBER, invoiceNumber)
            args.putParcelable(ARG_TRANSACTION, transaction)
            args.putString(ARG_LANGUAGE, language)

            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
