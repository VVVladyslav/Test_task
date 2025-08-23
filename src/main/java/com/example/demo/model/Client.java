package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordersAsSupplier", "ordersAsConsumer"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "clients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_client_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_client_name", columnList = "name")
        }
)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Order> ordersAsSupplier = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "consumer", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Order> ordersAsConsumer = new HashSet<>();

    @Transient
    public BigDecimal getTotalProfit() {
        BigDecimal plus = ordersAsSupplier.stream()
                .map(Order::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal minus = ordersAsConsumer.stream()
                .map(Order::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return plus.subtract(minus);
    }
}