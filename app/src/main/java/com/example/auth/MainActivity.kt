package com.example.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.auth.ui.theme.AuthTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AuthTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "register") {
          composable("register") { RegistrationScreen(navController) }
          composable("userList") { UserListScreen() }
        }
      }
    }
  }
}

@Composable
fun RegistrationScreen(navController: NavHostController) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = "Create Account!", style = MaterialTheme.typography.headlineLarge)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email Address") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it },
      label = { Text("Password") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = {
        isLoading = true
        message = ""
        registerUser(email, password) { success, responseMessage ->
          isLoading = false
          if (success) {
            navController.navigate("userList")
          } else {
            message = responseMessage
          }
        }
      },
      enabled = !isLoading,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Register")
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (isLoading) {
      CircularProgressIndicator()
    }
    Text(text = message)
  }
}

fun registerUser(email: String, password: String, callback: (Boolean, String) -> Unit) {
  val retrofit = Retrofit.Builder()
    .baseUrl("https://reqres.in/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val service = retrofit.create(ApiService::class.java)
  val request = RegisterRequest(email, password)

  service.registerUser(request).enqueue(object : Callback<RegisterResponse> {
    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
      if (response.isSuccessful) {
        val responseBody = response.body()
        if (responseBody != null) {
          callback(true, "Registration Successful: Token - ${responseBody.token}")
        } else {
          callback(false, "Unknown error occurred")
        }
      } else {
        callback(false, "Registration Failed")
      }
    }

    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
      callback(false, "Network Error: ${t.message}")
    }
  })
}

data class RegisterRequest(val email: String, val password: String)
data class RegisterResponse(val id: Int, val token: String)
data class User(val id: Int, val email: String, val first_name: String, val last_name: String, val avatar: String)

interface ApiService {
  @POST("register")
  fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

  @GET("users")
  fun getUsers(): Call<UserListResponse>
}

data class UserListResponse(val data: List<User>)

@Composable
fun UserListScreen() {
  var users by remember { mutableStateOf(listOf<User>()) }
  var isLoading by remember { mutableStateOf(true) }
  var message by remember { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
    } else {
      if (message.isNotEmpty()) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
      } else {
        users.forEach { user ->
          UserItem(user)
        }
      }
    }
  }

  // Fetch users
  val retrofit = Retrofit.Builder()
    .baseUrl("https://reqres.in/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
  val service = retrofit.create(ApiService::class.java)

  service.getUsers().enqueue(object : Callback<UserListResponse> {
    override fun onResponse(call: Call<UserListResponse>, response: Response<UserListResponse>) {
      if (response.isSuccessful) {
        val userListResponse = response.body()
        if (userListResponse != null) {
          users = userListResponse.data
          message = ""
        } else {
          message = "Failed to load users"
        }
      } else {
        message = "Failed to load users"
      }
      isLoading = false
    }

    override fun onFailure(call: Call<UserListResponse>, t: Throwable) {
      message = "Network Error: ${t.message}"
      isLoading = false
    }
  })
}

@Composable
fun UserItem(user: User) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
    Image(
      painter = rememberImagePainter(data = user.avatar),
      contentDescription = null,
      modifier = Modifier.height(40.dp)
        .clip(CircleShape)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column {
      Text(text = "${user.first_name} ${user.last_name}", style = MaterialTheme.typography.bodyLarge)
      Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
