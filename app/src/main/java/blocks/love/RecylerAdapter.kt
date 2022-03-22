package blocks.love

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RecyclerAdapter(val context: Context): RecyclerView.Adapter<RecyclerAdapter.ProjectViewHolder>() {

    var projectList : List<Project> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_adapter,parent,false)
        return ProjectViewHolder(view)
    }

    override fun getItemCount(): Int {
        return projectList.size
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projectList[position]
        holder.bind(project, context)
    }

    fun setProjectListItems(projectList: List<Project>){
        this.projectList = projectList
        this.notifyDataSetChanged()
    }

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val projectName: TextView = itemView.findViewById(R.id.projectName)
        private val downloadButton: Button = itemView.findViewById(R.id.downloadButton)

        fun bind(project: Project, context: Context) {
            projectName.text = project.name

            downloadButton.setOnClickListener {
                if (context is MainActivity){
                    val user = Firebase.auth.currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
                        if (getIDToken.isSuccessful) {
                            val idToken = getIDToken.result.token
                            val projectInfoData = ProjectInfoData(
                                id_token = idToken!!,
                                id = project.id,
                            )
                            Log.d("DOWNLOAD", "project id: ${project.id}")
                            RestApiManager().getProjectInfo(projectInfoData) { responseData ->
                                if (responseData?.url != null ) {
                                    //todo URL
                                    RestApiManager().downloadProject("https://192.168.0.20/download") { fileData ->
                                        if (fileData != null){
                                            context.writeFile(fileData, "New_File.jpg")
                                        }
                                        else{
                                            Log.d("DOWNLOAD", "Null")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}