package com.example.aemads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class DashboardViewModel : ViewModel() {

    private val supabaseUrl = "https://jmfawibrlwiebqdyhdsk.supabase.co"
    private val supabaseKey = "sb_publishable_260CCSS5agIPHgDAKh04JA_u3Mps_Ho"

    private val supabase = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Postgrest)
        install(Realtime)
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true
    }

    private val _dataList = MutableStateFlow<List<EnergyLog>>(emptyList())
    val dataList: StateFlow<List<EnergyLog>> = _dataList.asStateFlow()

    private val _alertState = MutableStateFlow(AlertState.NORMAL)
    val alertState = _alertState.asStateFlow()

    private val _reportList = MutableStateFlow<List<DamageReport>>(emptyList())
    val reportList: StateFlow<List<DamageReport>> = _reportList.asStateFlow()

    init {
        fetchInitialData()
        startRealtimeListener()
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                // Fetch 20 data terakhir
                val initialData = supabase.postgrest["energy_logs"]
                    .select {
                        order("id", Order.DESCENDING)
                        limit(20)
                    }.decodeList<EnergyLog>()
                
                _dataList.value = initialData.reversed()
                _dataList.value.lastOrNull()?.let { evaluateThreshold(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startRealtimeListener() {
        viewModelScope.launch {
            while(true) {
                try {
                    val latestData = supabase.postgrest["energy_logs"]
                        .select {
                            order("id", Order.DESCENDING)
                            limit(20)
                        }.decodeList<EnergyLog>()
                    
                    _dataList.value = latestData.reversed()
                    _dataList.value.lastOrNull()?.let { evaluateThreshold(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(2000) // Polling setiap 2 detik
            }
        }
    }

    private fun evaluateThreshold(data: EnergyLog) {
        when {
            data.power_kw > 150.0 -> {
                _alertState.value = AlertState.SPIKE
            }
            data.power_factor < 0.85 && data.thd_value > 10.0 -> {
                _alertState.value = AlertState.DRIFT
            }
            else -> {
                _alertState.value = AlertState.NORMAL
            }
        }
    }

    fun exportToCsv(): String {
        val currentData = _dataList.value
        if (currentData.isEmpty()) return "Tidak ada data untuk diekspor"

        val csvBuilder = java.lang.StringBuilder()
        csvBuilder.append("ID,Waktu,Lini Produksi,Power (kW),Cos Phi,OEE (%),THD (%)\n")
        
        currentData.forEach { data ->
            csvBuilder.append("${data.id},${data.created_at},${data.lini_name},${data.power_kw},${data.power_factor},${data.oee_score},${data.thd_value}\n")
        }
        
        return csvBuilder.toString()
    }

    fun addReport(report: DamageReport) {
        val currentList = _reportList.value.toMutableList()
        currentList.add(0, report) // Add to top
        _reportList.value = currentList
    }
}

enum class AlertState { NORMAL, SPIKE, DRIFT }

data class DamageReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val notes: String,
    val imageBitmap: android.graphics.Bitmap? = null
)
