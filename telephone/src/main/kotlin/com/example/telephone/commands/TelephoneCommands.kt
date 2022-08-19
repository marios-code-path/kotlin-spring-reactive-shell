package com.example.telephone.commands

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.shell.Availability
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellMethodAvailability
import org.springframework.shell.standard.ShellOption

@ShellComponent
class TelephoneCommands {

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    fun call(@ShellOption username: String,
             @ShellOption message: String) {

        val name = SecurityContextHolder.getContext().authentication.name

        println("$name says $message")
    }

    fun checkLoggedIn(): Availability =
            when (SecurityContextHolder.getContext().authentication != null) {
                true -> Availability.available()
                false -> Availability.unavailable("You are not logged in.")
            }
}