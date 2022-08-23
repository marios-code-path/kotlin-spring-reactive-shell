package example.rsocket.security

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RsocketSecurityApplication

fun main(args: Array<String>) {
	runApplication<RsocketSecurityApplication>(*args)
}
