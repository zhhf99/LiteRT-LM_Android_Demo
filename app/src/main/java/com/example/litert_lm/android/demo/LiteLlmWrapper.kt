package com.example.litertlmdemo

import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.*

object LiteLlmWrapper {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun init(modelPath: String) {
        Engine.setNativeMinLogSeverity(LogSeverity.ERROR)

        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.GPU()
        )

        engine = Engine(config).apply {
            initialize()
        }

        conversation = engine?.createConversation()
    }

    fun send(prompt: String, callback: (String) -> Unit) {
        val conv = conversation ?: return

        scope.launch {
            val sb = StringBuilder()

            conv.sendMessageAsync(prompt).collect { token ->
                sb.append(token)
                callback(sb.toString())
            }
        }
    }

    fun release() {
        scope.cancel()

        conversation?.close()
        engine?.close()

        conversation = null
        engine = null
    }
}