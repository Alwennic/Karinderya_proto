package com.karinderya_proto.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemController {

    private final ItemRepository repo;
    private final TokenVerifier verifier;

    public ItemController(ItemRepository repo, TokenVerifier verifier) {
        this.repo = repo;
        this.verifier = verifier;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/items")
    public List<Item> list() {
        return repo.list();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<Object> get(@PathVariable("id") long id) {
        Optional<Item> item = repo.find(id);
        if (item.isEmpty()) {
            return error(404, "Item not found");
        }
        return ResponseEntity.ok().body(item.get());
    }

    @PostMapping("/items")
    public ResponseEntity<Object> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ItemRequest body) {
        ResponseEntity<Object> denied = denyUnlessStaff(authorization);
        if (denied != null) {
            return denied;
        }
        String problem = validate(body);
        if (problem != null) {
            return error(400, problem);
        }
        long newId = repo.create(body.name().trim(), body.category().trim(), body.price(), isAvailable(body));
        return ResponseEntity.status(201).body(repo.find(newId).get());
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Object> update(
            @PathVariable("id") long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ItemRequest body) {
        ResponseEntity<Object> denied = denyUnlessStaff(authorization);
        if (denied != null) {
            return denied;
        }
        String problem = validate(body);
        if (problem != null) {
            return error(400, problem);
        }
        boolean found = repo.update(id, body.name().trim(), body.category().trim(), body.price(), isAvailable(body));
        if (!found) {
            return error(404, "Item not found");
        }
        return ResponseEntity.ok().body(repo.find(id).get());
    }

    // ---- helpers ----

    /** Returns an error response if the caller is not staff, or null if the request may continue. */
    private ResponseEntity<Object> denyUnlessStaff(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return error(401, "Missing token");
        }
        Optional<String> role = verifier.verifiedRole(authorization.substring(7).trim());
        if (role.isEmpty()) {
            return error(401, "Invalid or expired token");
        }
        if (!role.get().equals("admin") && !role.get().equals("staff")) {
            return error(403, "Staff access only");
        }
        return null;
    }

    private String validate(ItemRequest body) {
        if (body == null
                || body.name() == null || body.name().isBlank()
                || body.category() == null || body.category().isBlank()) {
            return "Name and category are required";
        }
        if (body.price() == null || body.price() < 0) {
            return "Price must be a number of 0 or more";
        }
        return null;
    }

    private boolean isAvailable(ItemRequest body) {
        return body.available() == null || body.available();
    }

    private ResponseEntity<Object> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
