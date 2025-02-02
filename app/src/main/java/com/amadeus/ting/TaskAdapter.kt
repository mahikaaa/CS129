package com.amadeus.ting

import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList


class TaskAdapter(private val context: Context) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private val dbHelper = TingDatabase(context)
    private var taskList: List<TaskModel> = ArrayList()
    private var onClickItem: ((TaskModel) ->Unit)?=null
    private var selectedDate: String? = null

    //Takes the date attribute from the current calendarDateModel and performs a filter on the list of tasks
    fun addList(lists: List<TaskModel>, calendarDateModel: CalendarDateModel?=null) {
        //Clicking on a date in the horizontal calendar filters the tasks by the selected date
        if(calendarDateModel != null){
            val filteredList = lists.filter {it.taskDate.substringBefore("  |") == calendarDateModel.calendarDatefull}
            this.taskList = filteredList
        }
        //Shows all tasks by default
        else{
            this.taskList = lists
        }
        notifyDataSetChanged()
    }

    fun clearList() {
        this.taskList = ArrayList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        var itemView = LayoutInflater.from(parent.context).inflate(R.layout.view_tasks, parent, false)
        return TaskViewHolder(itemView)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.view_title)
        var detailsTextView: TextView = itemView.findViewById(R.id.view_details)
        var dateTextView: TextView = itemView.findViewById(R.id.view_date)
        var labelTextView: TextView = itemView.findViewById(R.id.view_label)
        var layout = itemView.findViewById<LinearLayout>(R.id.task_layout)
        var btnDeleteTask = itemView.findViewById<Button>(R.id.btnDeleteTask)
        var btnCheckTask = itemView.findViewById<Button>(R.id.btnCheckTask)

        // New views for expandable card view
        var cardView: CardView = itemView.findViewById(R.id.task_card)




    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {

        var currentItem = taskList[position]
        //Glide.with(context).load(currentItem.image).into(holder.)
        holder.titleTextView.text = currentItem.taskTitle
        holder.detailsTextView.text = currentItem.taskDetails
        holder.dateTextView.text = currentItem.taskDate
        holder.labelTextView.text = currentItem.taskLabel
        holder.itemView.setOnClickListener{onClickItem?.invoke(currentItem)}

        if (currentItem.isChecked) {
            holder.btnCheckTask.visibility = View.GONE
        } else {
            holder.btnCheckTask.visibility = View.VISIBLE
        }
        holder.btnDeleteTask.setOnClickListener {

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Delete Task")
            builder.setMessage("Are you sure you want to delete this task?")
            builder.setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteTask(currentItem)
                taskList = taskList.filter { it.taskId != currentItem.taskId }
                notifyDataSetChanged()
            }
            builder.setNegativeButton("No") { _, _ -> }
            builder.show()
        }
        holder.btnCheckTask.setOnClickListener {

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Done Task")
            builder.setMessage("Are you sure you're done with this task?")
            builder.setPositiveButton("Yes") { _, _ ->
                currentItem.isChecked = true // Set isChecked to true
                dbHelper.updateTask(currentItem) // Update the task in the database
                taskList = taskList.toMutableList().apply { removeAt(position) } // Remove the item from the list
                notifyItemRemoved(position) // Notify the adapter about the removal
            }
            builder.setNegativeButton("No") { _, _ -> }
            builder.show()
        }

        holder.cardView.setOnClickListener {
            if (currentItem.isChecked){
                null
            }
            else {
                edit_task(currentItem, holder.itemView)
            }
        }

        when (currentItem.taskLabel) {
            "☆ School" -> holder.layout.setBackgroundColor(Color.parseColor("#AEC6CF"))
            "♡ Personal" -> holder.layout.setBackgroundColor(Color.parseColor("#C3B1E1"))
            "\uD83C\uDF82 Birthday" -> holder.layout.setBackgroundColor(Color.parseColor("#F4949E"))
            else -> holder.layout.setBackgroundColor(Color.parseColor("#00917C"))
        }

    }


    private fun edit_task(currentItem: TaskModel, itemView: View) {
        val inflater = LayoutInflater.from(itemView.context)
        val dialogLayout = inflater.inflate(R.layout.create_popupwindow, itemView as ViewGroup, false)
        val editTitle = dialogLayout.findViewById<EditText>(R.id.edit_title)
        val editDetails = dialogLayout.findViewById<EditText>(R.id.edit_details)
        val dateButton = dialogLayout.findViewById<Button>(R.id.dateButton)
        val labelSpinner = dialogLayout.findViewById<Spinner>(R.id.task_spinner)

        editTitle.setText(currentItem.taskTitle)
        editDetails.setText(currentItem.taskDetails)
        val savedDate = currentItem.taskDate
        dateButton.text = savedDate
        val dateOpt = DatePick(dateButton)
        dateOpt.pickDate()
        val labelIndex = getLabelIndex(currentItem.taskLabel)
        labelSpinner.setSelection(labelIndex)
        labelSpinner.setSelection(labelIndex)

        val builder = AlertDialog.Builder(itemView.context)
        builder.setView(dialogLayout)
        builder.setCancelable(false)
        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancel_button)
        val dialog = builder.create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        val saveButton = dialogLayout.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            val title = editTitle?.text.toString()
            val details = editDetails?.text.toString()
            val date = dateButton?.text.toString()
            val label = labelSpinner?.selectedItem.toString()


            val updatedTask = TaskModel(currentItem.taskId, title, details, date, label)
            dbHelper.updateTask(updatedTask)
            val index = taskList.indexOf(currentItem)
            taskList = taskList.toMutableList().also { it[index] = updatedTask }
            notifyDataSetChanged()
            dialog.dismiss()
        }

        val window = dialog.window
        window?.setGravity(Gravity.CENTER)
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.gravity = Gravity.CENTER  // Center both horizontally and vertically
        window?.attributes = layoutParams
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()


    }

    private fun getLabelIndex(label: String): Int {
        return when (label) {
            "☆ School" -> 1
            "♡ Personal" -> 0
            "\uD83C\uDF82 Birthday" -> 2
            else -> 3
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

}
