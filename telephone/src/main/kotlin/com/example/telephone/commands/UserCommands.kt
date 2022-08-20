package com.example.telephone.commands

import com.example.telephone.config.ReactiveAuth
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.annotation.CurrentSecurityContext
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class UserCommands(val userDetailsService: MapReactiveUserDetailsService,
                   val authManager: UserDetailsRepositoryReactiveAuthenticationManager) {

    @ShellMethod
    fun findUser(@ShellOption username: String) = userDetailsService
            .findByUsername(username)
            .map { "username: ${it.username}" }
            .defaultIfEmpty("There is no user named: $username")
            .block()

    @ShellMethod
    fun whoAmI() =ReactiveSecurityContextHolder.getContext()
            .map {

    }

    @ShellMethod
    fun login(@ShellOption username: String,
              @ShellOption password: String) {
        try {
            authManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
                    .doOnSuccess { result ->
                        SecurityContextHolder.createEmptyContext().authentication = result
                    }
                    .block()
        } catch (e: AuthenticationException) {
            println("Authentication Failed : ${e.message}")
        } catch (e: AccessDeniedException) {
            println("Not Authorized for : ${e.message}")
        }
    }
}