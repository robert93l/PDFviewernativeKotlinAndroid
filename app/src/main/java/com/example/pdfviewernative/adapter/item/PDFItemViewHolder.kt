package com.example.pdfviewernative.adapter.item

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfviewernative.R
import com.github.chrisbanes.photoview.PhotoView
import java.lang.ref.WeakReference

class PDFItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val view = WeakReference(itemView)

    var textView: TextView? = null
    var imageView: PhotoView? = null

    var bitmap: Bitmap? = null

    var page: Int = 0

    init {

        view.get()?.let {
            textView = it.findViewById(R.id.textView)
            imageView = it.findViewById(R.id.imageView)
        }
    }

    fun updateView() {
        imageView?.setImageBitmap(bitmap)

        textView?.text = "${page + 1}"

    }
}