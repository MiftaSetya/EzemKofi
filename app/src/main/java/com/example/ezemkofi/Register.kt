package com.example.ezemkofi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.ezemkofi.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginTv.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        binding.registerBtn.setOnClickListener {

            if (binding.usernameEt.text.isNullOrEmpty() || binding.fullnameEt.text.isNullOrEmpty() || binding.emailEt.text.isNullOrEmpty() || binding.passwordEt.text.isNullOrEmpty() || binding.confirmpasswordEt.text.isNullOrEmpty()) {
                Toast.makeText(this@Register, "Please fill all data", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (binding.passwordEt.text.length < 4) {
                Toast.makeText(this@Register, "Password at least 4 character", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (binding.passwordEt.text.toString() != binding.confirmpasswordEt.text.toString()) {
                Toast.makeText(this@Register, "Confirmation password is not same with password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Log.d("pw", binding.passwordEt.text.toString())
            Log.d("cpw", binding.confirmpasswordEt.text.toString())

            GlobalScope.launch(Dispatchers.IO) {
                var con = URL("${Session.url}/api/register").openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json")

                var data = JSONObject().apply {
                    put("username", binding.usernameEt.text)
                    put("fullname", binding.fullnameEt.text)
                    put("email", binding.emailEt.text)
                    put("password", binding.passwordEt.text)
                }

                con.outputStream.write(data.toString().toByteArray())

                var code = con.responseCode

                if (code in 200 .. 209) {
                    var token = con.inputStream.bufferedReader().readText()
                    Session.token = token

                    runOnUiThread {
                        val intent = Intent(this@Register, Home::class.java)

                        startActivity(intent)
                        Toast.makeText(this@Register, "Token : ${Session.token}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                else {
                    val error = con.errorStream.bufferedReader().readText()

                    runOnUiThread {
                        Toast.makeText(this@Register, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}