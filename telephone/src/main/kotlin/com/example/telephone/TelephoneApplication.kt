package com.example.telephone

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity

@SpringBootApplication
//@EnableGlobalMethodSecurity(securedEnabled = true)
class TelephoneApplication

fun main(args: Array<String>) {
	runApplication<TelephoneApplication>(*args)
}
