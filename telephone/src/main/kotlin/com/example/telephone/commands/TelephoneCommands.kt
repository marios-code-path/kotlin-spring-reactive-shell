package com.example.telephone.commands

import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.CurrentSecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.shell.Availability
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellMethodAvailability
import org.springframework.shell.standard.ShellOption
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@ShellComponent
class TelephoneCommands {
    val callQueue: Queue<String> = ConcurrentLinkedQueue()

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    @Secured("ROLE_CALL")
    fun call(@ShellOption username: String,
             @ShellOption message: String,
             @CurrentSecurityContext(expression="authentication.name")
             currentUser: String) {
        val message = "$currentUser: @$username - $message"
        callQueue.offer(message)
    }

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    @Secured("ROLE_RECEIVE")
    fun receive() {
        callQueue.stream().forEach {
            println(it)
        }
    }

    fun checkLoggedIn(): Availability = when (SecurityContextHolder.getContext().authentication) {
                null -> Availability.unavailable("You are not logged in.")
                else -> Availability.available()
            }
}