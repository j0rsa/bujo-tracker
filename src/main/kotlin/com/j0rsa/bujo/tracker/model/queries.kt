package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import java.sql.PreparedStatement
import java.sql.ResultSet

fun <T> String.exec(body: PreparedStatement.() -> Unit, transform: (ResultSet) -> T): ArrayList<T> {
    val statement = currentTransaction().connection.prepareStatement(this).apply(body)
    val result = arrayListOf<T>()
    val resultSet = statement.executeQuery()
    resultSet?.use {
        while (it.next()) {
            result += transform(it)
        }
    }
    return result
}