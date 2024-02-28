package org.example.jooqspringexample

import org.example.tables.pojos.CountryPojo
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class ExampleService(private val dsl: DSLContext) {

    fun getFilmDescriptionByTitle(title: String): String? {
        TODO()
    }

    fun getFilmActorNamesByTitle(title: String): List<String> {
        TODO()
    }

    fun insertCountry(country: CountryPojo) {
        TODO()
    }

    fun increaseRentalRate(by: Number) {
        TODO()
    }

    fun countFilmsWithTrailersByYear(): List<YearCount> {
        TODO()
    }
}
