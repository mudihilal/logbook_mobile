package com.example.iptlogbook

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF4F6F9)
            ) {
                var currentScreen by remember { mutableStateOf("LOGIN") }
                var loggedInUser by remember { mutableStateOf(JSONObject()) }

                when (currentScreen) {
                    "LOGIN" -> {
                        LoginScreen(
                            onLoginClick = { email: String, pass: String, ctx: Context ->
                                loginUser(email, pass, ctx) { userJson, role ->
                                    loggedInUser = userJson
                                    currentScreen = when (role) {
                                        "SUPERVISOR" -> "SUPERVISOR_DASHBOARD"
                                        "ADMIN" -> "ADMIN_DASHBOARD"
                                        else -> "STUDENT_DASHBOARD"
                                    }
                                }
                            },
                            onNavigateToRegister = { currentScreen = "REGISTER" }
                        )
                    }

                    "REGISTER" -> {
                        RegisterScreen(
                            onRegisterClick = { name, email, pass, role, regNo, compName, course, ctx ->
                                registerUser(name, email, pass, role, regNo, compName, course, ctx) {
                                    currentScreen = "LOGIN"
                                }
                            },
                            onNavigateToLogin = { currentScreen = "LOGIN" }
                        )
                    }

                    "STUDENT_DASHBOARD" -> {
                        StudentDashboardScreen(
                            user = loggedInUser,
                            onLogout = { currentScreen = "LOGIN" })
                    }

                    "SUPERVISOR_DASHBOARD" -> {
                        SupervisorDashboardScreen(
                            user = loggedInUser,
                            onLogout = { currentScreen = "LOGIN" })
                    }

                    "ADMIN_DASHBOARD" -> {
                        AdminDashboardScreen(
                            user = loggedInUser,
                            onLogout = { currentScreen = "LOGIN" })
                    }
                }
            }
        }
    }

    private fun loginUser(
        email: String,
        password: String,
        context: Context,
        onSuccess: (JSONObject, String) -> Unit
    ) {
        val url = "http://10.0.2.2:8085/api/auth/login"
        val queue = Volley.newRequestQueue(context)

        val jsonBody = JSONObject()
        jsonBody.put("email", email)
        jsonBody.put("password", password)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val fullName = response.optString("full_name", response.optString("fullName", "User"))
                val role = response.optString("role", "STUDENT")
                Toast.makeText(context, "Welcome $fullName ($role)", Toast.LENGTH_LONG).show()
                onSuccess(response, role)
            },
            { error ->
                Log.e("LOGIN_ERROR", "Status Code: ${error.networkResponse?.statusCode}")
                Toast.makeText(context, "Login Failed! Invalid credentials.", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun registerUser(
        name: String,
        email: String,
        password: String,
        role: String,
        regNo: String?,
        compName: String?,
        course: String?,
        context: Context,
        onComplete: () -> Unit
    ) {
        val url = "http://10.0.2.2:8085/api/auth/register"
        val queue = Volley.newRequestQueue(context)

        val jsonBody = JSONObject()
        jsonBody.put("fullName", name)
        jsonBody.put("full_name", name)
        jsonBody.put("email", email)
        jsonBody.put("password", password)
        jsonBody.put("role", role)

        if (role == "STUDENT") {
            jsonBody.put("regNumber", regNo)
            jsonBody.put("reg_number", regNo)
            jsonBody.put("companyName", compName)
            jsonBody.put("company_name", compName)
            jsonBody.put("course", course)
        } else {
            jsonBody.put("regNumber", JSONObject.NULL)
            jsonBody.put("reg_number", JSONObject.NULL)
            jsonBody.put("companyName", compName ?: JSONObject.NULL)
            jsonBody.put("company_name", compName ?: JSONObject.NULL)
            jsonBody.put("course", JSONObject.NULL)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { _ ->
                Toast.makeText(context, "Registration Successful! Please Login.", Toast.LENGTH_LONG).show()
                onComplete()
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val errorBody = try {
                    error.networkResponse?.data?.let { String(it, Charsets.UTF_8) } ?: "No details provided"
                } catch (_: Exception) {
                    "Error reading registration payload"
                }
                Log.e("REG_ERROR", "Status Code: $statusCode | Body: $errorBody")
                Toast.makeText(context, "Registration failed: $errorBody", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(jsonObjectRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginClick: (String, String, Context) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SYSTEM LOGIN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text(text = "Industrial Practical Training Management", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val buttonText = if (passwordVisible) "HIDE" else "SHOW"
                Text(
                    text = buttonText,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(end = 8.dp)
                )
            }
        )

        Button(
            onClick = {
                if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                    onLoginClick(email.trim(), password, context)
                } else {
                    Toast.makeText(context, "All fields are required!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(text = "LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Create an account instead",
            color = Color(0xFF1565C0), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterClick: (String, String, String, String, String?, String?, String?, Context) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("STUDENT") }
    var regNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Text(text = "REGISTRATION", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Text(text = "Create your account", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val buttonText = if (passwordVisible) "HIDE" else "SHOW"
                    Text(
                        text = buttonText,
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { passwordVisible = !passwordVisible }
                            .padding(end = 8.dp)
                    )
                }
            )

            Text(text = "Select Role:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = (selectedRole == "STUDENT"), onClick = { selectedRole = "STUDENT" })
                Text(text = "Student", modifier = Modifier.padding(end = 8.dp))
                RadioButton(selected = (selectedRole == "SUPERVISOR"), onClick = { selectedRole = "SUPERVISOR" })
                Text(text = "Supervisor", modifier = Modifier.padding(end = 8.dp))
                RadioButton(selected = (selectedRole == "ADMIN"), onClick = { selectedRole = "ADMIN" })
                Text(text = "Admin")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedRole == "STUDENT") {
                OutlinedTextField(value = regNumber, onValueChange = { regNumber = it }, label = { Text("Registration Number") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course of Study (e.g. Mechanics)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Place of IPT (Company Name)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            } else if (selectedRole == "SUPERVISOR") {
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Assigned Institution") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (fullName.trim().isNotEmpty() && email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                        val regToSend = if (selectedRole == "STUDENT") regNumber.trim() else null
                        val compToSend = if (selectedRole == "STUDENT" || selectedRole == "SUPERVISOR") companyName.trim() else null
                        val courseToSend = if (selectedRole == "STUDENT") course.trim() else null
                        onRegisterClick(fullName.trim(), email.trim(), password, selectedRole, regToSend, compToSend, courseToSend, context)
                    } else {
                        Toast.makeText(context, "Please fill out all required fields.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text(text = "REGISTER", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Already registered? Go to Login", color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToLogin() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(user: JSONObject, onLogout: () -> Unit) {
    var activityDescription by remember { mutableStateOf("") }
    val context = LocalContext.current

    val fullName = user.optString("full_name", user.optString("fullName", "Student"))
    val studentId = user.optLong("id", 0L)
    val logbookEntries = remember { mutableStateListOf<JSONObject>() }

    fun fetchLogbooks() {
        val url = "http://10.0.2.2:8085/api/logbook/student/$studentId"
        val queue = Volley.newRequestQueue(context)

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                logbookEntries.clear()
                for (i in 0 until response.length()) { logbookEntries.add(response.getJSONObject(i)) }
            },
            { Log.e("STUDENT_DASHBOARD", "Failed to fetch student logs.") }
        )
        queue.add(jsonArrayRequest)
    }

    LaunchedEffect(Unit) { fetchLogbooks() }

    fun submitLogbook(description: String) {
        val url = "http://10.0.2.2:8085/api/logbook/submit"
        val queue = Volley.newRequestQueue(context)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val jsonBody = JSONObject()
        jsonBody.put("studentId", studentId)
        jsonBody.put("student_id", studentId)
        jsonBody.put("activityDescription", description)
        jsonBody.put("activity_description", description)
        jsonBody.put("entryDate", currentDate)
        jsonBody.put("entry_date", currentDate)
        jsonBody.put("imageUrl", "")
        jsonBody.put("image_url", "")

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { _ ->
                Toast.makeText(context, "Report submitted successfully!", Toast.LENGTH_LONG).show()
                activityDescription = ""
                fetchLogbooks()
            },
            { Toast.makeText(context, "Submission failed.", Toast.LENGTH_SHORT).show() }
        )
        queue.add(jsonObjectRequest)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "STUDENT PORTAL", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text(text = "Welcome, $fullName", fontSize = 14.sp, color = Color.Gray)
                }
                Button(onClick = { onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout", color = Color.White) }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Submit Daily Activity Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = activityDescription, onValueChange = { activityDescription = it }, label = { Text("Describe today's work details...") }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp))
                    Button(onClick = { if (activityDescription.trim().isNotEmpty()) submitLogbook(activityDescription.trim()) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("SUBMIT") }
                }
            }
        }
        items(logbookEntries) { entry ->
            val date = entry.optString("entry_date", entry.optString("entryDate", "N/A"))
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Status: ${entry.optString("status")}", fontWeight = FontWeight.Bold)
                        Text(text = "Date: $date", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = entry.optString("activity_description", entry.optString("activityDescription", "No Details")))
                    Text(text = "Grade: ${entry.optString("grade", "-")}", color = Color.Blue)
                    Text(text = "Feedback: ${entry.optString("feedback", "No supervisor feedback yet")}", color = Color.Gray)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(user: JSONObject, onLogout: () -> Unit) {
    val context = LocalContext.current
    val allEntries = remember { mutableStateListOf<JSONObject>() }
    var selectedEntryId by remember { mutableLongStateOf(-1L) }
    var inputGrade by remember { mutableStateOf("") }
    var inputFeedback by remember { mutableStateOf("") }

    val supervisorName = user.optString("full_name", user.optString("fullName", "Supervisor"))
    val supervisorInstitution = user.optString("company_name", user.optString("companyName", "N/A"))

    fun fetchAllLogbooks() {
        val url = "http://10.0.2.2:8085/api/logbook/pending"
        val queue = Volley.newRequestQueue(context)
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                allEntries.clear()
                for (i in 0 until response.length()) { allEntries.add(response.getJSONObject(i)) }
            },
            { Log.e("SUPERVISOR", "Error loading logbooks list") }
        )
        queue.add(jsonArrayRequest)
    }

    LaunchedEffect(Unit) { fetchAllLogbooks() }

    fun reviewLogbook(id: Long, grade: String, feedback: String, status: String) {
        val url = "http://10.0.2.2:8085/api/logbook/review/$id"
        val queue = Volley.newRequestQueue(context)
        val body = JSONObject()
        body.put("status", status)
        body.put("grade", if (status == "APPROVED") grade.uppercase().trim() else "-")
        body.put("feedback", feedback.trim())

        val req = JsonObjectRequest(Request.Method.PUT, url, body,
            { _ ->
                selectedEntryId = -1L
                fetchAllLogbooks()
                Toast.makeText(context, "Review complete: $status", Toast.LENGTH_SHORT).show()
            },
            { Toast.makeText(context, "Failed to submit review.", Toast.LENGTH_SHORT).show() }
        )
        queue.add(req)
    }

    val filteredEntries = allEntries.filter { entry ->
        val studentComp = entry.optJSONObject("student")?.optString("companyName", entry.optJSONObject("student")?.optString("company_name", ""))
            ?: entry.optString("company_name", entry.optString("companyName", ""))

        studentComp.equals(supervisorInstitution, ignoreCase = true)
    }

    val uniqueStudentsCount = filteredEntries.map { entry ->
        entry.optJSONObject("student")?.optLong("id") ?: entry.optLong("student_id")
    }.distinct().size

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SUPERVISOR: $supervisorName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
            Text("Assigned Hub: $supervisorInstitution", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Assigned Students: $uniqueStudentsCount", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
        }
        if (selectedEntryId != -1L) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Review Daily Report", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(value = inputGrade, onValueChange = { inputGrade = it }, label = { Text("Assign Grade (A, B, C, D, F)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        OutlinedTextField(value = inputFeedback, onValueChange = { inputFeedback = it }, label = { Text("Remarks / Feedback") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(
                                onClick = { if (inputGrade.trim().isNotEmpty()) reviewLogbook(selectedEntryId, inputGrade, inputFeedback, "APPROVED") else Toast.makeText(context, "Provide a grade first!", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) { Text("APPROVE") }

                            Button(
                                onClick = { reviewLogbook(selectedEntryId, "", inputFeedback, "REJECTED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { Text("REJECT") }

                            Button(
                                onClick = { selectedEntryId = -1L },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }
        }
        if (filteredEntries.isEmpty()) {
            item {
                Text(
                    "No pending reports for $supervisorInstitution.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
        } else {
            items(filteredEntries) { entry ->
                val date = entry.optString("entry_date", entry.optString("entryDate", "N/A"))

                val studentName = entry.optJSONObject("student")?.optString("fullName", "Student")
                    ?: entry.optString("student_name", "Anonymous")
                val regNo = entry.optJSONObject("student")?.optString("regNumber", "N/A")
                    ?: entry.optString("reg_number", "N/A")
                val studentCourse = entry.optJSONObject("student")?.optString("course", "Not Specified")
                    ?: entry.optString("course", "Not Specified")
                val currentStatus = entry.optString("status", "PENDING")

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedEntryId = entry.optLong("id") },
                    colors = CardDefaults.cardColors(
                        containerColor = when(currentStatus) {
                            "APPROVED" -> Color(0xFFE8F5E9)
                            "REJECTED" -> Color(0xFFFFEBEE)
                            else -> Color.White
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "LOG ID: #${entry.optLong("id")} ($currentStatus)", fontWeight = FontWeight.Bold)
                            Text(text = date, fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Student Name: $studentName", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "Reg No: $regNo", fontSize = 13.sp, color = Color.DarkGray)
                        // Student's course highlighted clearly to assist the supervisor:
                        Text(text = "Course: $studentCourse", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = entry.optString("activity_description", entry.optString("activityDescription", "No details")), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun AdminDashboardScreen(user: JSONObject, onLogout: () -> Unit) {
    val context = LocalContext.current
    var totalStudents by remember { mutableLongStateOf(0L) }
    var totalSupervisors by remember { mutableLongStateOf(0L) }
    val userList = remember { mutableStateListOf<JSONObject>() }
    val adminName = user.optString("full_name", user.optString("fullName", "Admin"))
    val adminId = user.optLong("id", -1L)

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedStudentForLogs by remember { mutableStateOf<JSONObject?>(null) }
    val studentLogsList = remember { mutableStateListOf<JSONObject>() }

    fun loadAdminData() {
        val queue = Volley.newRequestQueue(context)

        val statsUrl = "http://10.0.2.2:8085/api/admin/stats"
        val statsReq = JsonObjectRequest(Request.Method.GET, statsUrl, null,
            { res ->
                totalStudents = res.optLong("total_students", res.optLong("totalStudents", 0L))
                totalSupervisors = res.optLong("total_supervisors", res.optLong("totalSupervisors", 0L))
            },
            { Log.e("ADMIN", "Could not fetch registration counters.") }
        )
        queue.add(statsReq)

        val usersUrl = "http://10.0.2.2:8085/api/admin/users"
        val usersReq = JsonArrayRequest(Request.Method.GET, usersUrl, null,
            { response ->
                userList.clear()
                for (i in 0 until response.length()) {
                    val u = response.getJSONObject(i)
                    val role = u.optString("role", "STUDENT")
                    val uId = u.optLong("id", -1L)

                    if (uId != adminId && role != "ADMIN") {
                        userList.add(u)
                    }
                }
            },
            { Log.e("ADMIN", "Error pulling complete user records.") }
        )
        queue.add(usersReq)
    }

    fun fetchStudentLogbooks(studentId: Long) {
        val url = "http://10.0.2.2:8085/api/logbook/student/$studentId"
        val queue = Volley.newRequestQueue(context)
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                studentLogsList.clear()
                for (i in 0 until response.length()) { studentLogsList.add(response.getJSONObject(i)) }
            },
            { Log.e("ADMIN", "No logs returned for student #$studentId") }
        )
        queue.add(jsonArrayRequest)
    }

    LaunchedEffect(Unit) { loadAdminData() }

    fun deleteUser(userId: Long) {
        val url = "http://10.0.2.2:8085/api/admin/users/$userId"
        val queue = Volley.newRequestQueue(context)

        val deleteReq = JsonObjectRequest(Request.Method.DELETE, url, null,
            { _ ->
                Toast.makeText(context, "Deleted Successfully!", Toast.LENGTH_SHORT).show()
                loadAdminData()
            },
            { error ->
                if (error.networkResponse?.statusCode == 200) {
                    Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show()
                    loadAdminData()
                } else {
                    Toast.makeText(context, "Failed to remove user account.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        queue.add(deleteReq)
    }

    val filteredUsers = userList.filter { sysUser ->
        val role = sysUser.optString("role", "STUDENT")
        if (selectedTab == 0) role == "STUDENT" else role == "SUPERVISOR"
    }

    // Advanced dashboard groupings:
    val studentsByCompany = userList.filter { it.optString("role") == "STUDENT" }
        .groupBy { it.optString("company_name", it.optString("companyName", "Other Locations")) }
        .mapValues { it.value.size }

    val studentsByCourse = userList.filter { it.optString("role") == "STUDENT" }
        .groupBy { it.optString("course", "Unspecified Course") }
        .mapValues { it.value.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ADMIN PANEL: $adminName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
        }

        if (selectedStudentForLogs != null) {
            val stName = selectedStudentForLogs!!.optString("full_name", selectedStudentForLogs!!.optString("fullName", "Student"))
            val stCompany = selectedStudentForLogs!!.optString("company_name", selectedStudentForLogs!!.optString("companyName", "N/A"))
            val stCourse = selectedStudentForLogs!!.optString("course", "N/A")

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("REPORTS: ${stName.uppercase()}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Button(onClick = { selectedStudentForLogs = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
                        }
                        Text("Course of Study: $stCourse", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Assigned Hub: $stCompany", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (studentLogsList.isEmpty()) {
                item { Text("No logbook entries on record.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
            } else {
                items(studentLogsList) { entry ->
                    val date = entry.optString("entry_date", entry.optString("entryDate", "N/A"))
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status: ${entry.optString("status", "PENDING")}", fontWeight = FontWeight.Bold)
                                Text("Date: $date", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Description: ${entry.optString("activity_description", entry.optString("activityDescription", ""))}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Assigned Grade: ${entry.optString("grade", "-")}", color = Color.Blue, fontWeight = FontWeight.Medium)
                            Text("Feedback Remarks: ${entry.optString("feedback", "No Comments Provided")}", color = Color.DarkGray)
                        }
                    }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("System Overview Statistics", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Text("Total Registered Students: $totalStudents", fontSize = 14.sp)
                        Text("Total Active Supervisors: $totalSupervisors", fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("MAPPING BY HUB (INSTITUTION):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1565C0))
                        studentsByCompany.forEach { (company, count) ->
                            Text("• $company: $count students", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("MAPPING BY FIELD OF STUDY:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                        studentsByCourse.forEach { (courseName, count) ->
                            Text("• $courseName: $count students", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("STUDENTS ($totalStudents)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("SUPERVISORS ($totalSupervisors)", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            items(filteredUsers) { sysUser ->
                val id = sysUser.optLong("id")
                val name = sysUser.optString("full_name", sysUser.optString("fullName", "No Name"))
                val uEmail = sysUser.optString("email", "No Email")
                val uRole = sysUser.optString("role", "STUDENT")
                val company = sysUser.optString("company_name", sysUser.optString("companyName", "-"))
                val field = sysUser.optString("course", "-")

                val extraInfo = if (uRole == "STUDENT") {
                    "Reg No: ${sysUser.optString("reg_number", sysUser.optString("regNumber", "-"))}\nCourse: $field\nCompany: $company"
                } else {
                    "Supervisor\nAssigned Hub: $company"
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Email: $uEmail", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = extraInfo, fontSize = 13.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Access Role: $uRole", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (uRole == "STUDENT") {
                                Button(
                                    onClick = {
                                        selectedStudentForLogs = sysUser
                                        fetchStudentLogbooks(id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("VIEW LOGS", color = Color(0xFF1565C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { deleteUser(id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2))
                            ) {
                                Text(text = "DELETE", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}