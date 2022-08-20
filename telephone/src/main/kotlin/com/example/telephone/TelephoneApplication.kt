package com.example.telephone

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity

@SpringBootApplication
//@EnableGlobalMethodSecurity(securedEnabled = true)
@EnableReactiveMethodSecurity
class TelephoneApplication

fun main(args: Array<String>) {
    runApplication<TelephoneApplication>(*args)
}
