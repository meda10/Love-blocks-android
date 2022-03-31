package blocks.love

import android.util.Log
import blocks.love.UnsafeOkHttpClient.unsafeOkHttpClient
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ProjectsData(var id_token: String)
data class Project(var id: Int, var name: String)
data class ProjectInfoData(var id: Int, var id_token: String)
data class ProjectInfoResponse(val url: String, val name: String)
data class RegisterData(var name: String, var email: String, var password: String, var password_confirmation: String, var terms: String, var fcm_token: String)
data class RegisterResponse(val id: String, val access_token: String, val token_type: String, val expires_at: String, val errors: Error)
data class LoginData(var email: String, var password: String, var fcm_token: String)
data class LoginResponse(val id: String, val access_token: String, val token_type: String, val expires_at: String, val errors: Error)
data class TokenData(val fcm_token: String, val id_token: String)
data class TokenResponse(val errors: Error)
data class Error(val name: List<String>, val email: List<String>, val password: List<String>, val terms: List<String>, val error: String)

var unsafeHttpClient = unsafeOkHttpClient // TODO remove -> only for local development
const val BASE_URL = "https://192.168.0.20/api/"
//        var BASE_URL = "https://loveblocks.tk/api/"

private val client = OkHttpClient.Builder().build()


interface ApiProjects {
    @Headers("Content-Type: application/json")
    @POST("projects")
    fun getProjects(@Body projectsData: ProjectsData) : Call<List<Project>>

    companion object {
        fun create() : ApiProjects {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiProjects::class.java)
        }
    }
}

interface ApiProjectInfo {
    @Headers("Content-Type: application/json")
    @POST("project/love")
    fun getProjectInfo(@Body projectInfoData: ProjectInfoData): Call<ProjectInfoResponse>

    companion object {
        fun create(): ApiProjectInfo {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiProjectInfo::class.java)
        }
    }
}

interface ApiDownloadProject {

    @Streaming
    @GET
    fun downloadProject(@Url fileUrl: String): Call<ResponseBody>

    companion object {
        fun create(): ApiDownloadProject {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiDownloadProject::class.java)
        }
    }
}

interface ApiToken {
    @Headers("Content-Type: application/json")
    @POST("fcm_token")
    fun sendToken(@Body tokenData: TokenData): Call<TokenResponse>

    companion object {
        fun create(): ApiToken {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiToken::class.java)
        }
    }
}

interface ApiLogin {
    @Headers("Content-Type: application/json")
    @POST("login")
    fun loginUser(@Body loginData: LoginData): Call<LoginResponse>

    companion object {
        fun create(): ApiLogin {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiLogin::class.java)
        }
    }
}

interface ApiRegister {
    @Headers("Content-Type: application/json")
    @POST("register")
    fun registerUser(@Body registerData: RegisterData): Call<RegisterResponse>

    companion object {
        fun create(): ApiRegister {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiRegister::class.java)
        }
    }
}

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
                override fun onFailure(call: Call<List<Project>>?, t: Throwable?) {
                    //todo handle error
                }
                override fun onResponse(call: Call<List<Project>>?, response: Response<List<Project>>) {
                    Log.d("PROJECTS", "Getting Projects")
                    onResult(response.body())
                }
            }
        )
    }
}


// TODO remove -> only for local development
object UnsafeOkHttpClient {
    val unsafeOkHttpClient: OkHttpClient
    get() = try {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> { return arrayOf() }
            }
        )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { hostname, session -> true }
        builder.build()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}
