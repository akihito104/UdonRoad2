package com.freshdigitable.udonroad2.timeline

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.Keep
import androidx.customview.view.AbsSavedState
import androidx.recyclerview.widget.RecyclerView

open class ItemClickableRecyclerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attr, defStyle) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnItemTouchListener(touchListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeOnItemTouchListener(touchListener)
    }

    private val touchListener = object : OnItemTouchListener {
        private var prevEvent: MotionEvent? = null

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    clearPreviewEvent()
                    prevEvent = MotionEvent.obtain(e)
                }
                MotionEvent.ACTION_UP -> {
                    if (isClickEvent(e, rv.context)) {
                        onClickItem(rv, e)
                    }
                    clearPreviewEvent()
                }
                MotionEvent.ACTION_CANCEL -> clearPreviewEvent()
            }
            return false
        }

        private fun isClickEvent(
            currentEvent: MotionEvent,
            context: Context
        ): Boolean {
            val previewEvent = prevEvent ?: return false
            val dx = (currentEvent.x - previewEvent.x).toDouble()
            val dy = (currentEvent.y - previewEvent.y).toDouble()
            return currentEvent.eventTime - previewEvent.eventTime < ViewConfiguration.getTapTimeout()
                || Math.hypot(dx, dy) < ViewConfiguration.get(context).scaledTouchSlop
        }

        private fun clearPreviewEvent() {
            prevEvent?.recycle()
            prevEvent = null
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    }

    interface OnItemClickListener {
        fun onItemClick(vh: ViewHolder)
    }

    var itemClickListener : OnItemClickListener? = null

    private fun onClickItem(rv: RecyclerView, e: MotionEvent) {
        val v = rv.findChildViewUnder(e.x, e.y) ?: return
        val vh = rv.findContainingViewHolder(v) ?: return
        onClickItem(vh)
    }

    open fun onClickItem(viewHolder: RecyclerView.ViewHolder) {
        itemClickListener?.onItemClick(viewHolder)
    }
}

class ItemSelectableRecyclerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ItemClickableRecyclerView(context, attr, defStyle) {

    private var selectedItemId: Long? = null

    override fun onClickItem(viewHolder: RecyclerView.ViewHolder) {
        val itemId = viewHolder.itemId
        if (selectedItemId == null) {
            selectedItemId = itemId
            viewHolder.itemView.isSelected = true
        } else {
            if (selectedItemId == itemId) {
                selectedItemId = null
                viewHolder.itemView.isSelected = false
            } else {
                val selected = selectedItemId ?: return
                val vh = findViewHolderForItemId(selected)
                vh?.itemView?.isSelected = false
                selectedItemId = itemId
                viewHolder.itemView.isSelected = true
            }
        }
        super.onClickItem(viewHolder)
    }

    override fun onChildAttachedToWindow(child: View) {
        super.onChildAttachedToWindow(child)
        val selected = selectedItemId ?: return
        val vh = findContainingViewHolder(child) ?: return
        child.isSelected = vh.itemId == selected
    }

    override fun onChildDetachedFromWindow(child: View) {
        super.onChildDetachedFromWindow(child)
        child.isSelected = false
    }

    override fun onSaveInstanceState(
    ): Parcelable? = SavedState(requireNotNull(super.onSaveInstanceState()), selectedItemId)

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        state.superState?.let { super.onRestoreInstanceState(it) }
        this.selectedItemId = state.selectedItemId
        this.selectedItemId?.let {
            val vh = findViewHolderForItemId(it)
            vh?.itemView?.isSelected = true
        }
    }

    @Keep
    class SavedState(
        superState: Parcelable,
        val selectedItemId: Long?
    ) : AbsSavedState(superState) {

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            selectedItemId?.let(parcel::writeLong)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.ClassLoaderCreator<SavedState> {
            override fun createFromParcel(source: Parcel?, loader: ClassLoader?): SavedState {
                val s = requireNotNull(source) { "source should be null." }
                val superState = requireNotNull(s.readParcelable<Parcelable>(loader)) { "superState should be not noll." }
                val selectedItemId = s.readLong()
                return SavedState(superState, selectedItemId)
            }

            override fun createFromParcel(parcel: Parcel): SavedState = createFromParcel(parcel, null)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
