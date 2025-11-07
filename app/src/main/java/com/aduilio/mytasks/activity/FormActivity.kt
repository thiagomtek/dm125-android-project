package com.aduilio.mytasks.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aduilio.mytasks.R
import com.aduilio.mytasks.databinding.ActivityFormBinding
import com.aduilio.mytasks.entity.Task
import com.aduilio.mytasks.extension.hasValue
import com.aduilio.mytasks.extension.value
import com.aduilio.mytasks.service.TaskService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException // Certifique-se que este import está aqui

class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding

    private val taskService: TaskService by viewModels()

    private var taskId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // --- LÓGICA DE CARREGAMENTO ALTERADA ---
        // Em vez de receber o objeto 'task', recebemos o 'TASK_ID'
        val receivedId = intent.getLongExtra("TASK_ID", -1L)

        if (receivedId != -1L) {
            // Se recebemos um ID, é o modo "Editar"
            // Armazenamos o ID para quando formos salvar
            taskId = receivedId
            // Chamamos o serviço para buscar os detalhes da tarefa
            loadTaskDetails(receivedId)
        }
        // Se receivedId == -1L, é o modo "Nova Tarefa".
        // O taskId continua nulo e os campos ficam em branco.
        // --- FIM DA ALTERAÇÃO ---

        initComponents()
    }

    /**
     * Nova função para buscar os detalhes da tarefa no servidor
     * e preencher os campos do formulário.
     */
    private fun loadTaskDetails(id: Long) {
        taskService.getById(id).observe(this) { response ->
            if (!response.error && response.value != null) {
                val task = response.value
                // Preenche os campos com os dados recebidos
                binding.etTitle.setText(task.title)
                binding.etDescription.setText(task.description)
                binding.etDate.setText(task.formatDate())
                binding.etTime.setText(task.formatTime())
            } else {
                // Se falhar, mostra um alerta e fecha a activity
                showAlert(R.string.load_task_error, true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initComponents() {
        binding.btSave.setOnClickListener {
            // --- VALIDAÇÃO DO REQUISITO 1 (JÁ FEITA) ---
            binding.layoutTitle.error = null
            binding.layoutDate.error = null
            binding.layoutTime.error = null

            if (!binding.etTitle.hasValue()) {
                binding.layoutTitle.error = getString(R.string.title_required)
                return@setOnClickListener
            }

            val dateStr = binding.etDate.value()
            if (binding.etDate.hasValue()) {
                try {
                    LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } catch (e: DateTimeParseException) {
                    binding.layoutDate.error = getString(R.string.date_format_error)
                    return@setOnClickListener
                }
            }

            val timeStr = binding.etTime.value()
            if (binding.etTime.hasValue()) {
                try {
                    LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: DateTimeParseException) {
                    binding.layoutTime.error = getString(R.string.time_format_error)
                    return@setOnClickListener
                }
            }
            // --- FIM DA VALIDAÇÃO ---

            // Se tudo for válido, continuamos
            val date = if (binding.etDate.hasValue()) {
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } else null

            val time = if (binding.etTime.hasValue()) {
                LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
            } else null

            val task = Task(
                id = taskId, // Esta variável é preenchida pelo 'loadTaskDetails' ou fica null
                title = binding.etTitle.value(),
                description = binding.etDescription.value(),
                date = date,
                time = time
            )

            // Lógica para Criar (taskId == null) ou Atualizar (taskId != null)
            if (taskId == null) {
                taskService.create(task).observe(this) { response ->
                    if (response.error) {
                        showAlert(R.string.create_error)
                    } else {
                        finish()
                    }
                }
            } else {
                taskService.update(task).observe(this) { response ->
                    if (response.error) {
                        showAlert(R.string.update_error)
                    } else {
                        finish()
                    }
                }
            }
        }
    }

    private fun showAlert(message: Int, finishActivity: Boolean = false) {
        val builder = AlertDialog.Builder(this)
            .setMessage(message)

        if (finishActivity) {
            builder.setNeutralButton(android.R.string.ok) { _, _ -> finish() }
        } else {
            builder.setNeutralButton(android.R.string.ok, null)
        }

        builder.create().show()
    }
}