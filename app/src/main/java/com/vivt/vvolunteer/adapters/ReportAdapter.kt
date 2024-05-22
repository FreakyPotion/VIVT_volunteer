import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.tables.ReportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportAdapter(var reports: MutableList<ReportTable>, var context: Context, private val UploadImage: () -> Unit) : RecyclerView.Adapter<ReportAdapter.MyViewHolder>() {

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

        holder.image.setOnClickListener {
            UploadImage()
        }
    }

    // Метод для получения массива файлов
    fun getFiles(): MutableList<ReportTable> {
        return reports
    }


}
