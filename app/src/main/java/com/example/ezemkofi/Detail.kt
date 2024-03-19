package com.example.ezemkofi

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputBinding
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.ezemkofi.databinding.ActivityDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Detail : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    private var selectedSize = "M"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val Id = intent.getIntExtra("Id", 0).toString()
        var itemCount = binding.itemCount.text.toString().toInt()

        binding.back.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        binding.sBtn.setOnClickListener {
            if(selectedSize == "S") {
                itemCount++
                binding.itemCount.setText(itemCount.toString())
            }
            else
            {
                animtor(0.85f)
                updatePrice(0.85f)
                selectedSize = "S"
            }
        }

        binding.mBtn.setOnClickListener {
            if(selectedSize == "M") {
                itemCount++
                binding.itemCount.setText(itemCount.toString())
            }
            else {
                animtor(1f)
                updatePrice(1f)
                selectedSize = "M"
            }
        }

        binding.lBtn.setOnClickListener {
            if(selectedSize == "L") {
                itemCount++
                binding.itemCount.setText(itemCount.toString())
            }
            else
            {
                animtor(1.15f)
                updatePrice(1.15f)
                selectedSize = "L"
            }
        }

        binding.minBtn.setOnClickListener {
            if(itemCount > 1) {
                itemCount--
                binding.itemCount.setText(itemCount.toString())
            }
            else {
                Toast.makeText(this, "Item count can't less than 0", Toast.LENGTH_LONG).show()
            }
        }

        binding.plusBtn.setOnClickListener {
            itemCount++
            binding.itemCount.setText(itemCount.toString())
        }

        GlobalScope.launch(Dispatchers.IO) {
            var con = URL("${Session.url}/api/coffee/${Id}").openConnection() as HttpURLConnection
            con.setRequestProperty("Authorization", "Bearer ${Session.token}")
            var detail = JSONObject(con.inputStream.bufferedReader().readText())

            runOnUiThread {
                binding.name.text = detail.getString("name")
                binding.desc.text = detail.getString("description")
                binding.price.text = "$ ${detail.getDouble("price")}"
                binding.rating.text = detail.getDouble("rating").toString()

                GlobalScope.launch(Dispatchers.IO) {
                    val image = detail.getString("imagePath")
                    val imagePath = URL("http://10.0.2.2:5000/images/${image}").openConnection() as HttpURLConnection
                    imagePath.setRequestProperty("Authorization", "Bearer ${Session.token}")

                    val inputstream = imagePath.inputStream
                    val imagebitmap = BitmapFactory.decodeStream(inputstream)

                    runOnUiThread {
                        binding.imageCoffe.setImageBitmap(imagebitmap)

                        binding.cartBtn.setOnClickListener {
                            var sharedPreferences = getSharedPreferences("LOCAL", Context.MODE_PRIVATE)
                            var editor = sharedPreferences.edit()

                            val coffeeId = Id
                            val name = detail.getString("name")
                            val category = detail.getString("category")
                            val imagePath = detail.getString("imagePath")
                            val size = selectedSize
                            val count = binding.itemCount.text.toString().toInt()
                            val pricePerItem = detail.getDouble("price")
                            val totalPrice = pricePerItem * count

                            val itemCart = JSONObject().apply {
                                put("Id", coffeeId)
                                put("name", name)
                                put("category", category)
                                put("size", size)
                                put("count", count)
                                put("price", pricePerItem)
                                put("totalPrice", totalPrice)
                                put("imagePath", imagePath)
                            }

                            val cartArray = JSONArray(sharedPreferences.getString("cart", "[]"))

                            cartArray.put(itemCart)

                            Log.d("cart", cartArray.toString())

                            editor.putString("cart", cartArray.toString())
                            editor.apply()

                            Toast.makeText(this@Detail, "Successful add to cart", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun animtor(scaleFactor : Float) {
        val image = binding.imageCoffe

        val scaleX = ObjectAnimator.ofFloat(image, "scaleX", scaleFactor)
        val scaleY = ObjectAnimator.ofFloat(image, "scaleY", scaleFactor)

        val rotate = ObjectAnimator.ofFloat(image, "rotation", 0f, 360f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotate)
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.duration = 500
        animatorSet.start()
    }

    private fun updatePrice(scaleFactor: Float) {
        val originalPrice = intent.getDoubleExtra("price", 0.0)
        val priceTV = binding.price

        val price = originalPrice * scaleFactor
        priceTV.text = "$ ${String.format("%.2f", price)}"
    }
}