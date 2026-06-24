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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.nio.charset.Charset

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
                            onRegisterClick = { name, email, pass, role, regNo, compName, ctx ->
                                registerUser(name, email, pass, role, regNo, compName, ctx) {
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
        } else {
            jsonBody.put("regNumber", JSONObject.NULL)
            jsonBody.put("reg_number", JSONObject.NULL)
            jsonBody.put("companyName", JSONObject.NULL)
            jsonBody.put("company_name", JSONObject.NULL)
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
                    error.networkResponse?.data?.let { String(it, Charsets.UTF_8) } ?: "No error body"
                } catch (e: Exception) {
                    "Could not read error body"
                }
                Log.e("REG_ERROR", "Status Code: $statusCode")
                Log.e("REG_ERROR", "Response Body: $errorBody")
                Toast.makeText(context, "Error $statusCode: $errorBody", Toast.LENGTH_LONG).show()
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
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SYSTEM LOGIN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text(text = "Modern IPT Management System", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    onLoginClick(email, password, context)
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(text = "LOGIN NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Don't have an account? Register here",
            color = Color(0xFF1565C0), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterClick: (String, String, String, String, String?, String?, Context) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("STUDENT") }
    var regNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Text(text = "CREATE ACCOUNT", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Text(text = "Join IPT Management System", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))

            Text(text = "Select Your Role:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
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
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Place of IPT / Company Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                        val regToSend = if (selectedRole == "STUDENT") regNumber else null
                        val compToSend = if (selectedRole == "STUDENT") companyName else null
                        onRegisterClick(fullName, email, password, selectedRole, regToSend, compToSend, context)
                    } else {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text(text = "REGISTER NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Already have an account? Login here", color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToLogin() })
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
            { Toast.makeText(context, "Failed to load logbook history.", Toast.LENGTH_SHORT).show() }
        )
        queue.add(jsonArrayRequest)
    }

    LaunchedEffect(Unit) { fetchLogbooks() }

    fun submitLogbook(description: String) {
        val url = "http://10.0.2.2:8085/api/logbook/submit"
        val queue = Volley.newRequestQueue(context)

        val jsonBody = JSONObject()
        jsonBody.put("studentId", studentId)
        jsonBody.put("student_id", studentId)
        jsonBody.put("activityDescription", description)
        jsonBody.put("activity_description", description)
        jsonBody.put("imageUrl", "")
        jsonBody.put("image_url", "")

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { _ ->
                Toast.makeText(context, "Logbook Submitted Successfully!", Toast.LENGTH_LONG).show()
                activityDescription = ""
                fetchLogbooks()
            },
            { Toast.makeText(context, "Failed to submit logbook.", Toast.LENGTH_SHORT).show() }
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
                    OutlinedTextField(value = activityDescription, onValueChange = { activityDescription = it }, label = { Text("Describe practical work...") }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp))
                    Button(onClick = { if (activityDescription.isNotEmpty()) submitLogbook(activityDescription) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("SUBMIT REPORT") }
                }
            }
        }
        items(logbookEntries) { entry ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Status: ${entry.optString("status")}", fontWeight = FontWeight.Bold)
                    Text(text = entry.optString("activity_description", entry.optString("activityDescription", "No Description")))
                    Text(text = "Grade: ${entry.optString("grade", "-")}", color = Color.Blue)
                    Text(text = "Feedback: ${entry.optString("feedback", "No comment")}", color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(user: JSONObject, onLogout: () -> Unit) {
    val context = LocalContext.current
    val allEntries = remember { mutableStateListOf<JSONObject>() }
    var selectedEntryId by remember { mutableLongStateOf(-1L) }
    var inputGrade by remember { mutableStateOf("") }
    var inputFeedback by remember { mutableStateOf("") }
    val supervisorName = user.optString("full_name", user.optString("fullName", "Supervisor"))

    fun fetchPending() {
        val url = "http://10.0.2.2:8085/api/logbook/pending"
        val queue = Volley.newRequestQueue(context)
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                allEntries.clear()
                for (i in 0 until response.length()) { allEntries.add(response.getJSONObject(i)) }
            },
            { Toast.makeText(context, "Error fetching data", Toast.LENGTH_SHORT).show() }
        )
        queue.add(jsonArrayRequest)
    }

    LaunchedEffect(Unit) { fetchPending() }

    fun reviewLogbook(id: Long, grade: String, feedback: String) {
        val url = "http://10.0.2.2:8085/api/logbook/review/$id"
        val queue = Volley.newRequestQueue(context)
        val body = JSONObject()
        body.put("status", "APPROVED")
        body.put("grade", grade)
        body.put("feedback", feedback)

        val req = JsonObjectRequest(Request.Method.PUT, url, body,
            { _ ->
                selectedEntryId = -1L
                fetchPending()
                Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show()
            },
            { Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show() }
        )
        queue.add(req)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SUPERVISOR: $supervisorName", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
        }
        if (selectedEntryId != -1L) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(value = inputGrade, onValueChange = { inputGrade = it }, label = { Text("Grade") })
                        OutlinedTextField(value = inputFeedback, onValueChange = { inputFeedback = it }, label = { Text("Feedback") })
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = { reviewLogbook(selectedEntryId, inputGrade, inputFeedback) }) { Text("Submit") }
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Button(onClick = { selectedEntryId = -1L }) { Text("Cancel") }
                        }
                    }
                }
            }
        }
        items(allEntries) { entry ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedEntryId = entry.optLong("id") }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Logbook ID: #${entry.optLong("id")}")
                    Text(text = entry.optString("activity_description", entry.optString("activityDescription", "No Description")))
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(user: JSONObject, onLogout: () -> Unit) {
    val context = LocalContext.current
    var totalStudents by remember { mutableLongStateOf(0L) }
    var totalSupervisors by remember { mutableLongStateOf(0L) }
    val userList = remember { mutableStateListOf<JSONObject>() }
    val adminName = user.optString("full_name", user.optString("fullName", "Admin"))

    // Function ya kuvuta Stats na Watumiaji wote
    fun loadAdminData() {
        val queue = Volley.newRequestQueue(context)

        // 1. Stats
        val statsUrl = "http://10.0.2.2:8085/api/admin/stats"
        val statsReq = JsonObjectRequest(Request.Method.GET, statsUrl, null,
            { res ->
                totalStudents = res.optLong("total_students", res.optLong("totalStudents", 0L))
                totalSupervisors = res.optLong("total_supervisors", res.optLong("totalSupervisors", 0L))
            },
            { Log.e("ADMIN_ERR", "Failed stats") }
        )
        queue.add(statsReq)

        // 2. Orodha ya watumiaji wote (Kutoka kwenye meza ya Users ya Spring Boot)
        val usersUrl = "http://10.0.2.2:8085/api/admin/users"
        val usersReq = JsonArrayRequest(Request.Method.GET, usersUrl, null,
            { response ->
                userList.clear()
                for (i in 0 until response.length()) {
                    userList.add(response.getJSONObject(i))
                }
            },
            { Log.e("ADMIN_ERR", "Failed to fetch users list") }
        )
        queue.add(usersReq)
    }

    LaunchedEffect(Unit) { loadAdminData() }

    // NJIA MPYA: Function ya kufuta mtumiaji (DELETE request ya Spring Boot)
    fun deleteUser(userId: Long) {
        val url = "http://10.0.2.2:8085/api/admin/users/$userId"
        val queue = Volley.newRequestQueue(context)

        val deleteReq = JsonObjectRequest(Request.Method.DELETE, url, null,
            { _ ->
                Toast.makeText(context, "User Deleted Successfully!", Toast.LENGTH_SHORT).show()
                loadAdminData() // Inapakia upya data baada ya kufuta
            },
            { error ->
                // Kama endpoint yako ya Spring Boot inarudisha text tupu au 200 bila body, Volley inaweza kuingia hapa.
                if (error.networkResponse?.statusCode == 200) {
                    Toast.makeText(context, "User Deleted!", Toast.LENGTH_SHORT).show()
                    loadAdminData()
                } else {
                    Toast.makeText(context, "Failed to delete user.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        queue.add(deleteReq)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ADMIN: $adminName", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout") }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Total Students: $totalStudents", fontSize = 15.sp)
                    Text("Total Supervisors: $totalSupervisors", fontSize = 15.sp)
                }
            }
        }

        item {
            Text(
                text = "Manage All Registered Users",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = Color.DarkGray
            )
        }

        // Kitanzi (Loop) ya kuonyesha kila mtumiaji na kitufe cha kudelete
        items(userList) { sysUser ->
            val id = sysUser.optLong("id")
            val name = sysUser.optString("full_name", sysUser.optString("fullName", "No Name"))
            val uEmail = sysUser.optString("email", "No Email")
            val uRole = sysUser.optString("role", "STUDENT")

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Email: $uEmail", fontSize = 13.sp, color = Color.Gray)
                        Text(text = "Role: $uRole", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                    }

                    // Kitufe cha kufuta mtumiaji
                    Button(
                        onClick = { deleteUser(id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = "DELETE", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}