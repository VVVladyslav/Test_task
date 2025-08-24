package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"supplier", "consumer"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                // Бизнес-ключ: title + supplier + consumer
                @UniqueConstraint(
                        name = "uk_order_business_key",
                        columnNames = {"title", "supplier_id", "consumer_id"}
                )
        },
        indexes = {
                @Index(name = "idx_order_supplier", columnList = "supplier_id"),
                @Index(name = "idx_order_consumer", columnList = "consumer_id"),
                @Index(name = "idx_order_title", columnList = "title")
        }
)
@Check(constraints = "price > 0")
@Check(constraints = "supplier_id <> consumer_id")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_supplier")
    )
    private Client supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "consumer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_consumer")
    )
    private Client consumer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public boolean isProcessed() {
        return finishedAt != null;
    }
}