package com.extremex.attendable

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.extremex.kotex_libs.EventListAdapter
import com.extremex.kotex_libs.EventViewHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class TeachersActivity : AppCompatActivity() {

    //
    private lateinit var fireBaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teachers)


        val signOutButton = findViewById<Button>(R.id.signoutBtn)
        val loginScreen = Intent(this, LoginActivity::class.java)
        val user: TextView = this.findViewById(R.id.User)

        fireBaseAuth = FirebaseAuth.getInstance()

        val markAttendance : Button = this.findViewById(R.id.CheckIn)
        val updateEvent: Button = this.findViewById(R.id.UpdateEvent)

        markAttendance.setOnClickListener {
            if (isCameraAvailable()){
                //mark attendance view
            } else {
                requestPermission()
            }
        }


        fireBaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")

        val ud = fireBaseAuth.currentUser?.email?.split("@")
        val uid = ud?.get(0)
        databaseReference.child("users").child(uid.toString()).get().addOnSuccessListener{
            if (it.exists()){
                user.text = "${it.child("firstname").value} ${it.child("lastname").value}"
            } else {
                user.text = "user"
            }
        }


        updateEvent.setOnClickListener {


            val uiInflater = LayoutInflater.from(this).inflate(R.layout.add_event, null)
            val popUpBox = AlertDialog.Builder(this).setView(uiInflater).setCancelable(false)
            popUpBox.create().window?.setBackgroundDrawableResource(R.color.NoBG)

            val popupMenu = popUpBox.show()
            val closeButton: Button = uiInflater.findViewById(R.id.CloseButton)
            val addEvent: Button = uiInflater.findViewById(R.id.AddEvent)
            val eventList: ListView = uiInflater.findViewById(R.id.EventList)
            val viewEvents: TextView = uiInflater.findViewById(R.id.ViewEvents)
            val calendarPickerView: ImageButton = uiInflater.findViewById(R.id.AddDueDate)
            val calendar: Calendar = Calendar.getInstance()

            val title = uiInflater.findViewById<EditText>(R.id.NameEvent)
            val description = uiInflater.findViewById<EditText>(R.id.DescriptionEvent)


            eventList.visibility = View.GONE
            viewEvents.visibility = View.GONE

            val progress: ProgressDialog = ProgressDialog(this)
            val pref: SharedPreferences = getSharedPreferences("EVENTS", MODE_PRIVATE)
            val editor = pref.edit()
            val allEntries = pref.all

            val entryKey = allEntries.keys
            val entryValue = allEntries.values
            val namelist = arrayListOf<String>()
            val descriptionlist = arrayListOf<String>()
            val dateslist = arrayListOf<String>()
            val finalList = mutableListOf<EventViewHandler>()
            if (entryKey.isNotEmpty()) {
                for (position in entryKey.indices) {
                    val valueList = entryValue.toList()
                    val values = valueList[position].toString().split("??")

                    namelist.add(values[0])
                    descriptionlist.add(values[1])
                    dateslist.add(values[2])
                }

                for (position in namelist.indices) {
                    finalList.add(
                        EventViewHandler(
                            namelist[position],
                            descriptionlist[position],
                            dateslist[position]
                        )
                    )
                    eventList.visibility = View.VISIBLE
                    viewEvents.visibility = View.VISIBLE

                }
            }


            val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }

            calendarPickerView.setOnClickListener {
                DatePickerDialog(
                    this,
                    datePicker,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()

            }

            val eventListAdaptor = EventListAdapter(this, R.layout.event_list, finalList)
            eventList.adapter = eventListAdaptor
            eventList.deferNotifyDataSetChanged()
            addEvent.setOnClickListener {
                var dueDate = formatedDate (calendar)

                if (title.text.isNullOrBlank()) {
                    title.error = "field cannot be empty"
                } else if (description.text.isNullOrBlank()) {
                    description.error = "field cannot be empty"
                } else {
                    if (dueDate.isBlank()) {
                        dueDate = "no due date"
                    }
                    progress.setTitle("Please Wait")
                    progress.setMessage("Applying Changes")
                    progress.setCancelable(false)
                    progress.show()
                    editor.putString(
                        title.text.toString(),
                        "${title.text.toString()}??${description.text.toString()}??$dueDate"
                    )
                    editor.apply()
                    eventList.run { deferNotifyDataSetChanged() }
                    eventList.adapter = eventListAdaptor
                    eventList.visibility = View.VISIBLE
                    viewEvents.visibility = View.VISIBLE
                    popupMenu.dismiss()
                    progress.dismiss()
                }

            }

            closeButton.setOnClickListener {
                popupMenu.dismiss()
            }
        }

        signOutButton.setOnClickListener {
            fireBaseAuth.signOut()
            startActivity(loginScreen);
            finish()
        }
    }

    private fun formatedDate(calender: Calendar): String {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return date.format(calender.time)
    }

    private fun isCameraAvailable(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
            1001)
    }
}

