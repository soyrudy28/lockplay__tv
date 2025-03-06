package com.touchin.lockplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CargaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carga)

        // Simular carga o esperar respuesta del servidor
        lifecycleScope.launch {
            delay(3000) // Simulación de espera
            finish() // Cierra la pantalla de carga y vuelve a la anterior
        }
    }
}
