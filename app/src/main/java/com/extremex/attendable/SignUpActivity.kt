package com.extremex.attendable

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.extremex.kotex_libs.databaseVars

class SignUpActivity : AppCompatActivity() {

    // firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Creating Intent To move Through Different Activities
        val LoginActivityScreen :Intent = Intent(this, LoginActivity::class.java)

        // View elements Binding
        val backButton = findViewById<ImageView>(R.id.BackBtn)
        val firstName :EditText = this.findViewById(R.id.FirstName)
        val lastName :EditText = this.findViewById(R.id.SirName)
        val email :EditText = this.findViewById(R.id.EmailAddress)
        val password :EditText = this.findViewById(R.id.NewPassword)
        val verifyPassword :EditText = this.findViewById(R.id.ConfirmPassword)
        val privRole :ImageView = this.findViewById(R.id.PrivRole)
        val role :TextView = this.findViewById(R.id.Role)
        val nextRole :ImageView = this.findViewById(R.id.NextRole)
        val privCourse :ImageView = this.findViewById(R.id.PrivCourse)
        val course :TextView = this.findViewById(R.id.Course)
        val nextCourse :ImageView = this.findViewById(R.id.NextCourse)
        val numberID :EditText = this.findViewById(R.id.IDNumber)
        val signUpButton :Button = this.findViewById(R.id.SignUpButton)

        var courseCodes = resources.getStringArray(R.array.department)
        val roleName = arrayListOf("teacher","student")

        var rolePointer = 1
        nextRole.alpha = 0.3f
        nextRole.isClickable = false
        var coursePointer = 1

        course.text = courseCodes[coursePointer]

        privRole.setOnClickListener {
            if (privRole.isClickable){
                rolePointer -= 1
                if (rolePointer == 0) {
                    rolePointer = 0
                    privRole.alpha = 0.3f
                    privRole.isClickable = false
                    rolePointer = 0
                    role.text = roleName[rolePointer]
                    nextRole.isClickable = true
                    nextRole.alpha = 1.0f

                } else {
                    privRole.alpha = 1.0f
                    privRole.isClickable = true
                    role.text = roleName[rolePointer]
                }
            }

        }

        nextRole.setOnClickListener {
            if (nextRole.isClickable){
                rolePointer += 1
                if (rolePointer == roleName.size -1) {
                    rolePointer = roleName.size -1
                    nextRole.alpha = 0.3f
                    nextRole.isClickable = false
                    role.text = roleName[rolePointer]
                    privRole.alpha = 1.0f
                    privRole.isClickable = true

                } else {
                    nextRole.alpha = 1.0f
                    nextRole.isClickable = true
                    role.text = roleName[rolePointer]
                }
            }

        }

        privCourse.setOnClickListener {
            coursePointer -= 1
            if(coursePointer == 0){
                it.alpha = 0.3f
                it.isClickable = false
                course.text = courseCodes[coursePointer]
                nextCourse.alpha = 1.0f
                nextCourse.isClickable = true

            } else {
                it.alpha = 1.0f
                it.isClickable = true
                course.text = courseCodes[coursePointer]
            }
        }

        nextCourse.setOnClickListener {
            coursePointer += 1
            if (coursePointer == courseCodes.size -1) {
                coursePointer = courseCodes.size -1
                it.alpha = 0.3f
                it.isClickable = false
                course.text = courseCodes[coursePointer]
                privCourse.alpha = 1.0f
                privCourse.isClickable = true

            } else {
                it.alpha = 1.0f
                it.isClickable = true
                course.text = courseCodes[coursePointer]
            }

        }

        signUpButton.setOnClickListener{

            if (firstName.text.toString().isBlank()) {
                firstName.error = "Field cannot be empty!"
                if (lastName.text.toString().isBlank()) {
                    lastName.error = "Field cannot be empty!"
                    if (email.text.toString().isBlank()) {
                        email.error = "Field cannot be empty!"
                        if (password.text.toString().isBlank()) {
                            password.error = "Field cannot be empty!"
                            if (verifyPassword.text.toString().isBlank()) {
                                verifyPassword.error = "Field cannot be empty!"
                                if (numberID.text.toString().isBlank()) {
                                    numberID.error = "Field cannot be empty!"
                                }
                            }
                        }
                    }
                }

            } else if (password.text.toString() == verifyPassword.text.toString()){
                VerifyAuth( this,
                    email.text.toString(),
                    password.text.toString(),
                    firstName.text.toString(),
                    lastName.text.toString(),
                    role.text.toString(),
                    course.text.toString(),
                    numberID.text.toString().toInt()
                )
            } else {
                verifyPassword.error = "Password does not match!"
            }

        }

        backButton.setOnClickListener {
            finish()
            startActivity(LoginActivityScreen)
        }
    }
    private fun VerifyAuth(context: Context ,email: String, password: String,
                           firstName: String, lastName: String ,
                           role: String, course: String, id: Int) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Invalid Email Address",Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password)&&TextUtils.isEmpty(firstName)&&TextUtils.isEmpty(lastName)&&TextUtils.isEmpty(id.toString()) ){
            Toast.makeText(this,"Password is Incorrect",Toast.LENGTH_SHORT).show()
        } else {


            if (userExists(email)){

                Toast.makeText(this,"User already exists",Toast.LENGTH_SHORT).show()
            }else {
                fireBaseLogin(context, firstName, lastName, email, password, role, course, id)
            }
        }


    }

    private fun fireBaseLogin(context: Context, firstName: String, lastName: String, email: String, password: String, role: String, course: String, id: Int) {
        firebaseAuth = FirebaseAuth.getInstance()
        val showProgress: ProgressDialog = ProgressDialog(this)
        showProgress.setTitle("Please wait")
        showProgress.setMessage("Signing in...")
        showProgress.setCancelable(false)
        showProgress.show()
        firebaseDatabase = FirebaseDatabase.getInstance("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")
        val Listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if data already exists in the database

                if (dataSnapshot.hasChild(course[0]+id.toString()) || dataSnapshot.hasChild(email)){
                    Toast.makeText( context, "User Already Exists", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        val ud = email.split("@")
        val uid =ud[0]

        databaseReference.child("users").addValueEventListener(Listener)
        databaseReference.child("users").child(uid).child("firstname").setValue(firstName)
        databaseReference.child("users").child(uid).child("lastname").setValue(lastName)
        databaseReference.child("users").child(uid).child("email").setValue(email)
        databaseReference.child("users").child(uid).child("nid").setValue(id)
        databaseReference.child("users").child(uid).child("course").setValue(course)
        databaseReference.child("users").child(uid).child("role").setValue(role)
        if (role.lowercase() == "student") {
            databaseReference.child("users").child(uid).child("uid")
                .setValue(course+id.toString())
        } else {
            databaseReference.child("users").child(uid).child("uid")
                .setValue(role[0].uppercase() + id.toString())
        }
        firebaseAuth.createUserWithEmailAndPassword(email,password)
            .addOnSuccessListener {
                showProgress.dismiss()
                val firebaseUser = firebaseAuth.currentUser
                val userEmail = firebaseUser!!.email
                Toast.makeText(this,"Signed in Successfully as $userEmail",Toast.LENGTH_SHORT).show()
                val lg = Intent(this, LoginActivity::class.java)
                lg.putExtra("ROLE",role)
                startActivity(lg)

            }
            .addOnFailureListener {
                showProgress.dismiss()
                Toast.makeText(this,"Failed to Sign in.",Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }
    private fun userExists(email: String): Boolean{
        firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        firebaseDatabase = FirebaseDatabase.getInstance("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://signup-attendable-default-rtdb.europe-west1.firebasedatabase.app/")

        val ud = email.split("@")
        val uid = ud[0]
        var flag: Boolean = true
        databaseReference.child("users").child(uid.toString()).get().addOnSuccessListener {
            flag = if (it.exists()){
                val key = it.key
                key == uid

            }else{
                false
            }
        } .addOnFailureListener {
            // task fo failure
            flag = true
        }

        return flag
    }
}