package com.vivt.vvolunteer.tables

import java.util.Date

data class EventsTable(val id: Int, val title: String, val description: String,
                       val date: String, val address: String,
                       val organizer: String, val imageURL: String, val maxParticipants: String)