package blocks.love

import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import blocks.love.utils.downloadLoveProject
import blocks.love.utils.openLove2dApp
import blocks.love.utils.showDialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File

class RecyclerAdapter(val context: Context): RecyclerView.Adapter<RecyclerAdapter.ProjectViewHolder>() {

    private var projectList : List<Project> = listOf()

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
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        private val openButton: Button = itemView.findViewById(R.id.openButton)

        fun bind(project: Project, context: Context) {
            projectName.text = project.name
            val fileName = projectName.text.toString().replace(" ", "_")
            val loveFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName + ".love"

            disableOrEnableButtons(loveFilePath)

            downloadButton.setOnClickListener { downloadProject(project, context, loveFilePath, this) }
            deleteButton.setOnClickListener { deleteProject(context, loveFilePath) }
            openButton.setOnClickListener { openLove2dApp(loveFilePath, context) }
        }

        private fun downloadProject(project: Project, context: Context, loveFilePath: String, projectViewHolder: ProjectViewHolder){
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
                                downloadLoveProject(
                                    responseData.url.replace("localhost", "192.168.0.20"),
                                    context,
                                    context.fileDownloader,
                                    loveFilePath,
                                    projectViewHolder
                                )
                            } else {
                                context.mainLayout.showDialog(R.string.connect_to_server, R.string.something_wrong_title, context)
                            }
                        }
                    }
                }
            }
        }

        fun disableOrEnableButtons(loveFilePath: String){
            if(!checkIfProjectExists(loveFilePath)) {
                deleteButton.isEnabled = false
                openButton.isEnabled = false
            } else {
                deleteButton.isEnabled = true
                openButton.isEnabled = true
            }
        }

        private fun checkIfProjectExists(loveFilePath: String): Boolean {
            if (File(loveFilePath).exists()) return true
            return false
        }

        private fun deleteProject(context: Context, loveFilePath: String){
            if (context is MainActivity){
                val loveFile = File(loveFilePath)
                if (loveFile.exists()) loveFile.delete()
                disableOrEnableButtons(loveFilePath)
            }
        }
    }
}