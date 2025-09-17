package t4m.toy_store.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipping_providers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private double fee;
}