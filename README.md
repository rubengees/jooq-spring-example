# jOOQ Spring Example

This repo contains an example application utilizing jOOQ for database access. This is a Gradle project with automatic
setup of a database during the build (using testcontainers). The database is using
the [Pagila](https://github.com/devrimgunduz/pagila) example database.

### Getting started

- Optional: Import the project into your favorite IDE.
- Optional: Start the database locally and inspect the schema.
  - Run `docker compose up` in the root.
- Run the tests. This will also generate the jOOQ classes.
- Check `ExampleService` for the queries.
