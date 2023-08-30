package com.softmed.payment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.telephony.PhoneNumberUtils
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.ClientsAdapterRV
import com.softmed.payment.helpers.DateTimeHelper
import com.softmed.payment.storage.ClientesContract
import com.softmed.payment.storage.ClientesDbHelper
import com.softmed.payment.storage.InvoiceDbHelper
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_clientes.*
import kotlinx.android.synthetic.main.content_clients_rv.*
import kotlinx.android.synthetic.main.fragment_client_details.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.jetbrains.anko.uiThread
import java.util.*

class ClientesActivity : BaseActivity(),
        SearchView.OnQueryTextListener,
        ClientsAdapterRV.OnMenuActionListener, AnkoLogger {

    override fun onDetailsSelected(id: Long) {
        val alertBuilder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.fragment_client_details, null)
        alertBuilder.setView(view)

        val client = clientList?.find { it.id == id }

        view.findViewById<TextView>(R.id.clientName).text = client?.name
        view.findViewById<TextView>(R.id.clientLastName).text = client?.lastname
        view.findViewById<TextView>(R.id.clientNit).text = client?.nit
        view.findViewById<TextView>(R.id.clientEmail).text = client?.email
        view.findViewById<TextView>(R.id.clientPhoneNumber).text = PhoneNumberUtils.formatNumber(client?.phoneNumber, locale.country)
        view.findViewById<TextView>(R.id.clientDirection).text = client?.direction

        if (client != null && client.birthday != 0L) {
            view.findViewById<TextView>(R.id.clientBirthDay).text = DateTimeHelper.dateToString(client.birthday, DateTimeHelper.PATTERN_BIRTHDAY_DAY_MONTH_YEAR)
        }

        val alert = alertBuilder.create()
        alert.show()
    }

    override fun onEditSelected(id: Long) {
        val client = clientList?.find { it.id == id } ?: return

        val intent = Intent(this, ClienteEditActivity::class.java)
        intent.putExtra(ClienteParcelableName, client)
        startActivity(intent)
    }

    override fun onDeleteSelected(id: Long) {
        val client = clientList?.find { it.id == id } ?: return

        deleteAlertDialog(client)
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(text: String?): Boolean {
        mAdapter.filter.filter(text)
        return true
    }

    private var clientList: List<ClientesContract.Cliente>? = null
    private lateinit var mAdapter: ClientsAdapterRV
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var linearLayoutManager: LinearLayoutManager

    private val locale: Locale by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            application.resources.configuration.locales.get(0)
        } else {
            application.resources.configuration.locale
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "Clientes List", null)

        setContentView(R.layout.activity_clientes)
        setSupportActionBar(toolbar)
        fab.setOnClickListener {
            val intent = Intent(this, ClienteEditActivity::class.java)
            startActivity(intent)
        }

        mAdapter = ClientsAdapterRV(this)
        linearLayoutManager =
            LinearLayoutManager(this)
        with(rvClients) {
            rvClients.layoutManager = linearLayoutManager
            rvClients.adapter = mAdapter
            val animation = AnimationUtils.loadLayoutAnimation(this@ClientesActivity, R.anim.layout_animation_fall_down)
            rvClients.layoutAnimation = animation
        }
    }

    override fun onResume() {
        refreshClientList()
        super.onResume()
    }

    private fun deleteAlertDialog(client: ClientesContract.Cliente) {
        val builder = AlertDialog.Builder(this)
        with(builder) {
            setTitle(getString(R.string.cliente_remove_alert_title))
            setMessage(getString(R.string.cliente_remove_alert_message, client.name, client.lastname))
            setPositiveButton(R.string.cliente_remove_alert_positive_button, {
                _: DialogInterface, _: Int -> deleteRecord(client.id)
            })
            setNegativeButton(R.string.cliente_remove_alert_negative_button, {_: DialogInterface, _: Int -> return@setNegativeButton})
        }
        builder.create()
        builder.show()
    }

    private fun deleteRecord(id: Long) {
        doAsync {
            val debts = InvoiceDbHelper.getInstance(applicationContext).getAllDebtsForClient(id)
            if (debts == null || debts.isEmpty()) {
                ClientesDbHelper.getInstance(applicationContext).delete(id)
                refreshClientList()
            } else {
                uiThread {
                    Toast.makeText(this@ClientesActivity, getString(R.string.cliente_remove_error_has_debt_message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshClientList() {
        showProgressBar()
        doAsync {
            val clients: List<ClientesContract.Cliente>? = ClientesDbHelper.getInstance(applicationContext).getAll()
            if (clients != null) {
                uiThread {
                    mAdapter.updateClients(clients)
                    rvClients.scheduleLayoutAnimation()
                }
                clientList = clients
            }
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
        val ClienteParcelableName = "Cliente"
    }
}
