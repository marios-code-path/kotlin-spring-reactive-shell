package com.example.telephone.domain

import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
data class User(val name: String)