package com.teste.minhaagenda

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CustomCalendarView : LinearLayout {
    var NextButton: ImageButton? = null
    var PreviousButton: ImageButton? = null
    var CurrentDate: TextView? = null
    var gridView: GridView? = null
    var calendar = Calendar.getInstance(Locale.ENGLISH)
    var context1: Context? = null
    var dateFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
    var monthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
    var yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
    var eventFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    var myGridAdapter: MyGridAdapter? = null
    var alertDialog: AlertDialog? = null
    var dates: MutableList<Date> = ArrayList()
    var eventsList: MutableList<Events> = ArrayList()
    var alarmYear = 0
    var alarmMonth = 0
    var alarmDay = 0
    var alarmHour = 0
    var alarmMinute = 0
    var dbOpenHelper: DBOpenHelper? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        this.context1 = context
        InitializeLayout()
        SetUpCalendar()
        PreviousButton!!.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            SetUpCalendar()
        }
        NextButton!!.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            SetUpCalendar()
        }
        gridView!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                val builder = AlertDialog.Builder(context)
                builder.setCancelable(true)
                // View addView1 = LayoutInflater.from(parent.getContext()).inflate(layout.singe_cell_layout, null);
                val addView: View =
                    LayoutInflater.from(parent.context).inflate(R.layout.add_newevent_layout, null)
                val EventName = addView.findViewById<EditText>(R.id.eventName)
                val EventTime = addView.findViewById<TextView>(R.id.eventtime)
                val SetTime = addView.findViewById<ImageButton>(R.id.seteventtime)
                val alarmMe = addView.findViewById<CheckBox>(R.id.alarm)
                val dateCalendar = Calendar.getInstance()
                dateCalendar.time = dates[position]
                alarmYear = dateCalendar[Calendar.YEAR]
                alarmMonth = dateCalendar[Calendar.MONTH]
                alarmDay = dateCalendar[Calendar.DAY_OF_MONTH]
                val AddEvent = addView.findViewById<Button>(R.id.addevent)
                SetTime.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    val hours = calendar[Calendar.HOUR_OF_DAY]
                    val minute = calendar[Calendar.MINUTE]
                    val timePickerDialog = TimePickerDialog(
                        addView.context,
                        androidx.appcompat.R.style.Theme_AppCompat_Dialog,
                        { view, hourOfDay, minute ->
                            val c = Calendar.getInstance()
                            c[Calendar.HOUR_OF_DAY] = hourOfDay
                            c[Calendar.MINUTE] = minute
                            c.timeZone = TimeZone.getDefault()
                            val hformate =
                                SimpleDateFormat("K:mm a", Locale.ENGLISH)
                            val event_Time = hformate.format(c.time)
                            EventTime.text = event_Time
                            alarmHour = c[Calendar.HOUR_OF_DAY]
                            alarmMinute = c[Calendar.MINUTE]
                        },
                        hours,
                        minute,
                        false
                    )
                    timePickerDialog.show()
                }
                val date = eventFormat.format(dates[position])
                val month = monthFormat.format(dates[position])
                val year = yearFormat.format(dates[position])
                AddEvent.setOnClickListener {
                    if (alarmMe.isChecked) {
                        SaveEvent(
                            EventName.text.toString(),
                            EventTime.text.toString(),
                            date,
                            month,
                            year,
                            "on"
                        )
                        SetUpCalendar()
                        val calendar = Calendar.getInstance()
                        calendar[alarmYear, alarmMonth, alarmDay, alarmHour] = alarmMinute
                        setAlarm(
                            calendar,
                            EventName.text.toString(),
                            EventTime.text.toString(),
                            getRequestCode(
                                date,
                                EventName.text.toString(),
                                EventTime.text.toString()
                            )
                        )
                        alertDialog!!.dismiss()
                    } else {
                        SaveEvent(
                            EventName.text.toString(),
                            EventTime.text.toString(),
                            date,
                            month,
                            year,
                            "off"
                        )
                        SetUpCalendar()
                        alertDialog!!.dismiss()
                    }
                }
                builder.setView(addView)
                alertDialog = builder.create()
                alertDialog!!.show()
            }
        gridView!!.onItemLongClickListener =
            OnItemLongClickListener { parent, view, position, id ->
                val date = eventFormat.format(dates[position])
                val builder = AlertDialog.Builder(context)
                builder.setCancelable(true)
                val showView: View =
                    LayoutInflater.from(parent.context).inflate(R.layout.show_events_layout, null)
                val recyclerView = showView.findViewById<RecyclerView>(R.id.EventRV)
                val layoutManager: RecyclerView.LayoutManager =
                    LinearLayoutManager(showView.context)
                recyclerView.layoutManager = layoutManager
                recyclerView.setHasFixedSize(true)
                val eventRecyclerAdapter =
                    EventRecyclerAdapter(showView.context, CollectEventbyDate(date))
                recyclerView.setAdapter(eventRecyclerAdapter)
                eventRecyclerAdapter.notifyDataSetChanged()
                builder.setView(showView)
                alertDialog = builder.create()
                alertDialog!!.show()
                alertDialog!!.setOnCancelListener(DialogInterface.OnCancelListener { SetUpCalendar() })
                true
            }
    }

    private fun getRequestCode(date: String, event: String, time: String): Int {
        var code = 0
        dbOpenHelper = DBOpenHelper(context)
        val database: SQLiteDatabase = dbOpenHelper!!.getReadableDatabase()
        val cursor: Cursor = dbOpenHelper!!.ReadIDEvents(date, event, time, database)
        while (cursor.moveToNext()) {
            code = cursor.getInt(cursor.getColumnIndex(DBStructure.ID))
        }
        cursor.close()
        dbOpenHelper!!.close()
        return code
    }

    private fun setAlarm(calendar: Calendar, event: String, time: String, RequestCode: Int) {
        val intent = Intent(context!!.applicationContext, AlarmReceiver::class.java)
        intent.putExtra("event", event)
        intent.putExtra("time", time)
        intent.putExtra("id", RequestCode)
        val pendingIntent =
            PendingIntent.getBroadcast(context, RequestCode, intent, PendingIntent.FLAG_ONE_SHOT)
        val alarmManager =
            context!!.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = pendingIntent
    }

    private fun CollectEventbyDate(date: String): ArrayList<Events> {
        val arrayList = ArrayList<Events>()
        dbOpenHelper = DBOpenHelper(context)
        val database: SQLiteDatabase = dbOpenHelper!!.getReadableDatabase()
        val cursor: Cursor = dbOpenHelper!!.ReadEvents(date, database)
        while (cursor.moveToNext()) {
            val event = cursor.getString(cursor.getColumnIndex(DBStructure.EVENT))
            val time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME))
            val Date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE))
            val month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH))
            val Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR))
            val events = Events(event, time, Date, month, Year)
            arrayList.add(events)
        }
        cursor.close()
        dbOpenHelper!!.close()
        return arrayList
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    private fun SaveEvent(
        event: String,
        time: String,
        date: String,
        month: String,
        year: String,
        notify: String
    ) {
        dbOpenHelper = DBOpenHelper(context)
        val database: SQLiteDatabase = dbOpenHelper!!.getWritableDatabase()
        dbOpenHelper!!.SaveEvent(event, time, date, month, year, notify, database)
        dbOpenHelper!!.close()
        Toast.makeText(context, "Evento Salvo", Toast.LENGTH_SHORT).show()
    }

    private fun InitializeLayout() {
        val inflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.calendar_layout, this)
        NextButton = view.findViewById(R.id.nextbtn)
        PreviousButton = view.findViewById(R.id.previousbtn)
        CurrentDate = view.findViewById(R.id.current_Date)
        gridView = view.findViewById(R.id.gridview)
    }

    private fun SetUpCalendar() {
        val currentDate = dateFormat.format(calendar.time)
        CurrentDate!!.text = currentDate
        dates.clear()
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar[Calendar.DAY_OF_MONTH] = 1
        val FirstDayofMonth = monthCalendar[Calendar.DAY_OF_WEEK] - 1
        monthCalendar.add(Calendar.DAY_OF_MONTH, -FirstDayofMonth)
        CollectEventsPerMonth(monthFormat.format(calendar.time), yearFormat.format(calendar.time))
        while (dates.size < MAX_CALENDAR_DAYS) {
            dates.add(monthCalendar.time)
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        myGridAdapter = MyGridAdapter(context, dates, calendar, eventsList)
        gridView!!.setAdapter(myGridAdapter)
    }

    private fun CollectEventsPerMonth(Month: String, year: String) {
        eventsList.clear()
        dbOpenHelper = DBOpenHelper(context)
        val database: SQLiteDatabase = dbOpenHelper!!.getReadableDatabase()
        val cursor: Cursor = dbOpenHelper!!.ReadEventsperMonth(Month, year, database)
        while (cursor.moveToNext()) {
            val event = cursor.getString(cursor.getColumnIndex(DBStructure.EVENT))
            val time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME))
            val date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE))
            val month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH))
            val Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR))
            val events = Events(event, time, date, month, Year)
            eventsList.add(events)
        }
        cursor.close()
        dbOpenHelper!!.close()
    }

    companion object {
        private const val MAX_CALENDAR_DAYS = 42
    }
}
