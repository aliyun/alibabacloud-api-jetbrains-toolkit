package com.alibabacloud.telemetry

import com.alibabacloud.models.telemetry.TelemetryData
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import java.util.concurrent.atomic.AtomicBoolean

abstract class TelemetryService (private val batch: BatchDataTelemetry) : Disposable{
    private val isDisposing = AtomicBoolean(false)

    override fun dispose() {
        if (!isDisposing.compareAndSet(false, true)) {
            return
        }
        batch.shutdown()
    }

    fun record(data: TelemetryData) {
        batch.enqueue(data)
    }

    @Synchronized
    fun setTelemetryEnabled(isEnabled: Boolean) {
        batch.onTelemetryEnabledChange(isEnabled)
    }

    companion object {
        fun getInstance(): TelemetryService = service()
    }
}

class DefaultTelemetryService : TelemetryService(batch) {
    private companion object {
        private val batch: BatchDataTelemetry by lazy { DefaultBatchDataTelemetry() }
    }
}