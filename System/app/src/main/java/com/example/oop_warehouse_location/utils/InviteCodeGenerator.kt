package com.example.oop_warehouse_location.utils

object InviteCodeGenerator {

    private const val CODE_LENGTH = 6
    private val CHARSET = ('A'..'Z') + ('0'..'9')


    fun generate(): String {
        return (1..CODE_LENGTH)
            .map { CHARSET.random() }
            .joinToString("")
    }
}
