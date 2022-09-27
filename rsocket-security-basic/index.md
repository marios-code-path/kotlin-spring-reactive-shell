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

## Application Setup - Enabling Security

The [Example app](https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.7.3&packaging=jar&jvmVersion=17&groupId=example&artifactId=rsocket-security&name=rsocket-security&description=Reactive%20RSocket%20Security%20Demo&packageName=example.rsocket.security&dependencies=rsocket,security,spring-shell) is a simple service that sends [Strings](). Two roles required to either 'shake' a tree or 'rake' for leaves. We will create users that hold ONE of these roles, stand up a service and then attach our shell app to let us login and perform 'shake' and 'rake'.

The first thing to do in securing an RSocket app is to enable security specific to RSocket itself, the [RSocketSecurity]() bean. This is done by declaring @[EnableRSocketSecurity]() onto the main configuraiton class. What this does as stated - allows configuring RSocket based security. 

```kotlin
@EnableReactiveMethodSecurity
@EnableRSocketSecurity
@SpringBootApplication
class App { 
    // ...
}
```

Secondly, enabling security on our methods in another concern. Since we can already tell when the RSocket connection is processing security details.  Thus, to enable the usage of JSR-250 and Spring's own method security annotations, add the @[EnableReactiveMethodSecurity]() annotation to the main configuraiton class.

### Configure RSocket Security 

Spring Security applies RSocket security through the standard [RSocket Interceptors](). Interceptors have the ability to work during, before or after a scope in processing. For RSocket, this means any of the following levels:

* Transport level
* At the level of accepting new connections
* Performing requests
* Responding to requests

For accepting new connections level, Spring provides the [SecuritySocketAcceptorInterceptor]() that 

The [RSocketSecurity]() bean provides a nice DSL for configuring Simple, Basic, JWT, and custom authentication methods, in addition to application-specific RBAC settings. 

By default, Spring Security RSocket provides a delegating interceptor called the[SecuritySocketAcceptorInterceptor]() that we provide an interceptor for. The [PayloadSocketAcceptorInterceptor]() interceptor is the primary target for for this, and as such will be 
required in order for Spring Security to function with our RSocket server.

Lets create a class that encapsulates all of the Security cusomizations we will need for this demo. We will configure a simple authentication strategy for our RSocket connections with the [RSocketSecurity]() class:

```kotlin
open class SecurityConfiguration {
    @Bean
    open fun simpleSecurityAuthentication(security: RSocketSecurity)
            : PayloadSocketAcceptorInterceptor = security
            .authorizePayload { spec ->
                spec
                        .anyExchange()
                        .authenticated()
            }
            .simpleAuthentication(Customizer.withDefaults())
            .build()
}
```

Another objective of this class is to configure the [ReactiveAuthenticationManager]() such that Roles and Users are available. In this case, we will insist that any request that is authenticated passes the interception.

*** Explain what this does to our application at startup. How it can be customized, and where users will turn to in order to bring in things like JWT.

### Resolving users at method sites

*** How do users make it to our method calls? 
Add AuthenticationPrincipalArgumentResolver to MessageHandler to resolve UserDetails at secure methods.

### User Stores

The MapReactiveUserDetailsService


Our UserDetails contains just the users for this demo:

```kotlin
open class SecurityConfiguration {
    // ...
    @Bean
    open fun userDetailService(): ReactiveUserDetailsService =
            MapReactiveUserDetailsService(
                    User.builder()
                            .username("plumber")
                            .password("{noop}supermario")
                            .roles("SHAKE")
                            .build(),
                    User.builder()
                            .username("gardner")
                            .password("{noop}superuser")
                            .roles("RAKE")
                            .build()
            )
}
```

If you want a custom version, checkout the `Demo-chat` repository. Usually you would contact a database to provide the users and a secret-store for credentials. That will be covered in another article. But for now lets focus on the simplicity of integrating security into the app.
### Enable Authorization 

In this guide, we will interact with a defualt [AuthorizationManager]() for simplicity's sake. However, to gain fundamental understanding of Authorization, I encourage you to read the [Spring Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html). This documentation is robust and does well in describing exactly how Authorization operates under the hood - especially for situations where you have legacy framework code and want to customize.

By default, you will interact with the [AuthorityAuthorizationManager]() which computes authorization based on a . You can interact with this Authorization by specifying SPeL expressions that call this bean's methods. For example:

```kotlin
@Secured('hasRole("myRole")')
fun myMethodRequiringRole() = Mono.just("Authorized!")
```

#### ON Multi-user Systems

Two options for securiting requesters:

* Secure the entire session
* Secure per request

### Secure the Client

* RSocketConnectionHandler
* RSocketRequester
* RequesterFactory

## Secure the Connection with TLS (SSL v3)

Because we're using BASIC_AUTH ( SimpleAuthentication ) which sends credentials in plain text, we need to ensure that transport is secure. Lets configure TLS for our connection.
## Summary

## Next Steps

## Informational and Learning Material

Ben Wilcock's [Getting Started to RSocket Security](https://spring.io/blog/2020/06/17/getting-started-with-rsocket-spring-security)

[Going coroutine with Reactive Spring Boot](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

[Spring Security Reference](https://docs.spring.io/spring-security/site/docs/5.0.0.RELEASE/reference/htmlsingle/#jc-erms)

[Spring Shell Reference](https://docs.spring.io/spring-shell/docs/2.1.0/site/reference/htmlsingle/#_what_is_spring_shell)

[Building WebApps with Spring-Boot and Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

[JWT RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)

[XACML for when RBAC is not enough](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml)