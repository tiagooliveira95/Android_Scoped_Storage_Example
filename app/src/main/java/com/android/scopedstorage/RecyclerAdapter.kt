package com.android.scopedstorage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_file_row.view.*

class RecyclerAdapter : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    var data: List<FileData> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        fun bind(data: FileData){
            with(view){
                name.text = "fileName: ${data.name}"
                mime.text = "mime: ${data.mime}"
                isDir.text = "isDir: ${data.isDir}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_file_row, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount() = data.size
}