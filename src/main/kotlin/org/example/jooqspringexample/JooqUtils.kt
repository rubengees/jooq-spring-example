package org.example.jooqspringexample

import org.jooq.Field

/**
 * Converts the type of the field to be non-null. This is not type-safe, but improves the ergonomics for fields we know
 * to be not null.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Field<T?>.asNonNullField() = this as Field<T>
