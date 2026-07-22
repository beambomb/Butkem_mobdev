package com.example.aemads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DashboardViewModel : ViewModel() {

    private val supabaseUrl = "https://jmfawibrlwiebqdyhdsk.supabase.co"
    private val supabaseKey = "sb_publishable_260CCSS5agIPHgDAKh04JA_u3Mps_Ho"

    private val supabase = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Postgrest)
        install(Realtime)
        install(Auth)
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true
    }

    // AUTH STATE
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState = _signUpState.asStateFlow()

    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession = _currentUserSession.asStateFlow()

    private val _dataList = MutableStateFlow<List<EnergyLog>>(emptyList())
    val dataList: StateFlow<List<EnergyLog>> = _dataList.asStateFlow()

    private val _alertState = MutableStateFlow(AlertState.NORMAL)
    val alertState = _alertState.asStateFlow()

    private val _reportList = MutableStateFlow<List<DamageReport>>(emptyList())
    val reportList: StateFlow<List<DamageReport>> = _reportList.asStateFlow()

    private val _anomalyHistory = MutableStateFlow<List<AnomalyRecord>>(emptyList())
    val anomalyHistory: StateFlow<List<AnomalyRecord>> = _anomalyHistory.asStateFlow()

    init {
        fetchInitialData()
        startRealtimeListener()
    }

    fun login(emailInput: String, passwordInput: String, plant: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                supabase.auth.signInWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                
                val user = supabase.auth.currentUserOrNull()
                val metadataName = user?.userMetadata?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "Unknown User"
                
                val session = UserSession(emailInput, metadataName, plant)
                _currentUserSession.value = session
                _loginState.value = LoginState.Success(session)
            } catch (e: Exception) {
                e.printStackTrace()
                val cleanError = e.message?.substringBefore("URL:")?.trim() ?: "Login failed"
                _loginState.value = LoginState.Error("Error: $cleanError")
            }
        }
    }

    fun signUp(nameInput: String, emailInput: String, passwordInput: String) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading
            try {
                supabase.auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
                    data = buildJsonObject {
                        put("name", nameInput)
                    }
                }
                _signUpState.value = SignUpState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                val cleanError = e.message?.substringBefore("URL:")?.trim() ?: "Registration failed"
                _signUpState.value = SignUpState.Error("Error: $cleanError")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetSignUpState() {
        _signUpState.value = SignUpState.Idle
    }

    private fun fetchInitialData() {
        val currentPlant = _currentUserSession.value?.plant ?: "Shop 1 - Stamping & Press"
        viewModelScope.launch {
            try {
                val initialData = supabase.postgrest["energy_logs"]
                    .select {
                        filter {
                            eq("lini_name", currentPlant)
                        }
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
            try {
                val currentPlant = _currentUserSession.value?.plant ?: "Shop 1 - Stamping & Press"
                val channel = supabase.channel("public:energy_logs")
                
                val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "energy_logs"
                }

                launch {
                    changes.collect { action ->
                        val newLog = action.decodeRecord<EnergyLog>()
                        if (newLog.lini_name == currentPlant) {
                            val currentList = _dataList.value.toMutableList()
                            currentList.add(newLog)
                            if (currentList.size > 20) {
                                currentList.removeAt(0)
                            }
                            _dataList.value = currentList
                            evaluateThreshold(newLog)
                        }
                    }
                }

                supabase.realtime.connect()
                channel.subscribe()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun evaluateThreshold(data: EnergyLog) {
        val currentState = _alertState.value
        val newState = when {
            data.power_kw > 150.0 -> AlertState.SPIKE
            data.power_factor < 0.85 && data.thd_value > 10.0 -> AlertState.DRIFT
            else -> AlertState.NORMAL
        }
        
        _alertState.value = newState

        // Log anomaly if state transitioned to an anomaly
        if (newState != AlertState.NORMAL && currentState != newState) {
            logAnomaly(newState, data)
        }
    }

    private fun logAnomaly(type: AlertState, data: EnergyLog) {
        val newRecord = AnomalyRecord(
            type = if (type == AlertState.SPIKE) "Case 2 (Spike)" else "Case 3 (Drift)",
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            triggerValue = if (type == AlertState.SPIKE) "${data.power_kw} kW" else "Cos Phi ${data.power_factor}",
            liniName = data.lini_name
        )
        val currentList = _anomalyHistory.value.toMutableList()
        currentList.add(0, newRecord)
        _anomalyHistory.value = currentList
        
        // TODO: In production, save this to `anomaly_history` table in Supabase.
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
        currentList.add(0, report)
        _reportList.value = currentList
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val session: UserSession) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    data class Error(val message: String) : SignUpState()
}

data class UserSession(
    val email: String,
    val name: String,
    val plant: String
)

enum class AlertState { NORMAL, SPIKE, DRIFT }

data class AnomalyRecord(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val timestamp: String,
    val triggerValue: String,
    val liniName: String
)

data class DamageReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val notes: String,
    val imageBitmap: android.graphics.Bitmap? = null
)
