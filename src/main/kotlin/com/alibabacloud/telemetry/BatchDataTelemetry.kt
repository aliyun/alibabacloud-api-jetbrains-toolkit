package com.alibabacloud.telemetry

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.models.telemetry.DefaultApplicationInfo
import com.alibabacloud.models.telemetry.TelemetryData
import com.alibabacloud.telemetry.constants.TelemetryConstants
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Volatile
var expireTime = 0L

@Volatile
var token = ""

interface BatchDataTelemetry {
    fun enqueue(data: TelemetryData)
    fun consume(shouldRetry: Boolean)
    fun onTelemetryEnabledChange(isEnabled: Boolean)
    fun shutdown()
}

class DefaultBatchDataTelemetry(
    private val httpClient: OkHttpClient = OkHttpClientProvider.instance,
    private val batchSize: Int = BATCH_SIZE,
    maxQueueSize: Int = MAX_QUEUE_SIZE,
    private val executor: ScheduledExecutorService = createExecutor(),
) : BatchDataTelemetry {
    private val log: Logger = Logger.getInstance(DefaultBatchDataTelemetry::class.java)
    private val isTelemetryEnabled: AtomicBoolean = AtomicBoolean(false)
    private val isShuttingDown: AtomicBoolean = AtomicBoolean(false)
    val telemetryDataQueue: LinkedBlockingDeque<TelemetryData> = LinkedBlockingDeque(maxQueueSize)
        @TestOnly get

    init {
        executor.scheduleWithFixedDelay(
            {
                if (!isShuttingDown.get()) {
                    try {
                        consume(true)
                    } catch (e: Exception) {
                        log.warn("Unexpected exception while reporting telemetry. ${e::class.simpleName}: ${e.message}")
                    }
                }
            },
            REPORT_INTERVAL,
            REPORT_INTERVAL,
            REPORT_INTERVAL_UNIT
        )
    }

    override fun enqueue(data: TelemetryData) {
        if (!isTelemetryEnabled.get()) {
            return
        }
        try {
            telemetryDataQueue.add(data)
        } catch (e: Exception) {
            log.warn("Failed to add telemetry data to queue.")
        }
    }

    @Synchronized
    override fun consume(shouldRetry: Boolean) {
        if (!isTelemetryEnabled.get()) {
            return
        }
        while (!telemetryDataQueue.isEmpty()) {
            val batch: ArrayList<TelemetryData> = arrayListOf()
            while (!telemetryDataQueue.isEmpty() && batch.size < batchSize) {
                batch.add(telemetryDataQueue.pop())
            }
            val stop = runBlocking {
                try {
                    report(batch)
                } catch (e: Exception) {
                    log.warn("Report telemetry data failed. ${e::class.simpleName}: ${e.message}")
                    if (shouldRetry) {
                        log.warn("Retry to report telemetry data later...")
                        telemetryDataQueue.addAll(batch)
                        return@runBlocking true
                    }
                }
                return@runBlocking false
            }
            if (stop) {
                return
            }
        }
    }

    override fun onTelemetryEnabledChange(isEnabled: Boolean) {
        if (isEnabled) {
            isTelemetryEnabled.set(true)
        } else {
            telemetryDataQueue.clear()
        }
        consume(isEnabled)
        if (!isEnabled) {
            isTelemetryEnabled.set(false)
        }
    }

    override fun shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            return
        }
        executor.shutdown()
        consume(false)
    }

    private suspend fun report(batch: ArrayList<TelemetryData>) {
        withContext(bgContext) {
            val jsonData = JsonObject()
            jsonData.add("metadata", Gson().toJsonTree(batch))
            val metadata = Gson().toJson(jsonData)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = metadata.toRequestBody(mediaType)
            val userAgent = DefaultApplicationInfo.userAgent
            val macAddr = DefaultApplicationInfo.macAddr
            if (System.currentTimeMillis() - expireTime > (23 * 60 + 55) * 60 * 1000) {
                val request = RequestUtil.createRequest(
                    TelemetryConstants.TOKEN_URL,
                    headers = hashMapOf(
                        "user-agent" to userAgent,
                        "x-plugin-source-ip" to macAddr,
                        "x-plugin-timestamp" to System.currentTimeMillis().toString(),
                    )
                )

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Get token not success.")
                    } else {
                        val tokenResponse = getResponse(response)
                        expireTime = tokenResponse?.get("expireTime")?.asLong ?: 0
                        token = tokenResponse?.get("token")?.asString ?: ""
                    }
                }
            }

            val request = RequestUtil.createRequest(
                TelemetryConstants.REPORT_URL,
                "POST",
                requestBody,
                hashMapOf(
                    "user-agent" to userAgent,
                    "x-plugin-source-ip" to macAddr,
                    "x-plugin-timestamp" to System.currentTimeMillis().toString(),
                    "x-plugin-token" to token
                )
            )

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Request for report data failed, $response")
                } else {
                    getResponse(response)
                }
            }
        }
    }

    private fun getResponse(response: Response): JsonObject? {
        val responseBody = response.body?.string()
        val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
        if (!jsonResponse.has("success") || jsonResponse.get("success")?.asString == "false") {
            throw IOException("Request failed, message: ${jsonResponse.get("message")?.asString}")
        }
        return jsonResponse
    }

    internal val bgContext
        get() = AppExecutorUtil.getAppExecutorService().asCoroutineDispatcher()

    companion object {
        private const val BATCH_SIZE = 50
        private const val MAX_QUEUE_SIZE = 2000
        private const val REPORT_INTERVAL = 5L
        private val REPORT_INTERVAL_UNIT = TimeUnit.MINUTES

        private fun createExecutor() = Executors.newSingleThreadScheduledExecutor { runnable ->
            val daemon = Thread(runnable)
            daemon.isDaemon = true
            daemon.name = "Telemetry-Data-Reporter"
            daemon
        }
    }
}