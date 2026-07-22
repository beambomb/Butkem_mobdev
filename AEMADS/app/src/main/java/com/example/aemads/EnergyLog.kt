package com.example.aemads

import kotlinx.serialization.Serializable

@Serializable
data class EnergyLog(
    val id: String = "",
    val lini_name: String = "",
    val power_kw: Double = 0.0,
    val power_factor: Double = 0.0,
    val output_qty: Int = 0,
    val sec_val: Double = 0.0,
    val oee_score: Double = 0.0,
    val thd_value: Double = 0.0,
    val created_at: String = ""
)
