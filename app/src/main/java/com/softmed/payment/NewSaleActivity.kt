package com.softmed.payment

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.softmed.payment.adapters.NewSaleItemsSelectionRV
import com.softmed.payment.storage.ServiciosContract
import com.softmed.payment.storage.ServiciosDbHelper
import kotlinx.android.synthetic.main.activity_new_sale.*
import kotlinx.android.synthetic.main.content_new_sale_rv.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class NewSaleActivity : BaseActivity(), AnkoLogger, ServiceAddFragment.OnFragmentInteractionListener, SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(text: String?): Boolean {
        adapter.filter.filter(text)
        return true
    }

    override fun onSaveService(service: ServiciosContract.Service) {
        ServiciosDbHelper.getInstance(applicationContext).insert(service)
        refreshServices()
    }

    override fun onCancelService() {

    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var adapter: NewSaleItemsSelectionRV
    private lateinit var mLayoutManager: LinearLayoutManager
    private var selectedServiceBundle: ArrayList<ServiciosContract.Service> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_sale)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.setCurrentScreen(this, "New Invoice View", null)

        if (savedInstanceState != null){
            selectedServiceBundle = savedInstanceState.getParcelableArrayList<ServiciosContract.Service>(SelectedServicesBundle) as ArrayList<ServiciosContract.Service>
        }

        fab.setOnClickListener {
            val dialog = ServiceAddFragment()
            dialog.show(supportFragmentManager, "AddService")
        }

        fabNext.setOnClickListener {
            val selected = adapter.getSelectedList()
            if (selected.size == 0){
                Toast.makeText(this, getString(R.string.add_service_is_null_or_empty), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, AddInvoiceItemsActivity::class.java)
                intent.putParcelableArrayListExtra(ItemsSelectedExtra, selected)
                startActivity(intent)
            }
        }

        refreshServices()

        mLayoutManager = LinearLayoutManager(this)
        adapter = NewSaleItemsSelectionRV(selectedServiceBundle)

        rvNewSale.layoutManager = mLayoutManager
        rvNewSale.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(SelectedServicesBundle, adapter.getSelectedList())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    private fun refreshServices() {
        showProgressBar()
        doAsync {
            val service = ServiciosDbHelper.getInstance(applicationContext).getAll()

            uiThread {
                adapter.update(service)
                rvNewSale.scheduleLayoutAnimation()
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
        const val ItemsSelectedExtra = "ItemsSelectedExtra"
        const val SelectedServicesBundle = "listSelectedService"
    }
}
