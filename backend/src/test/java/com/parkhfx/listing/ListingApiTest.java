package com.parkhfx.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ListingApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationSeedsHalifaxListings() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM listings WHERE sample_listing = true",
                Integer.class);

        assertThat(count).isEqualTo(70);
    }

    @Test
    void publicEndpointSearchesWithinBounds() throws Exception {
        mvc.perform(get("/api/v1/listings")
                        .param("minLongitude", "-63.590")
                        .param("minLatitude", "44.640")
                        .param("maxLongitude", "-63.570")
                        .param("maxLatitude", "44.660"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].latitude").isNumber())
                .andExpect(jsonPath("$[0].longitude").isNumber());
    }

    @Test
    void publicEndpointSearchesWithinRadius() throws Exception {
        mvc.perform(get("/api/v1/listings")
                        .param("longitude", "-63.5752")
                        .param("latitude", "44.6488")
                        .param("radiusKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void listingDetailIncludesPhotosAndAvailability() throws Exception {
        String id = jdbc.queryForObject(
                "SELECT id::text FROM listings WHERE sample_listing = true LIMIT 1",
                String.class);

        mvc.perform(get("/api/v1/listings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.photoUrls").isArray())
                .andExpect(jsonPath("$.photoUrls").isNotEmpty())
                .andExpect(jsonPath("$.availability").isArray())
                .andExpect(jsonPath("$.availability").isNotEmpty());
    }

    @Test
    void unknownListingReturns404() throws Exception {
        mvc.perform(get("/api/v1/listings/{id}",
                        "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Listing not found"));
    }

    @Test
    void partialBoundsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/listings")
                        .param("minLongitude", "-63.590")
                        .param("minLatitude", "44.640"))
                .andExpect(status().isBadRequest());
    }
}
