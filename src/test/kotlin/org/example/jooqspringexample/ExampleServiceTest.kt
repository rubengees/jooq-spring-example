package org.example.jooqspringexample

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode
import org.example.tables.pojos.CountryPojo
import org.example.tables.references.COUNTRY
import org.example.tables.references.FILM
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `should get film description by title`() {
        val result = service.getFilmDescriptionByTitle("KING EVOLUTION")

        assertEquals("A Action-Packed Tale of a Boy And a Lumberjack who must Chase a Madman in A Baloon", result)
    }

    @Test
    fun `should get film actor names by title (ordered by firstName, lastName)`() {
        val result = service.getFilmActorNamesByTitle("ALIEN CENTER")

        assertEquals(
            listOf("BURT DUKAKIS", "HUMPHREY WILLIS", "KENNETH PALTROW", "MENA HOPPER", "RENEE TRACY", "SIDNEY CROWE"),
            result
        )
    }

    @Test
    fun `should insert country`() {
        service.insertCountry(CountryPojo(12345, "XYZ"))

        val result = dsl.select(COUNTRY.COUNTRY_)
            .from(COUNTRY)
            .where(COUNTRY.COUNTRY_ID.eq(12345))
            .fetchOne()
            ?.value1()

        assertEquals("XYZ", result)
    }

    @Test
    fun `should increase rental rate`() {
        service.increaseRentalRate(2)

        val firstRentalRate = dsl.select(FILM.RENTAL_RATE).from(FILM).where(FILM.FILM_ID.eq(1)).fetchOne()
        val secondRentalRate = dsl.select(FILM.RENTAL_RATE).from(FILM).where(FILM.FILM_ID.eq(2)).fetchOne()

        assertEquals(2.99.toBigDecimal(), firstRentalRate?.value1())
        assertEquals(6.99.toBigDecimal(), secondRentalRate?.value1())
    }

    @Test
    fun `should count films with trailers by year (sorted by year)`() {
        val result = service.countFilmsWithTrailersByYear()

        assertEquals(
            listOf(
                YearCount(2006, 4),
                YearCount(2007, 8),
                YearCount(2008, 3),
                YearCount(2009, 1),
                YearCount(2010, 4),
                YearCount(2011, 2),
                YearCount(2012, 3),
                YearCount(2013, 4),
                YearCount(2014, 5),
                YearCount(2015, 7),
                YearCount(2016, 7),
                YearCount(2017, 2),
                YearCount(2018, 4),
                YearCount(2019, 2),
                YearCount(2020, 4),
                YearCount(2021, 3),
                YearCount(2022, 2),
                YearCount(2023, 3),
                YearCount(2024, 4),
            ),
            result,
        )
    }
}
