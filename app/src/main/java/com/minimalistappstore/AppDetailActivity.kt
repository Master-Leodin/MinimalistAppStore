package com.minimalistappstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.minimalistappstore.databinding.ActivityAppDetailBinding
import com.minimalistappstore.databinding.DialogFullscreenImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var currentApp: App
    private lateinit var screenshotAdapter: ScreenshotAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        currentApp = intent.getParcelableExtra("APP_EXTRA")!!

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = currentApp.name
            setDisplayHomeAsUpEnabled(true)
        }

        setupUI()
        setupScreenshots()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupUI() {
        binding.detailIconImageView.load(currentApp.iconUrl)
        binding.detailNameTextView.text = currentApp.name
        binding.detailVersionTextView.text = getString(R.string.version_label, currentApp.version)
        binding.detailDescriptionTextView.text = currentApp.description

        binding.downloadButton.text = getString(R.string.download_button)
        binding.downloadButton.setOnClickListener {
            startDownload()
        }
    }

    private fun setupScreenshots() {
        // DEBUG: Verificar screenshots
        android.util.Log.d("AppDetail", "Screenshots do app: ${currentApp.screenshotUrls}")

        if (currentApp.screenshotUrls.isNotEmpty()) {
            binding.screenshotsTitleTextView.visibility = android.view.View.VISIBLE
            binding.screenshotsRecyclerView.visibility = android.view.View.VISIBLE

            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.screenshotsRecyclerView.layoutManager = layoutManager

            screenshotAdapter = ScreenshotAdapter(currentApp.screenshotUrls) { imageUrl, position ->
                android.util.Log.d("AppDetail", "Clicado na screenshot: $imageUrl na posição $position")
                showFullscreenImageDialog(currentApp.screenshotUrls, position)
            }
            binding.screenshotsRecyclerView.adapter = screenshotAdapter
        } else {
            binding.screenshotsTitleTextView.visibility = android.view.View.GONE
            binding.screenshotsRecyclerView.visibility = android.view.View.GONE
            android.util.Log.d("AppDetail", "Nenhuma screenshot disponível")
        }
    }

    private fun showFullscreenImageDialog(imageUrls: List<String>, startPosition: Int) {
        // Inflar o layout correto
        val dialogView = layoutInflater.inflate(R.layout.dialog_fullscreen_viewpager, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val viewPager = dialogView.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.fullscreenViewPager)
        val closeButton = dialogView.findViewById<android.widget.ImageButton>(R.id.closeButton)

        // DEBUG: Verificar se temos URLs válidas
        android.util.Log.d("FullscreenDialog", "URLs recebidas: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            android.util.Log.d("FullscreenDialog", "URL $index: $url")
        }

        // Configurar o ViewPager2
        val adapter = FullscreenImageAdapter(imageUrls)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)

        // Adicionar um PageCallback para debug
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                android.util.Log.d("FullscreenDialog", "Página selecionada: $position")
            }
        })

        // Botão de fechar
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Fechar ao tocar na imagem (mas não no botão)
        viewPager.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.apply {
            // Configurar a janela para fullscreen
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        }

        dialog.show()

        // DEBUG: Verificar se o adapter tem itens
        android.util.Log.d("FullscreenDialog", "Adapter item count: ${adapter.itemCount}")
    }

    private fun startDownload() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.downloadButton.isEnabled = false
        binding.downloadButton.text = getString(R.string.downloading_button)
        binding.progressBar.progress = 0 // Reset progress

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apkUrl = URL(currentApp.apkUrl)
                val connection = apkUrl.openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                val contentLength = connection.contentLength
                val inputStream = connection.getInputStream()

                val apkFile = File(cacheDir, "${currentApp.name}_v${currentApp.version}.apk")
                val outputStream = FileOutputStream(apkFile)

                var totalBytesRead = 0
                val buffer = ByteArray(8 * 1024) // 8KB buffer
                var bytesRead: Int

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Atualiza o progresso
                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                withContext(Dispatchers.Main) {
                                    binding.progressBar.progress = progress
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = getString(R.string.install_button)
                    triggerInstallation(apkFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = getString(R.string.download_button)
                    Toast.makeText(
                        this@AppDetailActivity,
                        "Erro no download: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun triggerInstallation(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Não foi possível iniciar a instalação.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}