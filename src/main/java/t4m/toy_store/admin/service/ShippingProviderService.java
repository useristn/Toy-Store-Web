package t4m.toy_store.admin.service;

import t4m.toy_store.admin.entity.ShippingProvider;
import t4m.toy_store.admin.repository.ShippingProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingProviderService {
    private final ShippingProviderRepository repository;

    public List<ShippingProvider> findAll() { return repository.findAll(); }

    public ShippingProvider create(ShippingProvider provider) { return repository.save(provider); }

    public ShippingProvider update(Long id, ShippingProvider data) {
        return repository.findById(id)
                .map(p -> {
                    p.setName(data.getName());
                    p.setFee(data.getFee());
                    return repository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("Shipping provider not found"));
    }

    public void delete(Long id) { repository.deleteById(id); }
}
