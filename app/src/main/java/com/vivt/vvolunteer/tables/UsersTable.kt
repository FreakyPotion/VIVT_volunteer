package com.vivt.vvolunteer.tables

data class UsersTable(val id: Int, val name: String, val surname: String,
                 val patronymic: String, val email: String,
                 val phone: String, val imageURL: String, val rating: Int,
                 val birthday: String, val city: String)