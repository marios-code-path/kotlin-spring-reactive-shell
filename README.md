# Build a Spring Shell App w/ Security

What if I told you that the Spring Framework supportes creating [Shell](https://spring.io/projects/spring-shell) tools programatically?

# Motivation

As back-end developers, we are so used to using a front-end to guage our efforts. Perhaps you use [postman](https://www.postman.com) to send requests to your REST services. Maybe you're an RSocket kind of person and are using the [rsocket-cli](https://github.com/rsocket/rsocket-cli) tool. You might even be a streaming person, and send messages directly into your stream ingress such as a [Kafka](https://spring.io/projects/spring-kafka) topic! In any case, there exists the necessity to 'walk' through our services from the edge, and outside of the purview of any test regiment.

We sometimes create commandline tools, but that easily gets complicated when each execution can take time (to connect, authorize, etc..).  This is not the only reason why shell scripts typically exist. Indeed, shells are a vital if not necessary component of daily developer life.  But it helps to define a case for interacting with our apps in a quick, consistent manner. 

This demonstrations will guide us through building a simple shell app to interact with a 'telephone' application. The scenario involves microservices, session state and security. You should have a little understanding of [Spring Security](https://spring.io/projects/spring-security), [RSocket](https://spring.io/blog/2020/03/02/getting-started-with-rsocket-spring-boot-server), and [Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/). 
# The Demo Application

This demo application is a simplified form of 'telephone' that gives a few functions `new-user`, `login`, and `call`. We will begin with our favorite [start dot spring dot io initializr](https://start.spring.io) with the following parameters to make a fully interactive shell application:

* Kotlin
* JVM 17
* Dependencies: 
  * 'Spring Security'
  * 'Spring Shell'
  * 'reactive'

This application has a few backend components: `AuthenticationManager`, `UserDetailsService`, and our own `Telephone`.  Furthermore, shell-specific components include our own `UserCommands` for logging in, and our `TelephoneCommands` for sending and receiving messages - also known as 'calls'. True, this is more of an answering machine that all users share.

## Enabling Spring Security

First thing we want to do with this app is enable the use of `@Secure` annotations on our shell commands so that we can ensure we have `live` users interacting with our app. This is done with a global annotation `@EnableGlobalMethodSecurity` that also gets configured to indicate HOW we will decorate our secure methods. For example, we can use [JSR-250]() annotations and or use Spring `@Secure` annotation as a way to convery authorization requirements on a method.

> **_NOTE:_** Other configuration options are relative to how a security method gets wrapped in run-time, and at what cardinatlity in an AOP/Proxy stack does the security advisor execute (first, last, etc..). These are outside the scope of this demo, but check out [this doc]() for more info and examples.

TelephoneApplication.kt
```kotlin
@SpringBootApplication
@EnableGlobalMethodSecurity(securedEnabled = true)
class TelephoneApplication

fun main(args: Array<String>) {
	runApplication<TelephoneApplication>(*args)
}
```

The next security-related objectives are to handle user storage, and advise Spring Security how we wish to authenticate users.
### Implementing Users and Authentication

We will utilize Spring Security to provide the user store, authorization and authentication needs. In this demo, we will make an instance of [MapReactiveUserDetailService]() that does the job of ensuring we have users to map to the Security Context.  Like the imperative `UserDetailsService`, this version uses Reactive [Publishers]() to return [UserDetails]() data. Use one or the other in your own applications. We only need to provide a list of active users to this instance - or else we wont have someone log in with!

TelephoneConfiguration.kt:
```kotlin
@Configuration
class TelephoneConfiguration {

    @Bean
    fun userDetailService() = MapReactiveUserDetailsService(
            User.withUsername("spring")
                    .password("12345")
                    .authorities(
                            SimpleGrantedAuthority("ROLE_CALL")
                    ).build(),
            User.withUsername("tux")
                    .password("12345")
                    .authorities(SimpleGrantedAuthority("ROLE_RECEIVE")).build())
    // ...
}
```

The first two users we can login with having the same passwords - Spring and Tux - have different roles. The 'Tux' user can only receive messages, while the 'Spring' user can make calls. This give us enough use cases to demonstrate just the couple ideas in this example.

### Reactive Authentication in a local App

The next component also comes from Spring Security. This `AuthenticationManager` will be subclassed to do mostly boilerplate of credential matching before the active user is placed into the [SecurityContextHolder]().

ReactiveAuthentication.kt:
```kotlin
class ReactiveAuth(private val userDetailService: MapReactiveUserDetailsService) : ReactiveAuthenticationManager {
    override fun authenticate(authen: Authentication): Mono<Authentication> {
        val credential = authen.credentials.toString()

        return userDetailService
                .findByUsername(authen.name)
                .switchIfEmpty(Mono.error(Exception("User Not Found")))
                .map { user ->
                    if (!user.password.equals(credential, true))
                        throw Exception("User Authentication Error.")
                    UsernamePasswordAuthenticationToken(user, user.password, user.authorities)
                }
    }
}
```

The `ReactiveAuthenticationManager` variety does the same as standard `AuthenticationManager` with the change in signature from imperative `Authentication` to Reactive `Mono<Authentication>`. This also means we get the more fluent style for service composition - UserDetails will look up users, and we can map that result to perform credential matching and return the [Authentication]() token the app will need for the next section.
## Shell Commands

Now that we have the user and authentication/authorization needs met, we can focus on our app: to make a shell

# Shell Commands

```
shell:>help
AVAILABLE COMMANDS

Built-In Commands
       help: Display help about available commands
       stacktrace: Display the full stacktrace of the last error.
       clear: Clear the shell screen.
       quit, exit: Exit the shell.
       history: Display or save the history of previously run commands
       version: Show version info
       script: Read and execute commands from a file.

Telephone Commands
       * call: 
       * receive: 
       who-am-i: 

User Commands
       login: 
       find-user: 


Commands marked with (*) are currently unavailable.
Type `help <command>` to learn more.

shell:>
```
## Login Command

## Doing App Stuff

# Tests

## muting the shell during tests

# Conclusion


# Links and Learnings
