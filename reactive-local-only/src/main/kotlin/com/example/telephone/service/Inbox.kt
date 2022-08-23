package com.example.telephone.service

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Controller
class Inbox {
    val inboxes: MutableMap<String, Queue<String>> = ConcurrentHashMap()

    fun getInbox(name: String): Queue<String> {
        if (!inboxes.containsKey(name))
            inboxes[name] = ConcurrentLinkedQueue()

        return inboxes[name]!!
    }

    @MessageMapping("call")
    fun call(name: String, message: String) =
            getInbox(name).offer(message)

    @MessageMapping("receive")
    fun receive(name: String) =
            Flux.fromStream(getInbox(name).stream())
                    .doOnNext { message ->
                        println("$name: @$name - $message")
                    }
}