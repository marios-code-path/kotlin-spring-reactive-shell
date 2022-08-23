package com.example.telephone.commands

import com.example.telephone.service.SecurityContextRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import reactor.core.publisher.Mono

@ShellComponent
class UserCommands(val userDetailsService: MapReactiveUserDetailsService,
                   val authManager: ReactiveAuthenticationManager,
                   val repo: SecurityContextRepository) {

    @ShellMethod
    fun findUser(@ShellOption username: String) = userDetailsService
            .findByUsername(username)
            .map { "username: ${it.username}" }
            .defaultIfEmpty("There is no user named: $username")
            .block()

    @ShellMethod
    fun reactiveWhoAmI() = repo.load()
            .map {
                println("AUTHENTICATION ${it}")
            }
            .switchIfEmpty(Mono.error(Exception("Noone is logged in.")))
            .block()

    @ShellMethod
    fun login(@ShellOption username: String,
              @ShellOption password: String) {
        try {
            authManager
                    .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password))
                    .flatMap { result ->
                        repo.save(SecurityContextImpl(result))
                    }
                    .block()
        } catch (e: AuthenticationException) {
            println("Authentication Failed : ${e.message}")
        } catch (e: AccessDeniedException) {
            println("Not Authorized for : ${e.message}")
        }
    }
}