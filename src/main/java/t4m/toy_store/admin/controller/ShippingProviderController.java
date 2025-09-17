package t4m.toy_store.admin.controller;

import t4m.toy_store.admin.entity.ShippingProvider;
import t4m.toy_store.admin.service.ShippingProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping-providers")
@RequiredArgsConstructor
public class ShippingProviderController {

    private final ShippingProviderService service;

    @GetMapping
    public List<ShippingProvider> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<ShippingProvider> create(@RequestBody ShippingProvider provider) {
        return ResponseEntity.ok(service.create(provider));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingProvider> update(@PathVariable Long id, @RequestBody ShippingProvider provider) {
        return ResponseEntity.ok(service.update(id, provider));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
