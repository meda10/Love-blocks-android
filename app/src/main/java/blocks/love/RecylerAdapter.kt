package blocks.love

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import blocks.love.utils.showDialog
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

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                downloadButton.visibility = View.GONE
//            }

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
                                    context.downloadLoveProject(
                                        responseData.url.replace("localhost", "192.168.0.20"),
                                        responseData.name.replace(" ", "_")
                                    )
                                } else {
                                    context.mainLayout.showDialog(R.string.connect_to_server, R.string.something_wrong_title, context)
                                }
                            }
                        }
                    }
                }
            }

//            downloadAPKButton.setOnClickListener {
//                if (context is MainActivity){
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                        val user = Firebase.auth.currentUser
//                        user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
//                            if (getIDToken.isSuccessful) {
//                                val idToken = getIDToken.result.token
//                                val projectInfoData = ProjectInfoData(
//                                    id_token = idToken!!,
//                                    id = project.id,
//                                )
//                                Log.d("APK", "APK id: ${project.id}")
//                                context.downloadAPK("adw", "LoveBlocksProject.apk")
//                            }
//                        }
//                    } else {
//                        context.requestPermission(WRITE_EXTERNAL_STORAGE, MainActivity.WRITE_EXTERNAL_STORAGE_PERMISSION_CODE, R.string.storage_access_required)
//                        if (ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
//                            val user = Firebase.auth.currentUser
//                            user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
//                                if (getIDToken.isSuccessful) {
//                                    val idToken = getIDToken.result.token
//                                    val projectInfoData = ProjectInfoData(
//                                        id_token = idToken!!,
//                                        id = project.id,
//                                    )
//                                    Log.d("APK", "APK id: ${project.id}")
//                                    context.downloadAPK("adw", "LoveBlocksProject.apk")
////                                RestApiManager().getProjectAPKInfo(projectAPKInfoData) { responseData ->
////                                    if (responseData?.url != null ) {
//                                          //todo URL
////                                        context.downloadAPK(responseData.url)
////                                    }
////                                }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}