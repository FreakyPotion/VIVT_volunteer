package com.vivt.vvolunteer.adapters

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.profile.ProfileOtherUser
import com.vivt.vvolunteer.tables.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class UsersAdapter(var users: List<UsersTable>, var context: Context, private val eventid: String?,  var requestOrNo: Boolean = false): RecyclerView.Adapter<UsersAdapter.MyViewHolder>() {
    class MyViewHolder(view: View, requestOrNo: Boolean): RecyclerView.ViewHolder(view) {

        val image: ImageView?
        val name: TextView?
        val surname: TextView?
        val accept: Button?
        val decline: Button?


        init {
            if (requestOrNo) {
                image = view.findViewById(R.id.reqAvatar)
                name = view.findViewById(R.id.reqName)
                surname = view.findViewById(R.id.reqSurname)
                accept = view.findViewById(R.id.reqAccept)
                decline = view.findViewById(R.id.reqDecline)
            } else {
                image = view.findViewById(R.id.partAvatar)
                name = view.findViewById(R.id.partName)
                surname = view.findViewById(R.id.partSurname)
                accept = null
                decline = null

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val currentLayout = if (requestOrNo) R.layout.request_list_preview else R.layout.participants_list_preview
        val view = LayoutInflater.from(parent.context).inflate(currentLayout, parent,false)
        return MyViewHolder(view, requestOrNo)
    }

    override fun getItemCount(): Int {
        return users.count()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (requestOrNo) {
            holder.name?.text = users[position].name
            holder.surname?.text = users[position].surname
            Picasso.get().load(users[position].imageURL).into(holder.image)

            holder.image?.setOnClickListener {
                val intent = Intent(context, ProfileOtherUser::class.java)
                intent.putExtra("eventid", eventid)
                intent.putExtra("userid", users[position].id.toString())
                intent.putExtra("userName", users[position].name)
                intent.putExtra("userSurname", users[position].surname)
                intent.putExtra("userPatronymic",users[position].patronymic)
                intent.putExtra("userEmail", users[position].email)
                intent.putExtra("userPhone",users[position].phone)
                intent.putExtra("userCity", users[position].city)
                intent.putExtra("userImage",users[position].imageURL)
                intent.putExtra("userDate", users[position].birthday)
                intent.putExtra("userRate", users[position].rating)
                context.startActivity(intent)
            }

            holder.accept?.setOnClickListener {
                showConfirmationDialogAccept(DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            GlobalScope.launch {
                                DBCallChangeStatus(true, users[position].id)

                                withContext(Dispatchers.Main) {
                                    users = users.filterIndexed { index, _ -> index != position }
                                    notifyItemRemoved(position)
                                }
                            }

                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                })
            }
            holder.decline?.setOnClickListener {
                showConfirmationDialogDecline(DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            GlobalScope.launch {
                                DBCallChangeStatus(false, users[position].id)
                                withContext(Dispatchers.Main) {
                                    users = users.filterIndexed { index, _ -> index != position }
                                    notifyItemRemoved(position)
                                }
                            }

                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                })

            }
        } else {
            holder.name?.text = users[position].name
            holder.surname?.text = users[position].surname
            Picasso.get().load(users[position].imageURL).into(holder.image)

            holder.image?.setOnClickListener {
                val intent = Intent(context, ProfileOtherUser::class.java)
                intent.putExtra("eventid", eventid)
                intent.putExtra("userid", users[position].id.toString())
                intent.putExtra("userName", users[position].name)
                intent.putExtra("userSurname", users[position].surname)
                intent.putExtra("userPatronymic",users[position].patronymic)
                intent.putExtra("userEmail", users[position].email)
                intent.putExtra("userPhone",users[position].phone)
                intent.putExtra("userCity", users[position].city)
                intent.putExtra("userImage",users[position].imageURL)
                intent.putExtra("userDate", users[position].birthday)
                intent.putExtra("userRate", users[position].rating)
                context.startActivity(intent)
            }
        }

    }

    private fun showConfirmationDialogAccept(listener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Подтверждение")
            .setMessage("Принять заявку?")
            .setPositiveButton("Да", listener)
            .setNegativeButton("Отмена", listener)

        val dialog = builder.create()
        dialog.show()
    }
    private fun showConfirmationDialogDecline(listener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Подтверждение")
            .setMessage("Отклонить заявку?")
            .setPositiveButton("Да", listener)
            .setNegativeButton("Отмена", listener)

        val dialog = builder.create()
        dialog.show()
    }

    suspend fun DBCallChangeStatus(call: Boolean, id: Int) {
        coroutineScope {
            launch {
                var connect: Connection? = null
                try {
                    val connector = DBConnector()
                    connect = connector.getConnection()
                    if (connect != null) {
                        if (call) {
                            val changeSQL: PreparedStatement = connect.prepareStatement(
                                "UPDATE Запросы SET \"Status_ID\" = 0 WHERE \"User_ID\" = ? AND \"Event_ID\" = ?;"
                            )
                            changeSQL.setInt(1,id)
                            changeSQL.setInt(2, eventid!!.toInt())
                            changeSQL.executeUpdate()
                            changeSQL.close()
                        } else {
                            val changeSQL: PreparedStatement = connect.prepareStatement(
                                "UPDATE Запросы SET \"Status_ID\" = 2 WHERE \"User_ID\" = ? AND \"Event_ID\" = ?;"
                            )
                            changeSQL.setInt(1,id)
                            changeSQL.setInt(2, eventid!!.toInt())
                            changeSQL.executeUpdate()
                            changeSQL.close()
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                } finally {
                    // Закрытие соединения с базой данных
                    connect?.close()
                }
            }
        }
    }

}