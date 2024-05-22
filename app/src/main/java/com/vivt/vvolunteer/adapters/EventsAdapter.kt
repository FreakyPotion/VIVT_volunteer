package com.vivt.vvolunteer.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.event.EventDetailsActivity
import com.vivt.vvolunteer.tables.EventsTable

class EventsAdapter(var events: List<EventsTable>, var context: Context, var mainLayout: Boolean = false): RecyclerView.Adapter<EventsAdapter.MyViewHolder>() {
    class MyViewHolder(view: View, mainLayout: Boolean):RecyclerView.ViewHolder(view) {

        // 1 case
        val image: ImageView?
        val title: TextView?
        val date: TextView?
        val seeEvent: Button?
        val address: TextView?

        // 2 case
        val seeEvent2: TextView?
        val title2: TextView?
        val count: TextView?

        init {
            if (mainLayout) {
                // 1 case
                image = view.findViewById(R.id.eventPrevImage)
                title = view.findViewById(R.id.eventPrevTitle)
                date = view.findViewById(R.id.eventPrevDate)
                seeEvent = view.findViewById(R.id.seeEvent)
                address = view.findViewById(R.id.eventPrevAddr)

                seeEvent2 = null
                title2 = null
                count = null
            } else {
                // 2 case
                image = null
                title = null
                date = null
                seeEvent = null
                address = null

                seeEvent2 = view.findViewById(R.id.prevPButton)
                title2 = view.findViewById(R.id.prevPTitle)
                count = view.findViewById(R.id.prevPCount)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val currentLayout = if (mainLayout) R.layout.event_list_preview else R.layout.event_personal_list_preview
        val view = LayoutInflater.from(parent.context).inflate(currentLayout, parent,false)
        return MyViewHolder(view, mainLayout)
    }

    override fun getItemCount(): Int {
        return events.count()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (mainLayout) {
            holder.title?.text = events[position].title
            holder.address?.text = events[position].address
            holder.date?.text = events[position].date.toString()
            Picasso.get().load(events[position].imageURL).into(holder.image)

            holder.seeEvent?.setOnClickListener {
                val intent = Intent(context, EventDetailsActivity::class.java)
                intent.putExtra("eventid", events[position].id.toString())
                intent.putExtra("prevTitle", events[position].title)
                intent.putExtra("prevDate", events[position].date)
                intent.putExtra("prevAdress",events[position].address)
                intent.putExtra("prevOrg", events[position].organizer)
                intent.putExtra("prevMaxP",events[position].maxParticipants)
                intent.putExtra("prevDesc", events[position].description)
                intent.putExtra("prevImage",events[position].imageURL)
                context.startActivity(intent)
            }
        } else {
            holder.title2?.text = events[position].title
            holder.count?.text = (position + 1).toString()

            holder.seeEvent2?.setOnClickListener {
                val intent = Intent(context, EventDetailsActivity::class.java)
                intent.putExtra("eventid", events[position].id.toString())
                intent.putExtra("prevTitle", events[position].title)
                intent.putExtra("prevDate", events[position].date)
                intent.putExtra("prevAdress",events[position].address)
                intent.putExtra("prevOrg", events[position].organizer)
                intent.putExtra("prevMaxP",events[position].maxParticipants)
                intent.putExtra("prevDesc", events[position].description)
                intent.putExtra("prevImage",events[position].imageURL)
                context.startActivity(intent)
            }
        }
    }

}