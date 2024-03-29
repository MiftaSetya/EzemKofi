package com.example.ezemkofi

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.ezemkofi.databinding.ActivitySearchBinding
import com.example.ezemkofi.databinding.ListCoffeeBinding
import com.example.ezemkofi.databinding.ListTopPickBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Search : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        class SearchViewHolder(val binding: ListTopPickBinding) : RecyclerView.ViewHolder(binding.root)
        class SearchAdapter(val search: JSONArray) : RecyclerView.Adapter<SearchViewHolder>(){

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
                var binding = ListTopPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return SearchViewHolder(binding)
            }

            override fun getItemCount(): Int = search.length()

            override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
                var item = search.getJSONObject(position)

                holder.binding.coffeeName.text = item.getString("name")
                holder.binding.rating.text = item.getDouble("rating").toString()
                holder.binding.category.text = item.getString("category")
                holder.binding.price.text = "$${item.getDouble("price")}"

                GlobalScope.launch(Dispatchers.IO) {
                    val imagePath = URL("http://10.0.2.2:5000/images/${item.getString("imagePath")}").openConnection() as HttpURLConnection

                    val inputstream = imagePath.inputStream
                    val imagebitmap = BitmapFactory.decodeStream(inputstream)

                    runOnUiThread {
                        holder.binding.image.setImageBitmap(imagebitmap)
                    }
                }

                holder.itemView.setOnClickListener {
                    val intent = Intent(this@Search, Detail::class.java).apply {
                        putExtra("Id", item.getInt("id"))
                        putExtra("price", item.getDouble("price"))
                    }
                    startActivity(intent)
                }
            }
        }

        // Start
        super.onCreate(savedInstanceState)
        var binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        binding.searchBar.addTextChangedListener { text ->
            GlobalScope.launch(Dispatchers.IO) {
                val con = URL("http://10.0.2.2:5000/api/coffee?search=$text").openStream().bufferedReader().readText()
                var hasil = JSONArray(con)
                runOnUiThread {
                    binding.searchRv.adapter = SearchAdapter(hasil)
                    binding.searchRv.layoutManager = LinearLayoutManager(this@Search)
                }
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            var con = URL("http://10.0.2.2:5000/api/coffee").openConnection() as HttpURLConnection
            con.setRequestProperty("Authorization", "Bearer ${Session.token}")
            var coffee = JSONArray(con.inputStream.bufferedReader().readText())

            GlobalScope.launch(Dispatchers.Main) {
                binding.searchRv.adapter = SearchAdapter(coffee)
                binding.searchRv.layoutManager = LinearLayoutManager(this@Search)
            }
        }
    }
}

