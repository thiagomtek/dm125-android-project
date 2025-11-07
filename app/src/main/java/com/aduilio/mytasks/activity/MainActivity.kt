package com.aduilio.mytasks.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.aduilio.mytasks.R
import com.aduilio.mytasks.adapter.ListAdapter
import com.aduilio.mytasks.adapter.TouchCallback
import com.aduilio.mytasks.databinding.ActivityMainBinding
import com.aduilio.mytasks.entity.Task
import com.aduilio.mytasks.listener.ClickListener
import com.aduilio.mytasks.listener.SwipeListener
import com.aduilio.mytasks.service.TaskService
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ListAdapter
    private lateinit var auth: FirebaseAuth

    private val taskService: TaskService by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        askNotificationPermission()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("first_run", true)) {
            AlertDialog.Builder(this)
                .setMessage("Aqui vc vai criar suas tarefas.")
                .setNeutralButton(android.R.string.ok, null)
                .create()
                .show()

            preferences.edit { putBoolean("first_run", false) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            getTasks()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.preferences -> {
                startActivity(Intent(this, PreferenceActivity::class.java))
            }
            R.id.logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initComponents() {
        binding.tvMessage.visibility = View.INVISIBLE

        adapter = ListAdapter(this, binding.tvMessage, object : ClickListener {
            override fun onClick(task: Task) {
                val intent = Intent(this@MainActivity, FormActivity::class.java)
                intent.putExtra("TASK_ID", task.id)
                startActivity(intent)
            }

            override fun onComplete(id: Long) {
                taskService.complete(id).observe(this@MainActivity) { response ->
                    if (!response.error) {
                        getTasks()
                    }
                }
            }
        })
        binding.rvMain.adapter = adapter

        binding.fabNew.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }

        ItemTouchHelper(TouchCallback(object : SwipeListener {
            override fun onSwipe(position: Int) {
                val task = adapter.getItem(position)
                val taskId = task.id ?: return

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.delete_confirmation_title)
                    .setMessage(getString(R.string.delete_confirmation_message, task.title))
                    .setPositiveButton(R.string.delete_confirmation_yes) { dialog, _ ->
                        taskService.delete(taskId).observe(this@MainActivity) { response ->
                            if (response.error) {
                                adapter.notifyItemChanged(position)
                            } else {
                                adapter.removeItem(position)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        adapter.notifyItemChanged(position)
                        dialog.dismiss()
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        })).attachToRecyclerView(binding.rvMain)

        binding.srlMain.setOnRefreshListener {
            getTasks()
        }
    }

    private fun getTasks() {
        taskService.list().observe(this) { response ->
            binding.srlMain.isRefreshing = false

            if (response.error) {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = ContextCompat.getString(this, R.string.server_error)
            } else {
                response.value?.let {
                    adapter.setData(it)
                } ?: run {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvMessage.text = ContextCompat.getString(this, R.string.empty_list)
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission)
                    .setMessage(R.string.notification_permission_rationale)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { dialog, which -> null }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show()
            }
        }
    }
}