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
title: Implementing Secure RSocket services with Spring Boot
oldPath: "/content/guides/spring/reactive-rsocket-security-spring-boot-pt1.md"
aliases:
- "/guides/spring/reactive-distributed-tracing"
level1: Building Modern Applications
level2: Frameworks and Languages
---

This guide will discuss RSocket service security with Spring Boot, by way of Spring Security. We will surface RSocket routes that enforce specific security measures and describe what this means internally. This guide will inform you of the additional configuration options provided when configuring for Spring Security on a Spring Boot 2.7.x/RSocket application.

It is assumed the developer knows Kotlin, uses Spring Boot, and has an understanding of the [Reactive Streams](https://github.com/reactive-streams/reactive-streams-jvm) on the JVM. If you're new to Spring Security for Reactive streams, then this guide should help shed light on the subject. Of course, the best place to understand are [The Guides](https://docs.spring.io/spring-security/reference/5.6.5/reactive/integrations/rsocket.html#page-title), so please read them!

## Motivation

Writing secure RSocket apps with Spring Boot application is not hard and takes just a few lines of code. But you may already know that security is a broad and widely discussed topic. This example is designed to help you to quickly understand the basics of integrating Spring Security into your Reactive / RSocket application. We will cover the authentication and authorization aspects and how they are applied within Spring. There are a number of strategies to authenticate with such as JWT, Kerberos, and username/password to name a few. This guide will focus on the `simple` username/password strategy.

We want our applications to respond to a user's privilege level; as multi-user applications tend to be specific with regards to feature availability. What emerges through Spring Security, is Role Based Access Control - the ability to make privilege (role) specific logic feasable and with minimal boilerplate.

## Authorization vs Authentication

[Authentication](https://docs.spring.io/spring-security/reference/features/authentication/index.html) is the process which lets our apps identify a user. Authentication schemes are methods which describe a secure process of identification. For example, you already have logged into a multitude of systems over the course of the past day (or even hour!). Simple authentication systems usually only care about a username and password (and maybe a dash of network addresses). Even more complex ones require some form of authenticative credential from another trusted party as in a multi-factor systems you might be familiar with such as OAuth.

[Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html) (access control) is the process which lets your application determine how access is granted to users. This begins to sound straight forward, but can be surfaced in our application in a number of ways. One such way is Role Based Access Control - in which a user may have granted privileges given by role 'names' - e.g. 'WRITE', 'READ' for a given resource. Additionally, RBAC relies on the application to make these decisions as you will see later in this guide.

## The Application

The [Example app](https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.7.3&packaging=jar&jvmVersion=17&groupId=example&artifactId=rsocket-security&name=rsocket-security&description=Reactive%20RSocket%20Security%20Demo&packageName=example.rsocket.security&dependencies=rsocket,security,spring-shell) is a RSocket Messaging service containing 2 privileged routes, 1 un-privilege 'status' route will also get explored.

The service interface is as follows:

```kotlin
interface TreeService {
    fun shakeForLeaf(): Mono<String> // 1
    fun rakeForLeaves(): Flux<String> // 2

    companion object {
        val LEAF_COLORS = listOf("Green", "Yellow", "Orange", "Brown", "Red")
    }
}
```

Above, we have 2 functions and a static list that:

1) Return a `Mono<String>` of leaf colors.
2) Return a `Flux<String>` of leaf colors.

We can then write the backing implementation:

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

Subclass the service interface to create the RSocket controller using [@MessageMapping](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/handler/annotation/MessageMapping.java):

```kotlin
interface TreeControllerMapping : TreeService {
    @MessageMapping("shake")
    override fun shakeForLeaf(): Mono<String>

    @MessageMapping("rake")
    override fun rakeForLeaves(): Flux<String>
}
```

Next, subclass our service once more and apply Spring Security annotations. Use [@PreAuthorize](https://docs.spring.io/spring-security/reference/5.6.8/servlet/authorization/expression-based.html#_access_control_using_preauthorize_and_postauthorize), which is the preferred way for securing reactive streams through annotation.

```kotlin
interface TreeServiceSecurity : TreeService {

    @PreAuthorize("hasRole('SHAKE')")
    override fun shakeForLeaf(): Mono<String>

    @PreAuthorize("hasRole('RAKE')")
    override fun rakeForLeaves(): Flux<String>
}
```

Finally, we can put the whole thing together and expose it as an RSocket server with help from Spring Boot!

### Putting the App together

The production application will merge security rules, messaging routes and backing implementation. Using Spring Security's supp and provide a user database where run-time authentication and authorization can be derived. In the next listing, we will look at enabling Spring Security for RSocket services.

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

Spring Security uses a [ReactiveSecurityContextHolder](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/context/ReactiveSecurityContextHolder.html) to place the [SecurityContext](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/context/SecurityContext.html) into [Reactor's Context](https://projectreactor.io/docs/core/release/reference/#context).

Since reactive operators get access to this `SecurityContext`, Spring Security (or the developer) can then wrap logic within advices to determine things like the current logged in user and it's privileges. The way to enable this goes as follows:

1) Enabled the [RSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/RSocketSecurity.java) bean by decorating a configuration class with [@EnableRSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/EnableRSocketSecurity.java). What this does is as stated in documentation -  it allows configuring RSocket based security. 
2) Enable security-specific annotations on Reactive Streams (return types of [Publisher](https://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/org/reactivestreams/Publisher.html?is-external=true)), add the @[EnableReactiveMethodSecurity](https://github.com/spring-projects/spring-security/blob/210693eb6bd0cba51874ce150c73090c95d4e08b/docs/modules/ROOT/pages/reactive/authorization/method.adoc) annotation to the main configuraiton class. 
3) The RSocket messaging controller is fully configured here, along with the third un-secure `status` route. The status route uses [@AuthenticationPrincipal](https://github.com/spring-projects/spring-security/blob/main/web/src/main/java/org/springframework/security/web/bind/annotation/AuthenticationPrincipal.java) to inject - if available - the UserDetails object from the afformentioned `SecurityContext`.

> **_Customize the User:_** There is a nice to know informal example that describes how one would resolve a custom User object with the [AuthenticationPrincipalArgumentResolver](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/bind/support/AuthenticationPrincipalArgumentResolver.html).

## Application Users

Spring Security provides concrete [User](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/User.java) objects that implement the [UserDetail](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/UserDetails.java) interface. This interface is used internally and shold be subclassed when you have specific needs. The [User.UserBuilder](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/User.java#L215) object provides a fluent builder for describing instances of UserDetail.

Spring Security comes with components to handle UserDetail storage. This activity is exposed for Reactive services, through [ReactiveUserDetailsService](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/ReactiveUserDetailsService.java). The easiest way to use this is by creating an instance of the in-memory [MapReactiveUserDetailService](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/userdetails/MapReactiveUserDetailsService.java).

To review, we can completely populate a `ReactiveUserDetailService` in our production app:

```kotlin
class App {
    // ...
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
}
```

The above code sample reads well, but there is some nuance with password setup:

 1) The builder supports the algorithm hint using curly braces. Here we specify `noop` (plaintext) password encoding. In the background, Spring Security uses an [DelegatingPasswordEncoder](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/password/DelegatingPasswordEncoder.html) to determine the proper encoder to use such as pbkdf2, scrypt, sha256, etc...

> **_WARNING:_**  Please do not use plaintext `{noop}` in production!
### Review of RSocket Server Security

By using `@EnableRSocketSecurity`, we gain RSocket security through [Payload Interceptors](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/rsocket/api/PayloadInterceptor.html). Interceptors themselves are cross-cutting, and Spring Security uses them to work on processing at various parts of an interaction such as:

* Transport level
* At the level of accepting new connections
* Performing requests
* Responding to requests

Since a payload can have many metadata formats to confer credential exchange, Spring's [RSocketSecurity](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/RSocketSecurity.java) bean provides a fluent builder for configuring [Simple](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md), Basic, JWT, and custom authentication methods, in addition to RBAC authorization.

The `RSocketSecurity` provided builder will describe a set of [AuthenticationPayloadInterceptor](https://github.com/spring-projects/spring-security/blob/main/rsocket/src/main/java/org/springframework/security/rsocket/authentication/AuthenticationPayloadInterceptor.java)'s that converts payload metdata into an [Authentication](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/Authentication.java) instances inside the `SecurityContext`. 

To further our understanding of the configuration, lets examine the [SecuritySocketAcceptorInterceptorConfiguration](https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/rsocket/SecuritySocketAcceptorInterceptorConfiguration.java) class, which sets up the default security configuration for RSocket. This class, imported by `@EnableRSocketSecurty`, will configure a [PayloadSocketAcceptorInterceptor](https://github.com/spring-projects/spring-security/blob/main/rsocket/src/main/java/org/springframework/security/rsocket/core/PayloadSocketAcceptorInterceptor.java) for sim[Simple](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md)ple and (deprecated) basic authentications, while requiring authentication for requests:

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

1) Basic credential passing for backwards compatability; this is deprecated in favor of #2
2) [Simple](https://github.com/rsocket/rsocket/blob/master/Extensions/Security/Simple.md) credential passing is supported by default; this is the winning spec and superceeds Basic.
3) Access control rules that specifies which operations must be authenticated before being granted access to the server. 
4) Ensures `setup` operations requre authentication frame data.
5) Any request operation requires authentication.
6) Any other (not disclosed above) operation is permitted regardless of authentication.

> **_Request Vs Setup:_** Spring Security defines any `request` operation one of the following; FIRE_AND_FORGET, REQUEST_RESPONSE, REQUEST_STREAM, REQUEST_CHANNEL and METADATA_PUSH. SETUP and PAYLOAD types are considered `setup` operations.

### Security in Reactive Streams

With the usage of `@EnableReactiveMethodSecurity` in our main class, we gained the ability to annotate reactive streams with rules for authorization. This happens mainliy in the [ReactiveAuthorizationManager](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/authorization/ReactiveAuthorizationManager.java) instances for specific use cases. Out of the box, we get the support for a variety of expressions with [@PreAuthorize](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/access/prepost/PostAuthorize.java) to introspect the authenticated user for necessary privileges. There are a variety of built-in expressions that we can use. 

Here are built-in expressions supported as defined in [SecurityExpressionOperations](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/access/expression/SecurityExpressionOperations.java) and described in [the Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html):

| Expression | Description |
|------------|-------------|
|hasRole(role: String) | Returns true if the current principal has the specified role.
For example, hasRole('admin') By default if the supplied role does not start with 'ROLE_' it will be added. This can be customized by modifying the defaultRolePrefix on DefaultWebSecurityExpressionHandler. |
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

To gain fundamental understanding of the underpinnings of Authorization, I encourage you to read the [Spring Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html). This documentation is robust and does well in describing exactly how Authorization operates under the hood - especially for situations where you have legacy framework code and want to customize.

> **_CURRENTLY_**: For custom expressions, Spring Security supports return values of `boolean` and cannot be wrapped in deferred values such as a reactive `Publisher`. As such, the expressions must not block.

## Security at the Client

Spring RSocket creates a [RSocketRequesterBuilder](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketRequester.java#L164) bean at startup. This bean provides a builder for creating new [RSocketRequesters](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketRequester.java). An `RSocketRequester` provides a single connection interface to RSocket operations usually across a network.

RSocket Security can be applied at the setup and or request levels. If an connection is shared across multiple users, then it is recommended to authenticate the setup with it's own 'connectivity' user,then each request with its specific user. We will discuss both methods below.  

### Authentication Styles on the Client

We can secure the entire RSocket connection by sending metadata in the [SETUP](https://github.com/rsocket/rsocket/blob/master/Protocol.md#frame-setup) frame. The `RSocketRequester.Builder` builder lets us specify `setupMetadata` that contains authentication metadata.

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

1) Requester needs to know how to encode our `Simple` authentication metadata.
2) Which needs to be registered as an encoder in Spring's [RSocketStrategies](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/rsocket/RSocketStrategies.java).
3) Then use `setupMetadata` to encode credentials going into the setup frame.

Next, we need a non-authenticated setup requester:

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

## Testing the Client and Server

The first thing we want to is test whether authenticated connections are truely secure by ensuring proper rejection of a un-authenticated setup. This listing, we will look at the options chosen in this test case:

```kotlin
@SpringBootTest         // 1
class RequesterFactoryTests {
    @Test
    fun `no setup authentication is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()    // 2

        val request = requester
                .route("status")
                .retrieveMono<String>() // 3

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)  //4
    }

```
Whats happening is a usual test setup, but lets inspect what our test means:

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

1) We only authenticate the request with `Simple` authentication. 
2) This wont work, and will result with RejectedSetupException since our server expects authentication in the `setup` frame.

### Authorization in tests

Next, we will test for authentication and to check that our `@PreAuthorize` rules are functioning. Recall earlier we have a `TreeServiceSecurity` class that adds `@PreAuthorize` to our service methods. Lets test this using a user of insufficent privilege:

```kotlin
    @Test
    fun `underprivileged shake request is APPLICATIONERROR Denied`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory
                .requester("raker", "nopassword") //1
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

> **_NOTE TO FUTURE_**: To ensure safer communication while using `Simple` authentication, you might apply TLS security across the transport. This way, noone can snoop the network for credential payloads.

## Closing and Next Step

This guide was meant to introduce you to Spring Boot and Spring Security with Kotlin. One key takeaway, that Spring Security configuration can allow `Simple` or other authentication schemes such as JWT and Kerberos. Understanding how permissions work out of the box in Spring Security, and applying authorization to Reactive Methods. Then next step on this topic will take advantage of Spring Security's JWT interface. For in-depth implementation details on that topic now, please see the [Spring Security Samples](https://github.com/spring-projects/spring-security-samples) project on Github. 

## Informational and Learning Material

Ben Wilcock's [Getting Started to RSocket Security](https://spring.io/blog/2020/06/17/getting-started-with-rsocket-spring-security)

[Going coroutine with Reactive Spring Boot](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

[Spring Security Reference](https://docs.spring.io/spring-security/reference/)

[Spring Shell Reference](https://docs.spring.io/spring-shell/docs/2.1.0/site/reference/htmlsingle/#_what_is_spring_shell)

[Building WebApps with Spring-Boot and Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

[JWT RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)

[XACML for when RBAC is not enough](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml)