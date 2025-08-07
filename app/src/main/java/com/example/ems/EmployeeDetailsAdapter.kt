package com.example.ems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EmployeeDetailsAdapter(private val details: List<EmployeeDetail>) : RecyclerView.Adapter<EmployeeDetailsAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmployee: TextView = view.findViewById(R.id.tvEmployee)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvOffice: TextView = view.findViewById(R.id.tvOffice)
        val ivSelfie: ImageView = view.findViewById(R.id.ivSelfie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_employee_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val detail = details[position]
        holder.tvEmployee.text = detail.employee
        holder.tvType.text = detail.type
        holder.tvDate.text = detail.date
        holder.tvTime.text = detail.time
        holder.tvLocation.text = detail.location
        holder.tvOffice.text = detail.office
        Glide.with(holder.itemView.context)
            .load(detail.selfie)
            .placeholder(R.drawable.ic_placeholder_selfie)
            .into(holder.ivSelfie)
    }

    override fun getItemCount(): Int = details.size
} 