package com.example.telephone.commands

import com.example.telephone.config.ReactiveAuthentication
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class UserCommands(val userDetailsService: MapReactiveUserDetailsService,
                   val authManager: ReactiveAuthentication) {

    @ShellMethod
    fun newUser(@ShellOption name: String,
                        @ShellOption password: String) = userDetailsService
            .updatePassword(User.withUsername(name).build(), password)
            .block()


    @ShellMethod
    fun findUser(@ShellOption username: String) = userDetailsService
            .findByUsername(username)
            .map { "username: ${it.username}" }
            .defaultIfEmpty("There is no user named: $username")
            .block()

    @ShellMethod
    fun login(@ShellOption username: String,
                      @ShellOption password: String) {
        try {
            authManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
                    .doOnSuccess { result ->
                        SecurityContextHolder.getContext().authentication = result
                    }
                    .block()
        } catch (e: AuthenticationException) {
            println("Authentication Failed : ${e.message}")
        } catch (e: AccessDeniedException) {
            println("Not Authorized for : ${e.message}")
        }
    }
}