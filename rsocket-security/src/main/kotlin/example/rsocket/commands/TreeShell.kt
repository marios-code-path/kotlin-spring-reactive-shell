package example.rsocket.commands

import example.rsocket.clientconfig.RequesterFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.shell.Availability
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellMethodAvailability
import java.util.*

// Shutdown the application.
@ShellComponent
class TreeShell {

    @Autowired
    private lateinit var requesterFactory: RequesterFactory

    private var requester: Optional<RSocketRequester> = Optional.empty()

    @ShellMethod
    fun login(username: String, password: String) {
        requester = Optional.of(requesterFactory.requester(username, password))
        // fix so that availability check can happen via 'status' endpoint.
    }

    @ShellMethod
    @ShellMethodAvailability("checkLoggedIn")
    fun rake() =
        requester.get()
                .route("rake")
                .retrieveFlux<String>()
                .blockLast()!!


    fun checkLoggedIn(): Availability = when(requester.isPresent) {
        true -> Availability.available()
        else -> Availability.unavailable("Not Logged In")
    }


}