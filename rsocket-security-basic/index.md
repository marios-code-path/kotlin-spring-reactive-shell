# RSocket Security Based on Kotlin

Writing an RSocket application is pretty simple. Securing that app CAN be simple, but if you know what settings to use. This example is designed to help you understand each kind of authentication strategy that is configurable out of the box with Spring RSocket services.

## Kotlin & Spring

You should re-write and update this reference possibly add to it please 

One of Kotlin’s key features is null-safety - which cleanly deals with null values at compile time rather than bumping into the famous NullPointerException at runtime. This makes applications safer through nullability declarations and expressing "value or no value" semantics without paying the cost of wrappers like Optional. Note that Kotlin allows using functional constructs with nullable values; check out this comprehensive guide to Kotlin null-safety.

Although Java does not allow one to express null-safety in its type-system, Spring Framework provides null-safety of the whole Spring Framework API via tooling-friendly annotations declared in the org.springframework.lang package. By default, types from Java APIs used in Kotlin are recognized as platform types for which null-checks are relaxed. Kotlin support for JSR 305 annotations + Spring nullability annotations provide null-safety for the whole Spring Framework API to Kotlin developers, with the advantage of dealing with null related issues at compile time.

This feature can be enabled by adding the -Xjsr305 compiler flag with the strict options.

Notice also that Kotlin compiler is configured to generate Java 8 bytecode (Java 6 by default).

The [Spread Operator](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs) in Kotlin is another nice feature to have.

> **_NOTE:_** When we call a vararg-function, we can pass arguments one-by-one, e.g. asList(1, 2, 3), or, if we already have an array and want to pass its contents to the function, we use the spread operator (prefix the array with *):

```kotlin
    fun sample(vararg words: String) {  // DONT actually call this.
        sample(*listOf("hello","world").toTypedArray())
    }
```

## The Example

*By Default, Minimum RSocket Security Configuration will require a userDetailService, and that will enable basic/simple Authentication.

The [Example app](https://start.spring.io/#!type=maven-project&language=kotlin&platformVersion=2.7.3&packaging=jar&jvmVersion=17&groupId=example&artifactId=rsocket-security&name=rsocket-security&description=Reactive%20RSocket%20Security%20Demo&packageName=example.rsocket.security&dependencies=rsocket,security,spring-shell) is a simple service that vends [Strings](). Two roles required to either 'shake' a tree or 'rake' for leaves. We will create users that hold these roles, stand up a service and then attach our shell app to let us login and perform 'shake' and 'rake'.


## Informational and Learning Material

Ben Wilcock's [Getting Started to RSocket Security](https://spring.io/blog/2020/06/17/getting-started-with-rsocket-spring-security)

[Going coroutine with Reactive Spring Boot](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

[Spring Security Reference](https://docs.spring.io/spring-security/site/docs/5.0.0.RELEASE/reference/htmlsingle/#jc-erms)

[Spring Shell Reference](https://docs.spring.io/spring-shell/docs/2.1.0/site/reference/htmlsingle/#_what_is_spring_shell)

[Building WebApps with Spring-Boot and Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

[JWT RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)