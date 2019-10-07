package ru.werekitty.knowplaces.views

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_addcity.view.*
import ru.werekitty.knowplaces.R

class AddCityDialog : DialogFragment() {

    private lateinit var mListener: Listener

    interface Listener {
        fun DialogHelper(text: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as Listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_addcity, null)
        return AlertDialog.Builder(context!!)
            .setView(view!!)
            .setPositiveButton(android.R.string.ok, {_,_ ->
                mListener.DialogHelper(view.city_input.text.toString())
            })
            .setNegativeButton(android.R.string.cancel, {_,_ ->

            })
            .setTitle(R.string.enter_city_name)
            .create()
    }

}