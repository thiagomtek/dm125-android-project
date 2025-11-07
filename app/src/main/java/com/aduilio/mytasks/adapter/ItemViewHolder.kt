package com.aduilio.mytasks.adapter

import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.aduilio.mytasks.R
import com.aduilio.mytasks.databinding.ListItemBinding
import com.aduilio.mytasks.entity.Task
import com.aduilio.mytasks.listener.ClickListener
import java.time.LocalDate // Importe o LocalDate
import java.time.format.DateTimeFormatter // Importe o DateTimeFormatter
import java.util.Locale

class ItemViewHolder(
    private val binding: ListItemBinding,
    private val listener: ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    fun setData(task: Task) {
        binding.tvTitle.text = task.title

        binding.tvDate.text = formatTaskDate(task)

        val colorRes = getTaskColor(task)


        binding.root.setBackgroundResource(colorRes)



        binding.root.setOnClickListener {
            listener.onClick(task)
        }

        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(R.string.mark_completed).setOnMenuItemClickListener {
                task.id?.let { id -> listener.onComplete(task.id) }
                true
            }
        }
    }


    private fun formatTaskDate(task: Task): String {
        if (task.date == null) {
            return ""
        }
        val context = binding.root.context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val formatType = prefs.getString("date_format", "short")

        val dateFormatString = if (formatType == "long") {
            "dd 'de' MMMM 'de' yyyy"
        } else {
            "dd/MM/yyyy"
        }

        val dateFormatter = DateTimeFormatter.ofPattern(dateFormatString, Locale.getDefault())
        val formattedDate = task.date.format(dateFormatter)

        val formattedTime = if (task.time != null) {

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            task.time.format(timeFormatter)
        } else {
            ""
        }

        return if (formattedTime.isNotBlank()) {
            "$formattedDate - $formattedTime"
        } else {
            formattedDate
        }
    }

    private fun getTaskColor(task: Task): Int {
        if (task.completed) {
            return R.color.task_green
        }

        if (task.date == null) {
            return R.color.task_blue
        }

        val today = LocalDate.now()

        return when {

            task.date.isBefore(today) -> R.color.task_red


            task.date.isEqual(today) -> R.color.task_yellow

            else -> R.color.task_blue
        }
    }
}