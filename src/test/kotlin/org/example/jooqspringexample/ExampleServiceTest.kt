package org.example.jooqspringexample

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode
import org.example.tables.pojos.CountryPojo
import org.example.tables.references.COUNTRY
import org.example.tables.references.FILM
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureEmbeddedDatabase(refresh = RefreshMode.AFTER_EACH_TEST_METHOD)
class ExampleServiceTest {

    @Autowired
    private lateinit var service: ExampleService

    @Autowired
    private lateinit var dsl: DSLContext

    @Test
    @Disabled
    fun `should get film description by title`() {
        val result = service.getFilmDescriptionByTitle("KING EVOLUTION")

        assertEquals("A Action-Packed Tale of a Boy And a Lumberjack who must Chase a Madman in A Baloon", result)
    }

    @Test
    @Disabled
    fun `should get film actor names by title (ordered by firstName, lastName)`() {
        val result = service.getFilmActorNamesByTitle("ALIEN CENTER")

        assertEquals(
            listOf("BURT DUKAKIS", "HUMPHREY WILLIS", "KENNETH PALTROW", "MENA HOPPER", "RENEE TRACY", "SIDNEY CROWE"),
            result
        )
    }

    @Test
    @Disabled
    fun `should insert country`() {
        service.insertCountry(CountryPojo(12345, "XYZ"))

        val result = dsl.select(COUNTRY.COUNTRY_)
            .from(COUNTRY)
            .where(COUNTRY.COUNTRY_ID.eq(12345))
            .fetchOne { it.getValue(COUNTRY.COUNTRY_) }

        assertEquals("XYZ", result)
    }

    @Test
    @Disabled
    fun `should increase rental rate`() {
        service.increaseRentalRate(2)

        assertEquals(2.99.toBigDecimal(), getRentalRate(1))
        assertEquals(6.99.toBigDecimal(), getRentalRate(2))
    }

    private fun getRentalRate(id: Int) = dsl.select(FILM.RENTAL_RATE)
        .from(FILM)
        .where(FILM.FILM_ID.eq(id))
        .fetchOne { it.getValue(FILM.RENTAL_RATE) }

    @Test
    @Disabled
    fun `should count films with trailers by year (sorted by year)`() {
        val result = service.countFilmsWithTrailersByYear()

        assertEquals(
            listOf(
                YearCount(2006, 30),
                YearCount(2007, 33),
                YearCount(2008, 23),
                YearCount(2009, 26),
                YearCount(2010, 24),
                YearCount(2011, 29),
                YearCount(2012, 26),
                YearCount(2013, 39),
                YearCount(2014, 19),
                YearCount(2015, 24),
                YearCount(2016, 21),
                YearCount(2017, 28),
                YearCount(2018, 28),
                YearCount(2019, 34),
                YearCount(2020, 29),
                YearCount(2021, 32),
                YearCount(2022, 35),
                YearCount(2023, 31),
                YearCount(2024, 24),
            ),
            result,
        )
    }
}
