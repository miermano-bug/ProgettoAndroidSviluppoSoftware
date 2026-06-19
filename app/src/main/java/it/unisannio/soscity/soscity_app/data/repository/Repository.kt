package it.unisannio.soscity.soscity_app.data.repository

import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User

interface Repository {

    // AUTH

    suspend fun login(
        username: String,
        password: String
    ): Result<User>

    suspend fun register(
        request: RegisterRequest
    ): Result<User>

    // TICKETS

    suspend fun createTicket(
        ticket: Ticket
    ): Result<Ticket>

    suspend fun getCitizenTickets(
        citizenId: String
    ): Result<List<Ticket>>

    // NOTIFICATIONS

    suspend fun getNotifications(
        userId: String
    ): Result<List<Notification>>

    // INTERVENTIONS

    suspend fun getTechnicianInterventions(
        technicianId: String
    ): Result<List<Intervention>>

    suspend fun updateInterventionStatus(
        interventionId: String,
        status: String
    ): Result<Unit>
}