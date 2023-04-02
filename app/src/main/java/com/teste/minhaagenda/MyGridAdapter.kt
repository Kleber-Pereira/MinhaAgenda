package com.teste.minhaagenda

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MyGridAdapter(
    context: Context,
    var dates: List<Date>,
    var currentDate: Calendar,
    var events: List<Events>
) :
    ArrayAdapter<Any?>(context, R.layout.singe_cell_layout) {
    var inflater: LayoutInflater

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val monthDate = dates[position]
        val dateCalendar = Calendar.getInstance()
        dateCalendar.time = monthDate
        val DayNo = dateCalendar[Calendar.DAY_OF_MONTH]
        val displayMonth = dateCalendar[Calendar.MONTH] + 1
        val displayYear = dateCalendar[Calendar.YEAR]
        val currentMonth = currentDate[Calendar.MONTH] + 1
        val currentYear = currentDate[Calendar.YEAR]
        var view = convertView
        if (view == null) {
            view = inflater.inflate(R.layout.singe_cell_layout, null)
        }
        if (displayMonth == currentMonth && displayYear == currentYear) {
            view!!.setBackgroundColor(context.resources.getColor(R.color.purple_200))
        } else {
            view!!.setBackgroundColor(Color.parseColor("#cccccc"))
        }
        val Day_Number = view.findViewById<TextView>(R.id.calendar_day)
        val EventNumber = view.findViewById<TextView>(R.id.events_id)
        Day_Number.text = DayNo.toString()
        val eventCalendar = Calendar.getInstance()
        val arrayList = ArrayList<String>()
        for (i in events.indices) {
            eventCalendar.time = ConvertStringtoDate(events[i].DATE)
            if (DayNo == eventCalendar[Calendar.DAY_OF_MONTH] && displayMonth == eventCalendar[Calendar.MONTH] + 1 && displayYear == eventCalendar[Calendar.YEAR]) {
                arrayList.add(events[i].EVENT)
                EventNumber.text = arrayList.size.toString() + " Eventos"
            }
        }
        return view
    }

    private fun ConvertStringtoDate(eventDate: String): Date? {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        var date: Date? = null
        try {
            date = format.parse(eventDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    override fun getCount(): Int {
        return dates.size
    }

    override fun getPosition(item: Any?): Int {
        return dates.indexOf(item)
    }

    override fun getItem(position: Int): Any? {
        return dates[position]
    }
}




