package com.softmed.payment

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.SearchView
import com.softmed.payment.adapters.ClientListSelectAdapter
import com.softmed.payment.storage.ClientesContract
import com.softmed.payment.storage.ClientesDbHelper
import kotlinx.android.synthetic.main.activity_select_client.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SelectClientActivity : AppCompatActivity(), ClientAddFragment.OnFragmentInteractionListener, SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(text: String?): Boolean {
        adapter?.filter?.filter(text)
        return true
    }

    override fun onSaveClient(client: ClientesContract.Cliente) {
        ClientesDbHelper.getInstance(applicationContext).insert(client.name, client.lastname, client.nit)

        refreshList()
    }

    override fun onCancel() {

    }

    var clients: List<ClientesContract.Cliente>? = null
    var adapter: ClientListSelectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_client)

        fab.setOnClickListener {
            val dialog = ClientAddFragment()
            dialog.show(supportFragmentManager, "NewClient")
        }

        refreshList()

        clientListSelect.onItemClickListener = AdapterView.OnItemClickListener{
            _: AdapterView<*>, _: View, position: Int, _: Long ->

            val intent = Intent()
            intent.putExtra(AddInvoiceItemsActivity.INTENT_CLIENT, adapter?.getClientSelected(position))

            setResult(Activity.RESULT_OK, intent)
            finish()
        }
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

    private fun refreshList() {
        doAsync {
            clients = ClientesDbHelper.getInstance(this@SelectClientActivity).getAll()

            uiThread {
                adapter = ClientListSelectAdapter(this@SelectClientActivity, clients!!)
                if (clients != null) clientListSelect.adapter = adapter
            }
        }
    }
}
