package com.example.ezemkofi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.ezemkofi.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.usernameEt.setText("mahdi")
        binding.passwordEt.setText("1234")


        binding.registerTv.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        binding.loginBtn.setOnClickListener {

            if (binding.usernameEt.text.isNullOrEmpty() || binding.passwordEt.text.isNullOrEmpty()) {
                Toast.makeText(this@Login, "Please fill all data", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            GlobalScope.launch(Dispatchers.IO) {
                var con = URL("http://10.0.2.2:5000/api/auth").openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json")

                var dataAuth = JSONObject().apply {
                    put("username", binding.usernameEt.text)
                    put("password", binding.passwordEt.text)
                }

                con.outputStream.write(dataAuth.toString().toByteArray())

                var code = con.responseCode

                if (code in 200 .. 209) {
                    var token = con.inputStream.bufferedReader().readText()
                    Session.token = token

                    runOnUiThread {
                        val intent = Intent(this@Login, Home::class.java)

                        startActivity(intent)
                        finish()
                    }
                }
                else {
                    var error = con.errorStream.bufferedReader().readText()

                    runOnUiThread {
                        Toast.makeText(this@Login, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}