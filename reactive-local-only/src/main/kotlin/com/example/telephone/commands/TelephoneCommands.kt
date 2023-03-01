package com.example.telephone.commands

import com.example.telephone.service.SecurityContextRepository
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
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
class TelephoneCommands(val repo: SecurityContextRepository) {
    val callQueue: Queue<String> = ConcurrentLinkedQueue()

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    @PreAuthorize("CALL")
    fun call(@ShellOption username: String,
             @ShellOption message: String) =
            repo.load()
                    .doOnEach { signal ->
                        val currentUser = signal.get()?.authentication?.name
                        val message = "$currentUser: @$username - $message"
                        callQueue.offer(message)
                    }
                    //.block()

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    @PreAuthorize("hasRole('RECEIVE')")
    fun receive() = repo
            .load()
            .doOnEach{ signal ->
                callQueue.stream().forEach(::println)
            }
            //.block()

    fun checkLoggedIn(): Availability = when (repo.load().block() != null) {
                null -> Availability.unavailable("You are not logged in.")
                else -> Availability.available()
            }
}