package blocks.love.utils

import blocks.love.unsafeHttpClient
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

//https://medium.com/mobile-app-development-publication/download-file-in-android-with-kotlin-874d50bccaa2
class FileDownloader(okHttpClient: OkHttpClient) {

    companion object {
        private const val BUFFER_LENGTH_BYTES = 1024 * 8
        private const val HTTP_TIMEOUT = 30
    }

//    // TODO remove -> only for local development
//    object UnsafeOkHttpClient {
//        val unsafeOkHttpClient: OkHttpClient
//            get() = try {
//                val trustAllCerts = arrayOf<TrustManager>(
//                    object : X509TrustManager {
//                        @Throws(CertificateException::class)
//                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
//                        @Throws(CertificateException::class)
//                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
//                        override fun getAcceptedIssuers(): Array<X509Certificate> { return arrayOf() }
//                    }
//                )
//                val sslContext = SSLContext.getInstance("SSL")
//                sslContext.init(null, trustAllCerts, SecureRandom())
//                val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
//                val builder = OkHttpClient.Builder()
//                builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//                builder.hostnameVerifier { hostname, session -> true }
//                builder.build()
//            } catch (e: Exception) {
//                throw RuntimeException(e)
//            }
//    }

    private var okHttpClient: OkHttpClient
//    private var unsafeXHttpClient = blocks.love.UnsafeOkHttpClient.unsafeOkHttpClient // TODO remove -> only for local development

    init {
        val okHttpBuilder = okHttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT.toLong(), TimeUnit.SECONDS)
        this.okHttpClient = okHttpBuilder.build()
    }

    fun download(url: String, file: File): Observable<Int> {
        return Observable.create<Int> { emitter ->
            val request = Request.Builder().url(url).build()
//            val response = okHttpClient.newCall(request).execute()
            val response = unsafeHttpClient.newCall(request).execute()
            val body = response.body()
            val responseCode = response.code()
            if (responseCode >= HttpURLConnection.HTTP_OK &&
                responseCode < HttpURLConnection.HTTP_MULT_CHOICE &&
                body != null) {
                val length = body.contentLength()
                body.byteStream().apply {
                    file.outputStream().use { fileOut ->
                        var bytesCopied = 0
                        val buffer = ByteArray(BUFFER_LENGTH_BYTES)
                        var bytes = read(buffer)
                        while (bytes >= 0) {
                            fileOut.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = read(buffer)
                            emitter.onNext(((bytesCopied * 100)/length).toInt())
                        }
                    }
                    emitter.onComplete()
                }
            } else {
                throw IllegalArgumentException("Error occurred when do http get $url")
            }
        }
    }
}
