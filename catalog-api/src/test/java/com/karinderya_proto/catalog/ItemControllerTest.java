package com.karinderya_proto.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
@Import(TokenVerifier.class)
@TestPropertySource(properties = "karinderya_proto.jwt.secret=test-secret-key")
class ItemControllerTest {

    private static final String SECRET = "test-secret-key";
    private static final String NEW_ITEM =
            "{\"name\":\"Banana Cue\",\"category\":\"Snacks\",\"price\":15,\"available\":true}";

    /** In-memory stand-in for the database, so tests need no MySQL. */
    static class FakeRepo implements ItemRepository {
        private final Map<Long, Item> items = new LinkedHashMap<>();
        private long nextId;

        FakeRepo() {
            reset();
        }

        void reset() {
            items.clear();
            items.put(1L, new Item(1, "Iced Tea", "Drinks", 25, true));
            nextId = 2;
        }

        @Override
        public List<Item> list() {
            return new ArrayList<>(items.values());
        }

        @Override
        public Optional<Item> find(long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public long create(String name, String category, double price, boolean available) {
            long id = nextId++;
            items.put(id, new Item(id, name, category, price, available));
            return id;
        }

        @Override
        public boolean update(long id, String name, String category, double price, boolean available) {
            if (!items.containsKey(id)) {
                return false;
            }
            items.put(id, new Item(id, name, category, price, available));
            return true;
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        FakeRepo fakeRepo() {
            return new FakeRepo();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeRepo repo;

    @BeforeEach
    void resetData() {
        repo.reset();
    }

    /** Builds a signed login token the same way auth-api does (HS256). */
    private static String token(String role) throws Exception {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(
                ("{\"sub\":\"tester\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = enc.encodeToString(
                mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "Bearer " + header + "." + payload + "." + signature;
    }

    @Test
    void healthReturns200() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void listItemsIsPublic() throws Exception {
        mvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Iced Tea"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void missingItemReturns404() throws Exception {
        mvc.perform(get("/items/99")).andExpect(status().isOk());
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(NEW_ITEM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNonStaffRoleReturns403() throws Exception {
        mvc.perform(post("/items").header("Authorization", token("student"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ITEM))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWithStaffTokenReturns201() throws Exception {
        mvc.perform(post("/items").header("Authorization", token("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ITEM))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Banana Cue"));
    }

    @Test
    void negativePriceIsRejected() throws Exception {
        String bad = "{\"name\":\"Banana Cue\",\"category\":\"Snacks\",\"price\":-5}";
        mvc.perform(post("/items").header("Authorization", token("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesItem() throws Exception {
        String body = "{\"name\":\"Lemon Tea\",\"category\":\"Drinks\",\"price\":30,\"available\":true}";
        mvc.perform(put("/items/1").header("Authorization", token("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lemon Tea"));
    }

    @Test
    void updateMissingItemReturns404() throws Exception {
        mvc.perform(put("/items/99").header("Authorization", token("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ITEM))
                .andExpect(status().isNotFound());
    }
}
