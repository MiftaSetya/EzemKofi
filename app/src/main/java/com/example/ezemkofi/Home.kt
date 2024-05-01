package com.example.ezemkofi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ezemkofi.databinding.ActivityHomeBinding
import com.example.ezemkofi.databinding.ListCategoryBinding
import com.example.ezemkofi.databinding.ListCoffeeBinding
import com.example.ezemkofi.databinding.ListTopPickBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var selectedCategoryID: Int? = 4
    private lateinit var sharedPreferences : SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("JWT", Context.MODE_PRIVATE)
        var token = sharedPreferences.getString("token", null)

        binding.searchBar.setOnClickListener {
            startActivity(Intent(this, Search::class.java))
        }

        binding.cart.setOnClickListener {
            startActivity(Intent(this, Cart::class.java))
        }

        GlobalScope.launch(Dispatchers.IO) {
            var con = URL("${Session.url}/api/me").openConnection() as HttpURLConnection
            con.setRequestProperty("Authorization", "Bearer $token")

            var code = con.responseCode

            if (code == 200) {
                var user = JSONObject(con.inputStream.bufferedReader().readText())

                runOnUiThread {
                    binding.nameTv.text = user.getString("fullName")
                }
            }
        }

        class CategoryViewHolder(val binding: ListCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        GlobalScope.launch(Dispatchers.IO) {
            val con = URL("http://10.0.2.2:5000/api/coffee-category").openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Authorization", "Bearer $token")
            var category = JSONArray(con.inputStream.bufferedReader().readText())

            GlobalScope.launch(Dispatchers.Main) {
                var adapter = object : RecyclerView.Adapter<CategoryViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): CategoryViewHolder {
                        var binding = ListCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return CategoryViewHolder(binding)
                    }

                    override fun getItemCount(): Int = category.length()

                    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
                        val item = category.getJSONObject(position)

                        holder.binding.category.text = item.getString("name")

                        if (item.getInt("id") == selectedCategoryID) {
                            holder.binding.root.setBackgroundResource(R.drawable.bg_category_selected)
                            holder.binding.category.setTextColor(ContextCompat.getColor(this@Home, R.color.white))
                        } else {
                            holder.binding.root.setBackgroundResource(R.drawable.bg_category_unselected)
                            holder.binding.category.setTextColor(ContextCompat.getColor(this@Home, R.color.EzemGray))
                        }

                        holder.binding.root.setOnClickListener {
                            selectedCategoryID = item.getInt("id")
                            notifyDataSetChanged()
                            getCoffee(selectedCategoryID)
                        }
                    }
                }
                binding.categoryRv.adapter = adapter
                binding.categoryRv.layoutManager = LinearLayoutManager (this@Home, RecyclerView.HORIZONTAL, false)
            }
        }

        class TopPickViewHolder(val binding: ListTopPickBinding) : RecyclerView.ViewHolder(binding.root)

        GlobalScope.launch(Dispatchers.IO) {
            var con = URL("http://10.0.2.2:5000/api/coffee/top-picks").openConnection() as HttpURLConnection
            con.setRequestProperty("Authorization", "Bearer $token")
            var topPick = JSONArray(con.inputStream.bufferedReader().readText())

            GlobalScope.launch(Dispatchers.Main) {
                var adapter = object : RecyclerView.Adapter<TopPickViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): TopPickViewHolder {
                        val binding = ListTopPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return TopPickViewHolder(binding)
                    }

                    override fun getItemCount(): Int = topPick.length()

                    override fun onBindViewHolder(holder: TopPickViewHolder, position: Int) {
                        var item = topPick.getJSONObject(position)

                        holder.binding.coffeeName.text = item.getString("name")
                        holder.binding.rating.text = item.getDouble("rating").toString()
                        holder.binding.category.text = item.getString("category")
                        holder.binding.price.text = "$${item.getDouble("price")}"

                        GlobalScope.launch(Dispatchers.IO) {
                            val imagePath = URL("http://10.0.2.2:5000/images/${item.getString("imagePath")}").openConnection() as HttpURLConnection
                            imagePath.setRequestProperty("Authorization", "Bearer ${Session.token}")

                            val inputstream = imagePath.inputStream
                            val imagebitmap = BitmapFactory.decodeStream(inputstream)

                            runOnUiThread {
                                holder.binding.image.setImageBitmap(imagebitmap)
                            }
                        }

                        holder.itemView.setOnClickListener {
                            val intent = Intent(this@Home, Detail::class.java).apply {
                                putExtra("Id", item.getInt("id"))
                                putExtra("price", item.getDouble("price"))
                            }
                            startActivity(intent)
                        }
                    }
                }
                binding.topPickRv.adapter = adapter
                binding.topPickRv.layoutManager = LinearLayoutManager(this@Home)
            }
        }

        getCoffee(selectedCategoryID)
    }

    private fun getCoffee(Id: Int?) {
        class CoffeeViewHolder(val binding: ListCoffeeBinding) : RecyclerView.ViewHolder(binding.root)

        sharedPreferences = getSharedPreferences("JWT", Context.MODE_PRIVATE)
        var token = sharedPreferences.getString("token", null)

        GlobalScope.launch(Dispatchers.IO) {
            var con = URL("http://10.0.2.2:5000/api/coffee?coffeeCategoryID=${Id}").openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Authorization", "Bearer $token")
            var coffee = JSONArray(con.inputStream.bufferedReader().readText())

            GlobalScope.launch(Dispatchers.Main) {
                var adapter = object : RecyclerView.Adapter<CoffeeViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): CoffeeViewHolder {
                        val binding = ListCoffeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return CoffeeViewHolder(binding)
                    }

                    override fun getItemCount(): Int = coffee.length()

                    override fun onBindViewHolder(holder: CoffeeViewHolder, position: Int) {
                        var item = coffee.getJSONObject(position)

                        holder.binding.coffeeName.text = item.getString("name")
                        holder.binding.rating.text = item.getDouble("rating").toString()
                        holder.binding.price.text = item.getDouble("price").toString()

                        GlobalScope.launch(Dispatchers.IO) {
                            val imagePath = URL("http://10.0.2.2:5000/images/${item.getString("imagePath")}").openConnection() as HttpURLConnection
                            imagePath.setRequestProperty("Authorization", "Bearer ${Session.token}")

                            val inputstream = imagePath.inputStream
                            val imagebitmap = BitmapFactory.decodeStream(inputstream)

                            runOnUiThread {
                                holder.binding.image.setImageBitmap(imagebitmap)
                            }
                        }

                        holder.itemView.setOnClickListener {
                            val intent = Intent(this@Home, Detail::class.java).apply {
                                putExtra("Id", item.getInt("id"))
                                putExtra("price", item.getDouble("price"))
                            }
                            startActivity(intent)
                        }
                    }
                }
                binding.coffeeRv.adapter = adapter
                binding.coffeeRv.layoutManager = LinearLayoutManager(this@Home, RecyclerView.HORIZONTAL, false)
            }
        }
    }
}