package com.example.ezemkofi

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ezemkofi.databinding.ActivityCartBinding
import com.example.ezemkofi.databinding.ListCartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.log

class Cart : AppCompatActivity() {
    lateinit var binding: ActivityCartBinding
    lateinit var cartAdapter: CartAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("LOCAL", Context.MODE_PRIVATE)

        binding.back.setOnClickListener {
            finish()
        }

        binding.checkout.setOnClickListener {
            val sharedPreferences = getSharedPreferences("LOCAL", Context.MODE_PRIVATE)
            val cart = sharedPreferences.getString("cart", "[]")
            val cartJson = JSONArray(cart)

            GlobalScope.launch(Dispatchers.IO) {
                var con = URL("${Session.url}/api/checkout").openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Authorization", "Bearer ${Session.token}")
                con.setRequestProperty("Content-Type", "application/json")

                var cartData = JSONArray()

                for (i in 0 until cartJson.length()) {
                    val item = cartJson.getJSONObject(i)
                    val itemData = JSONObject()
                    itemData.put("coffeeId", item.getInt("Id"))
                    itemData.put("size", item.getString("size"))
                    itemData.put("qty", item.getInt("count"))
                    cartData.put(itemData)
                    Log.d("data", cartData.toString())
                }

                val outputStream = con.outputStream
                outputStream.write(cartData.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val code = con.responseCode

                if (code == 200) {
                    GlobalScope.launch(Dispatchers.Main) {
                        val editor = sharedPreferences.edit()
                        editor.clear().apply()
                        Toast.makeText(this@Cart, "Checkout successful", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else if (code == 400) {
                    var message = con.errorStream.bufferedReader().readText()
                    Log.d("message", message)
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@Cart, "${message}", Toast.LENGTH_LONG).show()
                    }
                } else if (code == 401) {
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@Cart, "Unauthorized", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val cart = sharedPreferences.getString("cart", "[]")
        val cartJson = JSONArray(cart)

        cartAdapter = CartAdapter(cartJson, sharedPreferences)
        binding.cartRV.adapter = cartAdapter
        binding.cartRV.layoutManager = LinearLayoutManager(this)
    }

    class CartAdapter(private val cartItems: JSONArray, private val sharedPreferences: SharedPreferences) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CartAdapter.CartViewHolder {
            val binding = ListCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CartViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CartAdapter.CartViewHolder, position: Int) {
            val item = cartItems.getJSONObject(position)

            holder.binding.coffeeName.text = item.getString("name")
            holder.binding.price.text = "$ ${String.format("%.2f", item.getDouble("totalPrice"))}"
            holder.binding.size.text = "Size : ${item.getString("size")}"
            holder.binding.category.text = item.getString("category")
            holder.binding.itemCount.text = item.getInt("count").toString()
            var pricePerItem = item.getDouble("price")

            GlobalScope.launch(Dispatchers.IO) {
                val image = item.getString("imagePath")
                val imagePath = URL("${Session.url}/images/${image}").openConnection() as HttpURLConnection
                imagePath.setRequestProperty("Authorization", "Bearer ${Session.token}")

                var inputStream = imagePath.inputStream
                val imageBitmap = BitmapFactory.decodeStream(inputStream)

                GlobalScope.launch(Dispatchers.Main) {
                    holder.binding.image.setImageBitmap(imageBitmap)
                }
            }

            var itemCount = item.getInt("count")
            holder.binding.plusBtn.setOnClickListener {
                itemCount++
                holder.binding.itemCount.setText(itemCount.toString())
                var totalPrice = itemCount * pricePerItem
                holder.binding.price.text = "$ ${String.format("%.2f", totalPrice)}"
            }

            holder.binding.minBtn.setOnClickListener {
                if(itemCount > 1) {
                    itemCount--
                    holder.binding.itemCount.text = itemCount.toString()
                    var totalPrice = itemCount * pricePerItem
                    holder.binding.price.text = "$ ${String.format("%.2f", totalPrice)}"
                }
                else
                {
                    cartItems.remove(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount)

                    val editor = sharedPreferences.edit()
                    editor.putString("cart", cartItems.toString())
                    editor.apply()
                }
            }
        }

        override fun getItemCount(): Int = cartItems.length()

        class CartViewHolder(val binding: ListCartBinding) : RecyclerView.ViewHolder(binding.root)
    }
}