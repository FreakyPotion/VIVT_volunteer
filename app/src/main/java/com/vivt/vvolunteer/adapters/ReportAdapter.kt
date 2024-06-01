import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.tables.ReportTable

class ReportAdapter(var reports: List<ReportTable>, var context: Context) : RecyclerView.Adapter<ReportAdapter.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.addImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.preview_upload_report, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = reports.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val file = reports[position].imageURL
        Picasso.get().load(file).into(holder.image)

        // Обработчик клика для выбора фотографии
        holder.image.setOnClickListener {
            openImageInGallery(Uri.parse(file))
        }
    }

    private fun openImageInGallery(imageUri: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(imageUri, "image/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }



}
