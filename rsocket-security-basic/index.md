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
title: Implementing Secure and user-aware RSocket services with Spring Boot
oldPath: "/content/guides/spring/reactive-rsocket-security-spring-boot-pt1.md"
aliases:
- "/guides/spring/reactive-distributed-tracing"
level1: Building Modern Applications
level2: Frameworks and Languages
---

This guide will discuss RSocket service security with Spring Boot, by way of Spring Security. We will surface endpoints that are locked down by Spring Security's authorization as well as implement simple authentication for the connection and request scopes. This guide will inform you of the additional configuration options provided when configuring for Spring Security on a Spring Boot 2.7.x/RSocket application. 

## Motivation

Writing an RSocket application is pretty simple. Securing that app can be simple, but you may now already know there are a few areas to tighen down when it comes to this topic. This example is designed to help you understand where to integrate security in your app. We will cover both authentication and authorization aspects. There are a number of strategies engage Authentication such as JWT, Kerberos, Password and more. This guide will use the simple strategy (username/password) that is configurable out of the box with Spring RSocket services.

It is assumed we want our applications to respond to a user's privilege level; as multi-user applications tend to be specific with regards to feature availability. What emerges through Spring Security, is Role Based Access Control - the ability to make privilege specific logic feasable and with minimal boilerplate.

### Authorization vs Authentication

Authentication is the process which lets our apps identify a user. Authentication schemes are methods which describe a secure process of identification. Some earlier ones are password, while currently OAuth is all the rage.

Authorization (access control) is the process which lets your application determine what a user has access to. This begins to sound straight forward, but can be surfaced in our application in a number of ways. One such way is Role Based Access Control - in which a user may have granted privileges given by role 'names' - e.g. 'WRITE', 'READ' for a given resource. Additionally, RBAC relies on the application to make these decisions (as governed through @Secured annotations).

This guide assumes RBAC as the choice strategy for authorization.

## The Application

The [Example app](https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.7.3&packaging=jar&jvmVersion=17&groupId=example&artifactId=rsocket-security&name=rsocket-security&description=Reactive%20RSocket%20Security%20Demo&packageName=example.rsocket.security&dependencies=rsocket,security,spring-shell) is a simple service containing 2 methods for sending streams of Strings.

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

We have 2 functions and a static list that 
1) returns a Mono of leaf colors.
2) returns a Flux of leaf colors.
3) The list of leaf colors.

We can implement this by doing the actual work for return our Mono/Flux streams of Strings:

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

And add another interface for RSocket Controller mappings:

```kotlin
interface TreeControllerMapping : TreeService {
    @MessageMapping("shake")
    override fun shakeForLeaf(): Mono<String>

    @MessageMapping("rake")
    override fun rakeForLeaves(): Flux<String>
}
```

We will simply apply authorization to our service with a well placed `@PreAuthorize` expression. We can create an interface as configuration for Spring Security annotations on our example service:

```kotlin
interface TreeServiceSecurity : TreeService {

    @PreAuthorize("hasRole('SHAKE')")
    override fun shakeForLeaf(): Mono<String>

    @PreAuthorize("hasRole('RAKE')")
    override fun rakeForLeaves(): Flux<String>
}
```
Secure the services with what level of authorization your application requires. In the above example,
we use [@PreAuthorize]() which is the method for securing reactive streams through annotation.

### Putting the App together

The production appliation will configure security rules, build the exposed services, and provide other support beans that we will discuss later on. In the next listing, we will look at security related annotations and our service configuration. The rest will be discussed as we move on.

```kotlin
@EnableReactiveMethodSecurity  // 1
@EnableRSocketSecurity // 2
@SpringBootApplication
class App {

 // ...

    @Controller
    class ServerTreeController : TreeControllerMapping,
            TreeServiceSecurity, TreeService by TreeServiceImpl() {  // 3
        @MessageMapping("status")
        fun status(): Mono<Boolean> = Mono.just(true)
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

1) The first thing to do in securing an RSocket app is to enable security specific to RSocket itself, the [RSocketSecurity]() bean. This is done by declaring @[EnableRSocketSecurity]() onto the main configuration class. What this does is as stated in documentation -  it allows configuring RSocket based security. 
2) Secondly, enabling security on our streams in service methods is another concern.  Thus, to enable the usage of Spring's own method security annotations, add the @[EnableReactiveMethodSecurity]() annotation to the main configuraiton class. At a deper level, this configures Reactive Publishers Hooks to decorate our streams with pre and post advices that access [SecurityContext]() object. 
3) The controller is fully configured here, along with an un-secure `status` route. The status route should only tell us that we are authenticated by returning a boolean.

### Configure RSocket Security 

With `@EnableRSocketSecurity`, we gain RSocket security through [RSocket Interceptors](). Interceptors have the ability to work during, before or after a scope in processing. For RSocket, this means any of the following levels:

* Transport level
* At the level of accepting new connections
* Performing requests
* Responding to requests

Since a payload can have many metadata formats to confer credential exchange, Spring's [RSocketSecurity]() bean provides a fluent DSL for configuring Simple, Basic, JWT, and custom authentication methods at these level, in addition to application-specific RBAC settings. This DSL ultimately describe a set of [AuthenticationPayloadInterceptor]()'s that converts payload metdata into an [Authentication]() instances at runtime.

Lets look at the [SecuritySocketAcceptorInterceptorConfiguration]() class that sets up default security configuration which we will need for this demo. This class, imported by `@EnableRSocketSecurty`, will configure a [PayloadSocketAcceptorInterceptor]() using RSocketSecurity's DSL:

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

The `authorizePayload` method decides how we can access the server, and thus is something you will want to customize. The operations we see configure above include:

1) Basic credential passing for backwards compatability; for a time, before Simple was accepted as the leading spec.
2) [Simple](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md) credential passing is supported by default; this is the winning spec and superceeds Basic.
3) Access control rules that specifies which operations must be authenticated before being granted access to the server. 
4) ensures `setup` operations happen with authentication
5) Any request operation requires authentication
6) Any other operation is permitted regardless of authentication

> **_Request Vs Setup:_** Spring Security defines any `request` operation one of the following; FIRE_AND_FORGET, REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL and METADATA_PUSH. SETUP and PAYLOAD types are considered `setup` operations.

### Spring Security User()s

Spring Security provides concrete [User]() objects that implement the [UserDetail]() interface. This interface is used internally and shold be subclassed when you have specific needs. The [User.UserBuilder]() object provides a fluent DSL for describing instances of UserDetail.

Spring Security comes with components to handle UserDetail storage. This activity is exposed for Reactive services, through [ReactiveUserDetailsService](). The easiest way to use this is by creating an instance of the in-memory [MapReactiveUserDetailService]() provided out of the box.

The next sample shows the use of these components in action:

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

Sometimes you want to select a specific encryption algorithm when specifying a password programatically such as above. However, this is specified by:

 1) specify `noop` (plaintext) password encoding using curly braces and the encoding name `{noop}` or otherwise named encryption algorithm. In the background, Spring Security uses an [DelegatingPasswordEncoder]() to determine the proper encoder to use such as pbkdf2, scrypt, sha256, etc...

### Security in Reactive Streams

With the usage of `@EnableReactiveMethodSecurity` in our main class, we gained the ability to annotate reactive streams with rules for authorization. This means we can use [@PreAuthorize]() to introspect the authenticated user for necessary credentials. There are a variety of built-in expressions that we can use. 

> **_CURRENTLY_**: For custom expressions, Spring Security supports return values of `boolean` and cannot be wrapped in deferred values such as a reactive `Publisher`. As such, the expressions must not block.

Here are built-in expressions supported as defined in [SecurityExpressionOperations]() and described in [the Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html):

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

By default, you will have little interaction with a [ReactiveAuthorizationManager]() or any [AuthorizationManager]()'s which computes authorization where applied.  To use Authorization with Reactive streams, use [@PreAuthorize]() annotations that let us express authorization requiremts.

## Secure the Client

Spring RSocket creates a [RSocketRequesterBuilder]() bean at startup. This bean provides a builder for creating new [RSocketRequesters](). An `RSocketRequester` provides a single connection interface to RSocket operations usually across a network.

RSocket Security can be applied to connection level or request level. It is up to how your application uses the connection. If a connection is shared across multiple [Principal]() (Users) then it is recommended to authenticate the setup with it's own 'connectivity' user, or to connect normally, and secure each request. We will discuss both methods below.  

### Securing at connection time

We can secure each connection by sending metadata in the SETUP frame. To configure this kind of authentication, use a [RSocketRequester.Builder]() builder. This builder lets us specify `setupMetadata`
to enable our setup frame to contain our user credentials.

A RequestFactory class that does this job can be made so we dont repeat the connection builder every time a requester is needed. We will create the authenticating Requester below:

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

1) Requester needs to know how to encode our `SIMPLE` v0 authentication metadata
2) Which needs to be registered as an encoder in Spring's [RSocketStrategies]()
3) Provide the credentials going into setup with the `setupMetdata` method on supported `RSocketbuilder.Builder`s

Additionally, we may not want to secure the SETUP portion. Lets create a method that returns a authentication-less requester.

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

The first thing we want to is test whether authenticated connections are truely secure by ensuring proper rejection of non setup metadata laden requests.  This demo inclues a test class called `RequesterFactoryTests` because we're testing  `RequesterFactory` as the factory of `RSocketRequester`.

Lets take a look at how we configure and test the first case where the setup is NOT authenticated:

```kotlin
@SpringBootTest         // 1
class RequesterFactoryTests {
    @Test
    fun `no setup metadata request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()    // 2

        val request = requester
                .route("status").retrieveMono<String>() // 3

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)  //4
    }

```
Whats happening is a usual test setup, but lets inspect what our test means.

1) Using `@SpringBootTest` ensures we get full autowiring of our production code to setup the server.
2) Issue a requester that omits setup authentication metadata.
3) The test site is simple and merely sends a request to the `status` route that returns whether we are authenticated or not.
4) Because our server configuration states that setup must be authenticated, we should expect a [RejectedSetupExeption] error upon request.

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

This test case is very similar to the previous one with one exception:

1) We only added the metadata line that tells `RSocketRequester` to send authentication metadata along with the request. 
2) This wont work, and will result with RejectedSetupException since our server expects authentication in the SETUP payload.

### Authorization in tests

Next, we will test for proper setup authentication and that roles are truely locked down on our server routes. Recall earlier we have a [TreeServiceSecurity]() class that adds `@PreAuthorize` to our service methods. Lets 
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

This test issues a requester with authentication setup payloads, so the factory method includes username and password. Dont do this in production. It's better to use a secrets store or otherwise safe credential holder.

1) issues the authenticated requester, this user is the 'raker' user and does not have 'shake' authority.
2) sends a request to the 'shake' route. This route is @PreAuthorized protected for users having 'shake' authority.
3) Since we dont have this kind of role for the 'raker' user, we will get [ApplicationErrorException]() with the message 'Denied'.

## Summary

## Next Steps

## Informational and Learning Material

Ben Wilcock's [Getting Started to RSocket Security](https://spring.io/blog/2020/06/17/getting-started-with-rsocket-spring-security)

[Going coroutine with Reactive Spring Boot](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

[Spring Security Reference](https://docs.spring.io/spring-security/reference/)

[Spring Shell Reference](https://docs.spring.io/spring-shell/docs/2.1.0/site/reference/htmlsingle/#_what_is_spring_shell)

[Building WebApps with Spring-Boot and Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

[JWT RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)

[XACML for when RBAC is not enough](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml)