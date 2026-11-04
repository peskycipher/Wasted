package me.lucky.wasted.btcpay

import me.lucky.wasted.BuildConfig
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import org.json.JSONObject

// client for the operator-owned BTCPay Server (Greenfield API).
// see docs/adr/0003: this is the only payment integration; pikasim.com's
// own API is intentionally never called from this app.
interface BtcpayApi {
    val configured: Boolean

    // amount is a decimal string in [currency] major units
    fun createInvoice(amount: String, currency: String): BtcpayInvoice

    fun getInvoice(id: String): BtcpayInvoice
}

data class BtcpayInvoice(val id: String, val checkoutLink: String, val status: String)

data class BtcpayClientConfig(
    val url: String,
    val storeId: String,
    val apiKey: String,
    val viaTor: Boolean,
)

class BtcpayClient(private val cfg: BtcpayClientConfig) : BtcpayApi {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val TOR_PROXY_HOST = "127.0.0.1"
        private const val TOR_PROXY_PORT = 9050

        fun fromBuildConfig() = BtcpayClientConfig(
            BuildConfig.BTCPAY_URL.trimEnd('/'),
            BuildConfig.BTCPAY_STORE_ID,
            BuildConfig.BTCPAY_API_KEY,
            BuildConfig.BTCPAY_VIA_TOR,
        )
    }

    override val configured: Boolean
        get() = cfg.url.isNotEmpty() && cfg.storeId.isNotEmpty() && cfg.apiKey.isNotEmpty()

    // amount is a decimal string in [currency] major units
    override fun createInvoice(amount: String, currency: String): BtcpayInvoice {
        val body = JSONObject()
            .put("amount", amount)
            .put("currency", currency)
        return call(
            "POST",
            "/api/v1/stores/${cfg.storeId}/invoices",
            body,
        ).let { parse(it) }
    }

    override fun getInvoice(id: String): BtcpayInvoice =
        call("GET", "/api/v1/stores/${cfg.storeId}/invoices/$id", null).let { parse(it) }

    private fun parse(o: JSONObject) = BtcpayInvoice(
        o.getString("id"),
        o.optString("checkoutLink"),
        o.optString("status"),
    )

    private fun call(method: String, path: String, body: JSONObject?): JSONObject {
        val conn = (if (cfg.viaTor) {
            URL("${cfg.url}$path").openConnection(
                Proxy(Proxy.Type.SOCKS, InetSocketAddress(TOR_PROXY_HOST, TOR_PROXY_PORT)),
            )
        } else {
            URL("${cfg.url}$path").openConnection()
        }) as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "token ${cfg.apiKey}")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                throw IllegalStateException("BTCPay $path -> HTTP $code $err")
            }
            return JSONObject(
                conn.inputStream.bufferedReader().use { it.readText() },
            )
        } finally {
            conn.disconnect()
        }
    }
}