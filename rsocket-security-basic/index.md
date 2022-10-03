---
date: '2022-09-22'
description: Implementing RSocket Security with Spring Boot. Part 1
lastmod: '2022-08-11'
linkTitle: Implementing RSocket Security with Spring Boot.
metaTitle: Simple RSocket Security with Spring Boot via Spring Security
patterns:
- API
tags:
- Security
- Getting Started
- Kotlin
- Reactive
- Spring Boot
- Spring Security
team:
- Mario Gray
title: Implementing Secure, user-aware RSocket services with Spring Boot
oldPath: "/content/guides/spring/reactive-rsocket-security-spring-boot-pt1.md"
aliases:
- "/guides/spring/reactive-distributed-tracing"
level1: Building Modern Applications
level2: Frameworks and Languages
---

This guide will discuss RSocket service security with Spring Boot, by way of Spring Security. We will surface RSocket routes that enforce specific security measures and describe what this means internally. This guide will inform you of the additional configuration options provided when configuring for Spring Security on a Spring Boot 2.7.x/RSocket application.

It is assumed the developer knows Kotlin, uses Spring Boot, and has an understanding of the [Reactive Streams](https://github.com/reactive-streams/reactive-streams-jvm) on the JVM. If you're new to Spring Security for Reactive streams, then this guide should help shed light on the subject. Of course, the best place to understand are [The Guides](https://docs.spring.io/spring-security/reference/5.6.5/reactive/integrations/rsocket.html#page-title), so please read them!

## Motivation

Writing secure RSocket apps with Spring Boot application is not hard and takes just a few lines of code. But you may now already know that security is a broad and widely discussed topic. This example is designed to help you to quickly understand the basics of integrating Spring Security into your Reactive / RSocket application. We will cover the authentication and authorization aspects and how they are applied within Spring. There are a number of strategies to authenticate with such as JWT, Kerberos, and Password to name a few. This guide will focus on the `simple` strategy.

We want our applications to respond to a user's privilege level; as multi-user applications tend to be specific with regards to feature availability. What emerges through Spring Security, is Role Based Access Control - the ability to make privilege specific logic feasable and with minimal boilerplate.

### Authorization vs Authentication

Authentication is the process which lets our apps identify a user. Authentication schemes are methods which describe a secure process of identification. Some simple ones are username/password, while currently OAuth and 2-n-factor are all the rage.

Authorization (access control) is the process which lets your application determine how access is granted to users (also known as the principal). This begins to sound straight forward, but can be surfaced in our application in a number of ways. One such way is Role Based Access Control - in which a user may have granted privileges given by role 'names' - e.g. 'WRITE', 'READ' for a given resource. Additionally, RBAC relies on the application to make these decisions (as governed through `@PreAuthorize` and `@Secured` annotations).

This guide assumes RBAC as the choice strategy for authorization, and is the default strategy given by Spring Security 5.

## The Application

The [Example app](https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.7.3&packaging=jar&jvmVersion=17&groupId=example&artifactId=rsocket-security&name=rsocket-security&description=Reactive%20RSocket%20Security%20Demo&packageName=example.rsocket.security&dependencies=rsocket,security,spring-shell) is a service containing 2 methods for sending streams of Strings.

The Service interface is as follows:

```kotlin
interface TreeService {
    fun shakeForLeaf(): Mono<String> // 1
    fun rakeForLeaves(): Flux<String> // 2

    companion object {
        val LEAF_COLORS = listOf("Green", "Yellow", "Orange", "Brown", "Red") // 3
    }
}
```

We have 2 functions and a static list that:

1) Return a `Mono<String>` of leaf colors.
2) Return a `Flux<String>` of leaf colors.
3) List of Strings for supported leaf colors.

We can then implement this Mono/Flux streams of Strings:

```kotlin
class TreeServiceImpl : TreeService {

    override fun shakeForLeaf(): Mono<String> = Mono.just(LEAF_COLORS.get(Random.nextInt(LEAF_COLORS.size)))

    override fun rakeForLeaves(): Flux<String> = Flux
            .fromStream(
                    Stream.generate { Random.nextInt(LEAF_COLORS.size) }
                            .limit(10)
            ).map { LEAF_COLORS[it] }
}
```

Subclass the service interface for our RSocket Controller mapping:

```kotlin
interface TreeControllerMapping : TreeService {
    @MessageMapping("shake")
    override fun shakeForLeaf(): Mono<String>

    @MessageMapping("rake")
    override fun rakeForLeaves(): Flux<String>
}
```

Next, another subclass that applies Spring Security annotations. Securing the services with what level of authorization our application requires. Use [@PreAuthorize](https://docs.spring.io/spring-security/reference/5.6.8/servlet/authorization/expression-based.html#_access_control_using_preauthorize_and_postauthorize), which is the preferred way for securing reactive streams through annotation.

```kotlin
interface TreeServiceSecurity : TreeService {

    @PreAuthorize("hasRole('SHAKE')")
    override fun shakeForLeaf(): Mono<String>

    @PreAuthorize("hasRole('RAKE')")
    override fun rakeForLeaves(): Flux<String>
}
```

Finally, we can put the whole thing together and expose it as an RSocket service.

### Putting the App together

The production application will configure security rules, create the exposed services, and provide other support beans that we will discuss later on. In the next listing, we will look at enabling Spring Security for RSocket.

```kotlin
@EnableReactiveMethodSecurity  // 1
@EnableRSocketSecurity // 2
@SpringBootApplication
class App {

 // ...

    @Controller
    class ServerTreeController : TreeControllerMapping,
            TreeServiceSecurity, TreeService by TreeServiceImpl() {  // 3
        fun status(@AuthenticationPrincipal user: Mono<UserDetails>): Mono<String> =
                user.hasElement().map (Boolean::toString)
    }

// ...

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<App>(*args)
        }
    }
}
```

To do much of the Reactive Security work, Spring Security uses a [ReactiveSecurityContextHolder](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/context/ReactiveSecurityContextHolder.html) to place the [SecurityContext](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/context/SecurityContext.html) into  [Reactor's Context](https://projectreactor.io/docs/core/release/reference/#context).

Since reactive operators now have access to this `SecurityContext`, Spring can wrap logic within advices to determine things like the current logged in user and it's privileges.

1) The [RSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/RSocketSecurity.java) bean is central to security of RSocket endpoints. It is enabled by decorating the main configuration class with [@EnableRSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/EnableRSocketSecurity.java). What this does is as stated in documentation -  it allows configuring RSocket based security. 
2) To enable the usage of Spring's own method security annotations on Reactive Streams, add the @[EnableReactiveMethodSecurity](https://github.com/spring-projects/spring-security/blob/210693eb6bd0cba51874ce150c73090c95d4e08b/docs/modules/ROOT/pages/reactive/authorization/method.adoc) annotation to the main configuraiton class. 
3) The controller is fully configured here, along with an added un-secure `status` route. The status uses [@AuthenticationPrincipal](https://github.com/spring-projects/spring-security/blob/main/web/src/main/java/org/springframework/security/web/bind/annotation/AuthenticationPrincipal.java) to tell whether we are really logged in by returning boolean.

** ReactiveAuthenticationManager paragraph
### Configure RSocket Security

With `@EnableRSocketSecurity`, we gain RSocket security through [RSocket Interceptors](https://github.com/spring-projects/spring-security/blob/main/rsocket/src/main/java/org/springframework/security/rsocket/core/ContextPayloadInterceptorChain.java). Interceptors have the ability to work during, before or after a `level` in processing. For RSocket, this means any of the following levels:

* Transport level
* At the level of accepting new connections
* Performing requests
* Responding to requests

Since a payload can have many metadata formats to confer credential exchange, Spring's [RSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/RSocketSecurity.java) bean provides a fluent builder for configuring Simple, Basic, JWT, and custom authentication methods at these level, in addition to application-specific RBAC settings. This builder will describe a set of [AuthenticationPayloadInterceptor](https://github.com/spring-projects/spring-security/blob/main/rsocket/src/main/java/org/springframework/security/rsocket/authentication/AuthenticationPayloadInterceptor.java)'s that converts payload metdata into an [Authentication](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/Authentication.java) instances inside the `SecurityContext`. 



To further our understanding of the configuration, lets examine the [SecuritySocketAcceptorInterceptorConfiguration](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/SecuritySocketAcceptorInterceptorConfiguration.java) class, which sets up the default security configuration for RSocket. This class, imported by `@EnableRSocketSecurty`, will configure a [PayloadSocketAcceptorInterceptor](https://github.com/spring-projects/spring-security/blob/main/rsocket/src/main/java/org/springframework/security/rsocket/core/PayloadSocketAcceptorInterceptor.java) for simple and basic authentications, while requiring authentication for requests:

```java
package org.springframework.security.config.annotation.rsocket;

class SecuritySocketAcceptorInterceptorConfiguration {
    //...
	private PayloadSocketAcceptorInterceptor defaultInterceptor(ObjectProvider<RSocketSecurity> rsocketSecurity) {
		rsocket.basicAuthentication(Customizer.withDefaults())  // 1
			.simpleAuthentication(Customizer.withDefaults())    // 2
			.authorizePayload((authz) -> authz                  // 3
				.setup().authenticated()                        // 4
				.anyRequest().authenticated()                   // 5
				.matcher((e) -> MatchResult.match()).permitAll() // 6
			);
    //...
}
```

The `authorizePayload` method decides how we can apply authorization at the server setup and request, and thus is something you will want to customize. The default operations we see configured above include:

1) Basic credential passing for backwards compatability; this is deprecated.
2) [Simple](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md) credential passing is supported by default; this is the winning spec and superceeds Basic.
3) Access control rules that specifies which operations must be authenticated before being granted access to the server. 
4) ensures `setup` operations happen with authentication.
5) Any request operation requires authentication.
6) Any other operation is permitted regardless of authentication.

> **_Request Vs Setup:_** Spring Security defines any `request` operation one of the following; FIRE_AND_FORGET, REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL and METADATA_PUSH. SETUP and PAYLOAD types are considered `setup` operations.

### Spring Security User()s

Spring Security provides concrete [User](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/User.java) objects that implement the [UserDetail](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/UserDetails.java) interface. This interface is used internally and shold be subclassed when you have specific needs. The [User.UserBuilder](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/User.java#L215) object provides a fluent builder for describing instances of UserDetail.

Spring Security comes with components to handle UserDetail storage. This activity is exposed for Reactive services, through [ReactiveUserDetailsService](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/ReactiveUserDetailsService.java). The easiest way to use this is by creating an instance of the in-memory [MapReactiveUserDetailService](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/MapReactiveUserDetailsService.java).

To review, we can completely populate a ReactiveUserDetailService:

```kotlin
    @Bean
    open fun userDetailService(): ReactiveUserDetailsService =
            MapReactiveUserDetailsService(
                    User.builder()
                            .username("plumber")
                            .password("{noop}nopassword")   // 1
                            .roles("SHAKE")
                            .build(),
                    User.builder()
                            .username("gardner")
                            .password("{noop}superuser")
                            .roles("RAKE", "LOGIN")
                            .build()
            )
```

in this example, we note that:

 1) The builder supports the algorithm hint using curly braces. Here we specify `noop` (plaintext) password encoding. In the background, Spring Security uses an [DelegatingPasswordEncoder]() to determine the proper encoder to use such as pbkdf2, scrypt, sha256, etc... Please do not use plaintext `{noop}` in production!

### Security in Reactive Streams

With the usage of `@EnableReactiveMethodSecurity` in our main class, we gained the ability to annotate reactive streams with rules for authorization. This happens mainliy in the [ReactiveAuthorizationManager](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/authorization/ReactiveAuthorizationManager.java) instances for specific use cases. Out of the box, we get the support for a variety of expressions with [@PreAuthorize](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/access/prepost/PostAuthorize.java) to introspect the authenticated user for necessary privileges. There are a variety of built-in expressions that we can use. 

Here are built-in expressions supported as defined in [SecurityExpressionOperations](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/access/expression/SecurityExpressionOperations.java) and described in [the Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html):

| Expression | Description |
|------------|-------------|
|hasRole(role: String) | ... |
|hasAnyRole(vararg roles: String)|	Returns true if the current principal has any of the supplied roles (given as a comma-separated list of strings)|
|hasAuthority(authority: String)|Returns true if the current principal has the specified authority. For example, `hasAuthority('read')`|
|hasAnyAuthority(vararg authorities: String)|Returns true if the current principal has any of the supplied authorities (given as a comma-separated list of strings) For example, `hasAnyAuthority('read', 'write')`|
|principal|	Allows direct access to the principal object representing the current user|
|authentication|	Allows direct access to the current Authentication object obtained from the SecurityContext|
|permitAll	|Always evaluates to true|
|denyAll|	Always evaluates to false|
|isAnonymous()|	Returns true if the current principal is an anonymous user|
|isRememberMe()|	Returns true if the current principal is a remember-me user|
|isAuthenticated()|	Returns true if the user is not anonymous|
|isFullyAuthenticated()|	Returns true if the user is not an anonymous or a remember-me user|
|hasPermission(target: Any, permission: Any)| Returns true if the user has access to the provided target for the given permission. For example, `hasPermission(domainObject, 'read')`|
|hasPermission(targetId: Any, targetType: String, permission: Any)|Returns true if the user has access to the provided target for the given permission. For example, `hasPermission(1, 'com.example.domain.Message', 'read'`)|

To gain fundamental understanding of Authorization, I encourage you to read the [Spring Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html). This documentation is robust and does well in describing exactly how Authorization operates under the hood - especially for situations where you have legacy framework code and want to customize.

> **_CURRENTLY_**: For custom expressions, Spring Security supports return values of `boolean` and cannot be wrapped in deferred values such as a reactive `Publisher`. As such, the expressions must not block.

## Secure the Client

Spring RSocket creates a [RSocketRequesterBuilder](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketRequester.java#L164) bean at startup. This bean provides a builder for creating new [RSocketRequesters](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketRequester.java). An `RSocketRequester` provides a single connection interface to RSocket operations usually across a network.

RSocket Security can be applied to connection level or request level. It is up to how your application uses the connection. If a connection is shared across multiple [Principal](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Principal.html) (UserDetails descend from this) then it is recommended to authenticate the setup with it's own 'connectivity' user, or to connect with a 'setup' user, then each request as another user. We will discuss both methods below.  

### Securing at connection time

We can secure each connection by sending metadata in the SETUP frame. To configure this kind of authentication, use a `RSocketRequester.Builder` builder. This builder lets us specify `setupMetadata` to enable our setup frame to contain our user credentials.

Our custom `RequestFactory` class makes it so we dont repeat the connection builder every time a requester is needed. We either need an authenticated connection or a non-authenticated connection. We will create the authenticating Requester below:

```kotlin
open class RequesterFactory(private val port: String) {
        companion object {
        val SIMPLE_AUTH = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string) // 1
    }
    open fun authenticatedRequester(username: String, password: String): RSocketRequester =
            RSocketRequester
                    .builder()
                    .rsocketStrategies { strategiesBuilder ->
                        strategiesBuilder.encoder(SimpleAuthenticationEncoder())
                    } // 2
                    .setupMetadata(UsernamePasswordMetadata(username, password), SIMPLE_AUTH) //3
                    .connectTcp("localhost", port.toInt())
                    .block()!!
 //..
}    
```
The lines of code we want to inspect here relate to the specifics for setup frame authentication metadata:

1) Requester needs to know how to encode our `SIMPLE` authentication metadata.
2) Which needs to be registered as an encoder in Spring's [RSocketStrategies](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketStrategies.java).
3) Then use `setupMetadata` to encode credentials going into the setup frame.

Next, need a non-authenticated setup requester:

```kotlin
    open fun requester(): RSocketRequester =
            RSocketRequester
                    .builder()
                    .rsocketStrategies { strategiesBuilder ->
                        strategiesBuilder.encoder(SimpleAuthenticationEncoder())
                    }
                    .connectTcp("localhost", port.toInt())
                    .block()!!
```

We need to keep the strategy encoder for `Simple` authentication so that we can still send authenticated requests at request time. Other than that, nothing else is different.

Next, we can create some tests to demonstrate connectivity and test whether our configuration is valid.

### Testing the client and server

The first thing we want to is test whether authenticated connections are truely secure by ensuring proper rejection of a un-authenticated setup. This listing, we will look at the options chosen in this test case:

```kotlin
@SpringBootTest         // 1
class RequesterFactoryTests {
    @Test
    fun `no setup authentication is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()    // 2

        val request = requester
                .route("status").retrieveMono<String>() // 3

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)  //4
    }

```
Whats happening is a usual test setup, but lets inspect what our test means.

1) Using `@SpringBootTest` ensures we get full autowiring of our production code to setup the RSocket server.
2) Create a requester that omits setup authentication metadata.
3) The test site is simple and merely sends a request to the `status` route that returns whether we are authenticated or not.
4) Because our server configuration states that setup must be authenticated, we should expect a [RejectedSetupExeption](https://github.com/rsocket/rsocket-java/blob/master/rsocket-core/src/main/java/io/rsocket/exceptions/RejectedSetupException.java) error upon request.

Next, we will test when we send authenticated requests without autheticating setup:

```kotlin
    @Test
    fun `sends credential metadata in request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()

        val request = requester
                .route("status")
                .metadata(UsernamePasswordMetadata("shaker", "nopassword"), RequesterFactory.SIMPLE_AUTH) // 1
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)    // 2
    }
```

This test case is very similar to the previous one except:

1) We only authenticate the request with simple authentication. 
2) This wont work, and will result with RejectedSetupException since our server expects authentication in the `setup` payload.

### Authorization in tests

Next, we will test for proper setup authentication and to check that our `@PreAuthorize` rules are functioning. Recall earlier we have a [TreeServiceSecurity]() class that adds `@PreAuthorize` to our service methods. Lets 
make a request without having sufficient privileges:

```kotlin
    @Test
    fun `underprivileged shake request is APPLICATIONERROR Denied`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory.requester("raker", "nopassword") //1
                .route("shake")  // 2
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(ApplicationErrorException::class.java) //3
    }
```

This test will:

1) create the authenticated requester. But this user is the 'raker' and does not have 'shake' authority.
2) sends a request to the 'shake' route. This route is `@PreAuthorized` protected for users having 'shake' authority.
3) Since we dont have this kind of permission for the 'raker' user, we will get [ApplicationErrorException](https://github.com/rsocket/rsocket-java/blob/master/rsocket-core/src/main/java/io/rsocket/exceptions/ApplicationErrorException.java) with the message 'Denied'.

Although this demo was on Security, it would be wise to further securing of the demo by using TLS security across the transport. This way, noone can snoop the network for credential payloads.

## Closing

This guide was meant to introduce you to Spring Boot, Kotlin, and Spring Security. One key takeaway, that Spring Security configuration can allow simple or complex authentication schemes. Understanding how permissions work out of the box in Spring Security, and applying authorization to Reactive Methods. Then next iteration will utilize a increasingly used authentication strategy - JWT. 

## Informational and Learning Material

Ben Wilcock's [Getting Started to RSocket Security](https://spring.io/blog/2020/06/17/getting-started-with-rsocket-spring-security)

[Going coroutine with Reactive Spring Boot](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

[Spring Security Reference](https://docs.spring.io/spring-security/reference/)

[Spring Shell Reference](https://docs.spring.io/spring-shell/docs/2.1.0/site/reference/htmlsingle/#_what_is_spring_shell)

[Building WebApps with Spring-Boot and Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

[JWT RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)

[XACML for when RBAC is not enough](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml)