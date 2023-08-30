package com.softmed.payment

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.SearchView
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.ServicesAdapterRV
import com.softmed.payment.adapters.ServicesListAdapter
import com.softmed.payment.helpers.DividerItemDecoration
import com.softmed.payment.helpers.SpaceItemDecoration
import com.softmed.payment.storage.ServiciosContract
import com.softmed.payment.storage.ServiciosDbHelper
import kotlinx.android.synthetic.main.activity_services.*
import kotlinx.android.synthetic.main.content_services_rv.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.jetbrains.anko.uiThread

class ServicesActivity : BaseActivity(),
        SearchView.OnQueryTextListener,
        ServicesAdapterRV.OnMenuItemClick{
    override fun onEditSelected(id: Long) {
        val service = services?.find { it.id == id }
        val intent = Intent(this, ServicesEditActivity::class.java)
        intent.putExtra(ServiceParcelableName, service)
        startActivity(intent)
    }

    override fun onDeleteSelected(id: Long) {
        val service = services?.find { it.id == id }
        if (service != null) deleteAlertDialog(service)
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(text: String?): Boolean {
        mAdapter.filter.filter(text)
        return true
    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    var services: List<ServiciosContract.Service>? = null
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: ServicesAdapterRV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        setContentView(R.layout.activity_services)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        fab.setOnClickListener {
            val intent = Intent(this, ServicesEditActivity::class.java)
            startActivity(intent)
        }

        //registerForContextMenu(servicesListView)

        mAdapter = ServicesAdapterRV(this)
        mLayoutManager = LinearLayoutManager(this)

        rvServices.layoutManager = mLayoutManager
        rvServices.adapter = mAdapter
        rvServices.addItemDecoration(DividerItemDecoration(this))
        rvServices.addItemDecoration(SpaceItemDecoration(4))
    }

    override fun onResume() {
        refreshServicesList()
        super.onResume()
    }

    private fun deleteAlertDialog(service: ServiciosContract.Service) {
        val builder = AlertDialog.Builder(this)
        with(builder) {
            setTitle(service.name)
            setMessage("Seguro que desea borrar el servicio?")
            setPositiveButton("Sí", {
                _: DialogInterface, _: Int -> deleteRecord(service.id)
            })
            setNegativeButton("No", { _: DialogInterface, _: Int -> return@setNegativeButton})
        }
        builder.create()
        builder.show()
    }

    private fun deleteRecord(id: Long) {
        doAsync {
            ServiciosDbHelper.getInstance(applicationContext).delete(id)
            refreshServicesList()
        }
    }
    
    private fun refreshServicesList() {
        showProgressBar()
        doAsync {
            val s = ServiciosDbHelper.getInstance(applicationContext).getAll()
            if (s != null) {
                uiThread {
                    mAdapter.update(s)
                    rvServices.scheduleLayoutAnimation()
                }
                services = s
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
        val ServiceParcelableName = "SERVICE_PARCELABLE_NAME"
    }
}
