package com.softmed.payment.helpers

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class SpaceItemDecoration(private val verticalSpaceHeight: Int,
                          private val horizontalSpaceWidth: Int = 0): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = verticalSpaceHeight
        outRect.bottom = verticalSpaceHeight
        outRect.left = horizontalSpaceWidth
        outRect.right = horizontalSpaceWidth
    }
}