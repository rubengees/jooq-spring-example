package org.example.jooqspringexample

import org.example.tables.pojos.CountryPojo
import org.example.tables.records.CountryRecord
import org.example.tables.references.ACTOR
import org.example.tables.references.FILM
import org.example.tables.references.FILM_ACTOR
import org.jooq.DSLContext
import org.jooq.Records.mapping
import org.jooq.impl.DSL.concat
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.`val`
import org.springframework.stereotype.Service

@Service
class ExampleService(private val dsl: DSLContext) {

    fun getFilmDescriptionByTitle(title: String): String? {
        return dsl.select(FILM.DESCRIPTION)
            .from(FILM)
            .where(FILM.TITLE.eq(title))
            .fetchOne { it.getValue(FILM.DESCRIPTION) }
    }

    fun getFilmActorNamesByTitle(title: String): List<String> {
        val fullName = concat(ACTOR.FIRST_NAME, `val`(" "), ACTOR.LAST_NAME).`as`("full_name")

        return dsl.select(fullName)
            .from(FILM)
            .innerJoin(FILM_ACTOR).on(FILM.FILM_ID.eq(FILM_ACTOR.FILM_ID))
            .innerJoin(ACTOR).on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID))
            .where(FILM.TITLE.eq(title))
            .orderBy(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
            .fetch { it.getValue(fullName) }
    }

    fun insertCountry(country: CountryPojo) {
        dsl.executeInsert(CountryRecord(country))
    }

    fun increaseRentalRate(by: Number) {
        dsl.update(FILM)
            .set(FILM.RENTAL_RATE, FILM.RENTAL_RATE.plus(by))
            .execute()
    }

    fun countFilmsWithTrailersByYear(): List<YearCount> {
        return dsl.select(FILM.RELEASE_YEAR.asNonNullField(), count())
            .from(FILM)
            .where(FILM.SPECIAL_FEATURES.contains(arrayOf("Trailers")))
            .groupBy(FILM.RELEASE_YEAR)
            .orderBy(FILM.RELEASE_YEAR)
            .fetch(mapping(::YearCount))
    }
}
