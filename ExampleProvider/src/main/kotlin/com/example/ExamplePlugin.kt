package com.example

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class RuCatalogPlugin : Plugin() {
    override fun load(context: Context) {
        val prefs = context.getSharedPreferences("ru_catalog", Context.MODE_PRIVATE)
        VkConfig.token = prefs.getString("vk_token", "") ?: ""

        registerMainAPI(RuCatalogProvider())

        // Шестерёнка в настройках плагина: окно для ввода токена VK
        openSettings = { ctx ->
            val input = EditText(ctx)
            input.setText(VkConfig.token)
            AlertDialog.Builder(ctx)
                .setTitle("Токен VK")
                .setView(input)
                .setPositiveButton("Сохранить") { _, _ ->
                    val t = input.text.toString().trim()
                    VkConfig.token = t
                    ctx.getSharedPreferences("ru_catalog", Context.MODE_PRIVATE)
                        .edit().putString("vk_token", t).apply()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}
