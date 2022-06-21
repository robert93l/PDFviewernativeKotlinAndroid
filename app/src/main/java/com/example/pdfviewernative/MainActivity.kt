package com.example.pdfviewernative

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfviewernative.adapter.PDFAdapter
import com.example.pdfviewernative.viewmodel.PDFReaderViewModel
import com.example.pdfviewernative.viewmodel.PDFReaderViewModelState
import kotlinx.android.synthetic.main.view_holder_pdf_item.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<PDFReaderViewModel>()

    private val permissionManager = PermissionManager(this)

    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView)
    }

    private val progress: LinearLayout by lazy {
        findViewById(R.id.progress)
    }

    private val textViewProgress: TextView by lazy {
        findViewById(R.id.textViewProgress)
    }


    private lateinit var adapter: PDFAdapter

    private val totalPDFBitmapList = mutableListOf<Bitmap>()

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initFlow()
        initList()

        permissionManager.requestPermission(
            "Permission",
            "Permissions are necessary",
            "setting",
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

        ) {
            viewModel.pdf(
                this,
                "https://www.ujaen.es/servicios/negapoyo/sites/servicio_negapoyo/files/uploads/Modelo%20Archivo%20en%20Formato%20Digital.pdf"
            ) //poner URL de ejemplo PDF
        }
    }

    private fun initFlow() {

        lifecycleScope.launch(Dispatchers.Main) {

            whenCreated {

                viewModel.pdfReaderViewModelState.collect {

                    when (it) {

                        is PDFReaderViewModelState.OnPDFFile -> {
                            Log.d("???", "pdf file ${it.file.length()}")
                            hideProgress()

                            viewModel.bitmaps(it.file, getScreenWidth(this@MainActivity))
                        }

                        is PDFReaderViewModelState.OnBitmaps -> {

                            hideProgress()
                            totalPDFBitmapList.clear()
                            totalPDFBitmapList.addAll(it.list)

                            reload()
                        }

                        is PDFReaderViewModelState.Error -> {
                            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                            hideProgress()
                        }

                        is PDFReaderViewModelState.Empty -> {
                            Toast.makeText(this@MainActivity, "empty", Toast.LENGTH_SHORT).show()
                            hideProgress()
                        }

                        is PDFReaderViewModelState.Loading -> {
                            showProgress()
                        }

                        is PDFReaderViewModelState.Progress -> {
                            textViewProgress.post {

                                textViewProgress.text = if (it.progress != 0) {
                                    "${it.progress}"
                                } else {
                                    ""
                                }
                            }
                        }

                        is PDFReaderViewModelState.None -> Unit
                    }
                }
            }
        }
    }

    private fun initList() {
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapter = PDFAdapter {
            loadMore()
        }

        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {

                    currentIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                }
            }
        })

        PagerSnapHelper().attachToRecyclerView(recyclerView)
    }

    private fun reload() {
        recyclerView.post {

            adapter.reload(fetchData(0, 10))
        }
    }

    private fun loadMore() {

        recyclerView.post {
            adapter.loadMore(fetchData(adapter.itemCount, 5))
        }
    }

    private fun fetchData(offset: Int, limit: Int): List<Bitmap> {

        val list = mutableListOf<Bitmap>()

        for (i in offset until offset + limit) {
            if (i > totalPDFBitmapList.size - 1) {
                break
            }

            list.add(totalPDFBitmapList[i])
        }
        return list

    }

    private fun getScreenWidth(context: Context): Int {

        val outMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val display = context.display
            display?.getRealMetrics(outMetrics)
        } else {

            val display =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            display.getMetrics(outMetrics)
        }
        return outMetrics.widthPixels

    }

    private fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
    }

}