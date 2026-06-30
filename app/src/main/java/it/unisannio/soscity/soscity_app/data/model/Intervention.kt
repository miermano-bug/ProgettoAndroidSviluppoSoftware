package it.unisannio.soscity.soscity_app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Il backend (intervention-service) usa nomi di campo in INGLESE, non in italiano:
 * technicianId, workStatus, interventionNotes, startDate, endDate, createdAt
 * (vedi soscity_api_contract_v4_FINAL.md, §9). I nomi italiani precedenti
 * (tecnicoId, statoLavoro, noteIntervento, dataInizio, dataFine, dataCreazione)
 * non corrispondevano a nulla nella risposta reale: i campi sarebbero rimasti
 * sempre vuoti/null. Ho mantenuto i nomi delle property Kotlin in italiano per
 * non rompere il resto del codice che le referenzia, e ho aggiunto @SerializedName
 * per far combaciare la (de)serializzazione Gson con il JSON reale del backend.
 */
data class Intervention(
    val id: String = "",
    val ticketId: String = "",
    val teamId: String = "",

    @SerializedName("technicianId")
    val tecnicoId: String = "",

    @SerializedName("workStatus")
    val statoLavoro: String = "",

    @SerializedName("interventionNotes")
    val noteIntervento: String = "",

    @SerializedName("startDate")
    val dataInizio: String = "",

    @SerializedName("endDate")
    val dataFine: String? = null,

    @SerializedName("createdAt")
    val dataCreazione: String = ""
)