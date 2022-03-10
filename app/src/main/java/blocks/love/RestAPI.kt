package blocks.love

import android.util.Log
import android.widget.Toast
import blocks.love.UnsafeOkHttpClient.unsafeOkHttpClient
import com.firebase.ui.auth.data.model.User
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


data class RegisterData(var name: String, var email: String, var password: String, var password_confirmation: String, var terms: String)
data class RegisterResponse(var id: Int, var access_token: String, var token_type: String, var expires_at: String, var error: String)
data class LoginData(var email: String, var password: String)
data class LoginResponse(var id: Int, var access_token: String, var token_type: String, var expires_at: String, var error: String)
data class Test(var Message: String)

var unsafeHttpClient = unsafeOkHttpClient // TODO remove -> only for local development
const val BASE_URL = "https://192.168.0.20/api/"
//        var BASE_URL = "https://loveblocks.tk/api/"

private val client = OkHttpClient.Builder().build()


interface ApiTest {
    @Headers("Content-Type: application/json")
    @GET("book")
    fun getTest(): Call<Test>

    companion object {
        fun create(): ApiTest {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(unsafeHttpClient) // uses unsafe SSL TODO remove -> only for local development
                .build()
            return retrofit.create(ApiTest::class.java)
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
                    val registeredUser = response.body()
                    Log.d("POST", response.body().toString())
                    onResult(registeredUser)
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
                    val loggedUser = response.body()
                    Log.d("POST", response.body().toString())
                    onResult(loggedUser)
                }
            }
        )
    }

    fun getTest(){
        val retrofit = ApiTest.create().getTest()
        retrofit.enqueue(
            object : Callback<Test>{
                override fun onFailure(call: Call<Test>?, t: Throwable?) {
                    Log.d("REST", t.toString())
                    Log.d("REST", "FAIL")
                    call?.cancel()
                }
                override fun onResponse(call: Call<Test>?, response: Response<Test>?) {
                    if(response?.body() != null){
                        Log.d("REST", "WIN")
                        Log.d("REST", response.body().toString())
                    } else {
                        Log.d("REST", response?.body().toString())
                        Log.d("REST", "NULL")
                    }
                }
            }
        )
        Log.d("REST", "END")
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
