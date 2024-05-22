package com.vivt.vvolunteer.DBFactory


import com.vivt.vvolunteer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class DBConnector {

        suspend fun getConnection(): Connection? {
                return withContext(Dispatchers.IO) {
                        var connection: Connection? = null
                        try {
                                Class.forName("org.postgresql.Driver")
                                val url = BuildConfig.URL
                                val username = BuildConfig.User
                                val password = BuildConfig.Password
                                connection = DriverManager.getConnection(url, username, password)
                        } catch (e: ClassNotFoundException) {
                                e.printStackTrace()
                        } catch (e: SQLException) {
                                e.printStackTrace()
                        }
                        connection
                }
        }
}