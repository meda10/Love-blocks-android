package blocks.love.utils

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RestApiManager {
    fun registerUser(registerData: RegisterData, onResult: (RegisterResponse?) -> Unit){
        val retrofit = ApiRegister.create().registerUser(registerData)
        retrofit.enqueue(
            object : Callback<RegisterResponse> {
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    Log.d("REG", response.body().toString())
                    onResult(response.body())
                }
            }
        )
    }

    fun loginUser(loginData: LoginData, onResult: (LoginResponse?) -> Unit){
        val retrofit = ApiLogin.create().loginUser(loginData)
        retrofit.enqueue(
            object : Callback<LoginResponse> {
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    Log.d("LOGIN", response.body().toString())
                    onResult(response.body())
                }
            }
        )
    }

    fun sendToken(tokenData: TokenData, onResult: (TokenResponse?) -> Unit){
        val retrofit = ApiToken.create().sendToken(tokenData)
        retrofit.enqueue(
            object : Callback<TokenResponse> {
                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                    when{
                        response.code() == 200 -> Log.d("TOKEN", "Successful 200")
                        else -> onResult(response.body())
                    }
                }
            }
        )
    }

    fun getProjectInfo(projectInfoData: ProjectInfoData, onResult: (ProjectInfoResponse?) -> Unit){
        val retrofit = ApiProjectInfo.create().getProjectInfo(projectInfoData)
        retrofit.enqueue(
            object : Callback<ProjectInfoResponse> {
                override fun onFailure(call: Call<ProjectInfoResponse>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<ProjectInfoResponse>, response: Response<ProjectInfoResponse>) {
                    Log.d("PROJECT", response.body().toString())
                    onResult(response.body())
                }
            }
        )
    }

    fun downloadProject(fileUrl: String, onResult: (ResponseBody?) -> Unit){
        val retrofit = ApiDownloadProject.create().downloadProject(fileUrl)
        retrofit.enqueue(
            object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    //todo handle error
                    onResult(null)
                }
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("DOWNLOAD", "server contacted and has file")
                    onResult(response.body())
                }
            }
        )
    }

    fun getProjects(projectsData: ProjectsData, onResult: (List<Project>?) -> Unit ){
        val retrofit = ApiProjects.create().getProjects(projectsData)
        retrofit.enqueue(
            object : Callback<List<Project>> {
                override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                    //todo handle error
                }
                override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                    Log.d("PROJECTS", "Getting Projects")
                    onResult(response.body())
                }
            }
        )
    }
}
