package com.softmed.payment.helpers

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class DividerItemDecoration constructor(context: Context): RecyclerView.ItemDecoration() {

    private val attrs: IntArray = IntArray(1) {android.R.attr.listDivider}
    private var divider: Drawable

    init {
        val styledAttributes: TypedArray = context.obtainStyledAttributes(attrs)
        divider = styledAttributes.getDrawable(0)!!
        styledAttributes.recycle()
    }

    constructor(context: Context, resId: Int) : this(context) {
        divider = ContextCompat.getDrawable(context, resId)!!
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left: Int = parent.paddingLeft
        val right: Int = parent.width - parent.paddingRight

        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child: View = parent.getChildAt(i)
            val params: RecyclerView.LayoutParams = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }
}