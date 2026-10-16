package ru.yandex.practicum.inventory.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Setter
    @Column(nullable = false)
    private Integer quantity;

    @Setter
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    public InventoryItem(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @Transient
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
