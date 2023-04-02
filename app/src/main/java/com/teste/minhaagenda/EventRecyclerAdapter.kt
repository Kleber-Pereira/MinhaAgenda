package com.teste.minhaagenda

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class EventRecyclerAdapter(var context: Context, var arrayList: ArrayList<Events>) :
    RecyclerView.Adapter<EventRecyclerAdapter.MyViewHolder>() {
    var dbOpenHelper: DBOpenHelper? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.show_event_rowlayout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val events = arrayList[position]
        holder.Event.setText(events.EVENT)
        holder.DateTxt.setText(events.DATE)
        holder.Time.setText(events.TIME)
        holder.delete.setOnClickListener {
            deleteCalendarEvent(events.EVENT, events.DATE, events.TIME)
            arrayList.removeAt(position)
            notifyDataSetChanged()
        }
        if (isArmed(events.DATE, events.EVENT, events.TIME)) {
            holder.setAlarm.setImageResource(R.drawable.notifications_active)
            //notifyDataSetChanged();
        } else {
            holder.setAlarm.setImageResource(R.drawable.notifications_off)
            //notifyDataSetChanged();
        }
        val dateCalendar = Calendar.getInstance()
        dateCalendar.time = ConvertStringtoDate(events.DATE)
        val alarmYear = dateCalendar[Calendar.YEAR]
        val alarmMonth = dateCalendar[Calendar.MONTH]
        val alarmDay = dateCalendar[Calendar.DAY_OF_MONTH]
        val timeCalendar = Calendar.getInstance()
        timeCalendar.time = ConvertStringtoTime(events.TIME)
        val alarmhour = timeCalendar[Calendar.HOUR_OF_DAY]
        val alarmMinute = timeCalendar[Calendar.MINUTE]
        holder.setAlarm.setOnClickListener {
            if (isArmed(events.DATE, events.EVENT, events.TIME)) {
                holder.setAlarm.setImageResource(R.drawable.notifications_off)
                cancelAlarm(getRequestCode(events.DATE, events.EVENT, events.TIME))
                updateEvent(events.DATE, events.EVENT, events.TIME, "off")
                notifyDataSetChanged()
            } else {
                holder.setAlarm.setImageResource(R.drawable.notifications_active)
                val alarmCalendar = Calendar.getInstance()
                alarmCalendar[alarmYear, alarmMonth, alarmDay, alarmhour] = alarmMinute
                setAlarm(
                    alarmCalendar, events.EVENT, events.TIME,
                    getRequestCode(events.DATE, events.EVENT, events.TIME)
                )
                updateEvent(events.DATE, events.EVENT, events.TIME, "on")
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var DateTxt: TextView
        var Event: TextView
        var Time: TextView
        var delete: Button
        var setAlarm: ImageButton

        init {
            DateTxt = itemView.findViewById(R.id.eventdate)
            Event = itemView.findViewById(R.id.eventname)
            Time = itemView.findViewById(R.id.eventtime)
            delete = itemView.findViewById(R.id.delete)
            setAlarm = itemView.findViewById(R.id.alarmBtn)
        }
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

    private fun ConvertStringtoTime(eventDate: String): Date? {
        val format = SimpleDateFormat("kk:mm", Locale.ENGLISH)
        var date: Date? = null
        try {
            date = format.parse(eventDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    private fun deleteCalendarEvent(event: String, date: String, time: String) {
        dbOpenHelper = DBOpenHelper(context)
        val database = dbOpenHelper!!.writableDatabase
        dbOpenHelper!!.deleteEvent(event, date, time, database)
        dbOpenHelper!!.close()
    }

    private fun isArmed(date: String, event: String, time: String): Boolean {
        var alarmed = false
        dbOpenHelper = DBOpenHelper(context)
        val database = dbOpenHelper!!.readableDatabase
        val cursor = dbOpenHelper!!.ReadIDEvents(date, event, time, database)
        while (cursor.moveToNext()) {
            val notify = cursor.getString(cursor.getColumnIndex(DBStructure.Notify))
            alarmed = if (notify == "on") {
                true
            } else {
                false
            }
        }
        cursor.close()
        dbOpenHelper!!.close()
        return alarmed
    }

    private fun setAlarm(calendar: Calendar, event: String, time: String, RequestCode: Int) {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
        intent.putExtra("event", event)
        intent.putExtra("time", time)
        intent.putExtra("id", RequestCode)
        val pendingIntent =
            PendingIntent.getBroadcast(context, RequestCode, intent, PendingIntent.FLAG_ONE_SHOT)
        val alarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = pendingIntent
    }

    private fun cancelAlarm(RequestCode: Int) {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, RequestCode, intent, PendingIntent.FLAG_ONE_SHOT)
        val alarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun getRequestCode(date: String, event: String, time: String): Int {
        var code = 0
        dbOpenHelper = DBOpenHelper(context)
        val database = dbOpenHelper!!.readableDatabase
        val cursor = dbOpenHelper!!.ReadIDEvents(date, event, time, database)
        while (cursor.moveToNext()) {
            code = cursor.getInt(cursor.getColumnIndex(DBStructure.ID))
        }
        cursor.close()
        dbOpenHelper!!.close()
        return code
    }

    private fun updateEvent(date: String, event: String, time: String, notify: String) {
        dbOpenHelper = DBOpenHelper(context)
        val database = dbOpenHelper!!.writableDatabase
        dbOpenHelper!!.updateEvent(date, event, time, notify, database)
        dbOpenHelper!!.close()
    }
}
